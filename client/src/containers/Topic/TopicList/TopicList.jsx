import React from 'react';
import {Link} from 'react-router-dom';
import Table from '../../../components/Table';
import Header from '../../Header';
import SearchBar from '../../../components/SearchBar';
import Pagination from '../../../components/Pagination';
import ConfirmModal from '../../../components/Modal/ConfirmModal';
import {uriConsumerGroupByTopics, uriDeleteTopics, uriTopicLastRecord, uriTopics} from '../../../utils/endpoints';
import constants from '../../../utils/constants';
import {calculateTopicOffsetLag, showBytes} from '../../../utils/converters';
import './styles.scss';
import {toast} from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import {Collapse} from 'react-bootstrap';
import Root from '../../../components/Root';
import {getClusterUIOptions} from "../../../utils/functions";
import {handlePageChange, getPageNumber} from "./../../../utils/pagination"

class TopicList extends Root {
  state = {
    topics: [],
    showDeleteModal: false,
    topicToDelete: {},
    selectedCluster: '',
    deleteMessage: '',
    deleteData: {},
    pageNumber: 1,
    totalPageNumber: 1,
    searchData: {
      search: '',
      topicListView: 'HIDE_INTERNAL'
    },
    keepSearch: false,
    createTopicFormData: {
      name: '',
      partition: 1,
      replication: 1,
      cleanup: 'delete',
      retention: 86400000
    },
    roles: JSON.parse(sessionStorage.getItem('roles')),
    loading: true,
    cancel: undefined,
    collapseConsumerGroups: {},
    uiOptions: {}
  };

  componentDidMount() {

   this._initializeVars(() => {
     this.getTopics();
     this.props.history.replace({
       pathname: `/ui/${this.state.selectedCluster}/topic`,
       search: this.props.location.search
     })});
  }

  componentDidUpdate(prevProps, prevState) {

    if (this.props.location.pathname !== prevProps.location.pathname) {
      this.cancelAxiosRequests();
      this.renewCancelToken();

      this._initializeVars(this.getTopics);
    }
  }

  async _initializeVars(callBackFunction) {
    const { clusterId } = this.props.match.params;
    const query =  new URLSearchParams(this.props.location.search);
    const {searchData, keepSearch} = this.state;
    let { pageNumber } = this.state;
    const uiOptions = await getClusterUIOptions(clusterId)

    let searchDataTmp;
    let keepSearchTmp = keepSearch;
    const topicListSearch = localStorage.getItem('topicListSearch');
    if(topicListSearch) {
      searchDataTmp = JSON.parse(topicListSearch);
      keepSearchTmp = true;
    } else {
      searchDataTmp = {
        search: (query.get('search'))? query.get('search') : searchData.search,
        topicListView: (query.get('topicListView'))? query.get('topicListView') :
            (uiOptions && uiOptions.topic && uiOptions.topic.defaultView)? uiOptions.topic.defaultView : searchData.topicListView,
      }
      pageNumber = (query.get('page'))? parseInt(query.get('page')) : parseInt(pageNumber)
    }

    this.setState({selectedCluster: clusterId, searchData: searchDataTmp, keepSearch: keepSearchTmp, uiOptions: (uiOptions)? uiOptions.topic : {}, pageNumber: pageNumber}, callBackFunction);
  }

  showDeleteModal = deleteMessage => {
    this.setState({ showDeleteModal: true, deleteMessage });
  };

  closeDeleteModal = () => {
    this.setState({ showDeleteModal: false, deleteMessage: '' });
  };

  deleteTopic = () => {
    const { selectedCluster, topicToDelete } = this.state;

    this.removeApi(uriDeleteTopics(selectedCluster, topicToDelete.id))
      .then(() => {
        toast.success(`Topic '${topicToDelete.name}' is deleted`);
        this.setState({ showDeleteModal: false, topicToDelete: {} }, () => this.getTopics());
      })
      .catch(() => {
        this.setState({ showDeleteModal: false, topicToDelete: {} }, () => this.getTopics());
      });
  };

  handleOnDelete(topic) {
    this.setState({ topicToDelete: topic }, () => {
      this.showDeleteModal(
        <React.Fragment>Do you want to delete topic: {<code>{topic.id}</code>} ?</React.Fragment>
      );
    });
  }

  handleSearch = data => {
    const { searchData } = data;

    this.setState({ pageNumber: 1, searchData }, () => {
      this.getTopics();
      this.handleKeepSearchChange(data.keepSearch);
      this.props.history.push({
        pathname: `/ui/${this.state.selectedCluster}/topic`,
        search: `search=${searchData.search}&topicListView=${this.state.searchData.topicListView}&page=${this.state.pageNumber}`
      });

    });
  };

  handlePageChangeSubmission = value => {
    let pageNumber = getPageNumber(value, this.state.totalPageNumber);

    this.setState({ pageNumber: pageNumber }, () => {
      this.getTopics();
      this.props.history.push({
        pathname: `/ui/${this.state.selectedCluster}/topic`,
        search: `search=${this.state.searchData.search}&topicListView=${this.state.searchData.topicListView}&page=${pageNumber}`
      });
    });
  };

  async getTopics() {
    const { selectedCluster, pageNumber } = this.state;
    const { search, topicListView } = this.state.searchData;
    this.setState({ loading: true } );

    let response = await this.getApi(uriTopics(selectedCluster, search, topicListView, pageNumber));
    let data = response.data;

    if (data) {
      if (data.results) {
        this.handleTopics(data.results);
      } else {
        this.setState({ topics: [] });
      }
      this.setState({ selectedCluster, totalPageNumber: data.page, loading: false }  )
    } else {
      this.setState({ topics: [], loading: false, totalPageNumber: 0});
    }
  }

  handleTopics(topics) {
    let tableTopics = {};
    const collapseConsumerGroups = {};

    const { selectedCluster, uiOptions } = this.state;

    const setState = () =>  {
      this.setState({ topics: Object.values(tableTopics) });
    }

    topics.forEach(topic => {
      tableTopics[topic.name] = {
        id: topic.name,
        name: topic.name,
        count: topic.size,
        lastWrite: undefined,
        size: showBytes(topic.logDirSize, 0),
        partitionsTotal: topic.partitions.length,
        replicationFactor: topic.replicaCount,
        replicationInSync: topic.inSyncReplicaCount,
        groupComponent: undefined,
        internal: topic.internal
      }
      collapseConsumerGroups[topic.name] = false;
    });
    this.setState({collapseConsumerGroups});
    setState()

    const topicsName = topics.map(topic => topic.name).join(",");

    if(!uiOptions.skipConsumerGroups) {
      this.getApi(uriConsumerGroupByTopics(selectedCluster, encodeURIComponent(topicsName)))
          .then(value => {
            topics.forEach(topic => {
              tableTopics[topic.name].groupComponent = (value && value.data) ? value.data.filter(consumerGroup =>
                  (consumerGroup.activeTopics && consumerGroup.activeTopics.includes(topic.name))
                  || (consumerGroup.topics && consumerGroup.topics.includes(topic.name))) : [];
            });
            setState();
          });
    }

    if(!uiOptions.skipLastRecord) {
      this.getApi(uriTopicLastRecord(selectedCluster, encodeURIComponent(topicsName)))
          .then(value => {
            topics.forEach((topic) => {
              tableTopics[topic.name].lastWrite = value.data[topic.name] ? value.data[topic.name].timestamp : ''
            });
            setState();
          });
    }
  }



  handleConsumerGroups = (consumerGroups, topicId) => {
    if (consumerGroups && consumerGroups.length > 0) {
      return consumerGroups.map(consumerGroup => {
        let className = 'btn btn-sm mb-1 mr-1 btn-';
        let offsetLag = calculateTopicOffsetLag(consumerGroup.offsets, topicId);

          const activeTopic = consumerGroup.activeTopics && consumerGroup.activeTopics.find(
              activeTopic => activeTopic === topicId
          );
          activeTopic ? (className += 'success') : (className += 'warning');

          const noPropagation = e => e.stopPropagation();

          return (
            <Link
              key={consumerGroup.id}
              to={`/ui/${this.state.selectedCluster}/group/${consumerGroup.id}`}
              className={className}
              onClick={noPropagation}
            >
              {consumerGroup.id} <div className="badge badge-secondary"> Lag: {offsetLag}</div>
            </Link>
          );
      });
    }

    return '';
  };


  handleCollapseConsumerGroups(id) {
    const tmpGroups = {};

    Object.keys(this.state.collapseConsumerGroups).forEach(key => {
      tmpGroups[key] = (key === id)?  !this.state.collapseConsumerGroups[key] : this.state.collapseConsumerGroups[key];
     });
     this.setState({collapseConsumerGroups : tmpGroups});
   }

  handleKeepSearchChange(value){
    const { searchData } = this.state;
    if(value) {
      localStorage.setItem('topicListSearch', JSON.stringify(searchData));
    } else {
      localStorage.removeItem('topicListSearch');
    }

  }

  render() {
    const { topics, selectedCluster, searchData, pageNumber, totalPageNumber, loading, collapseConsumerGroups, keepSearch, uiOptions } = this.state;
    const roles = this.state.roles || {};
    const { clusterId } = this.props.match.params;

    const topicCols =
        [
          {
            id: 'name',
            accessor: 'name',
            colName: 'Name',
            type: 'text'
          },
          {
            id: 'count',
            accessor: 'count',
            colName: 'Count',
            type: 'text',
            cell: (obj, col) => {
              return <span className="text-nowrap">≈ {obj[col.accessor]}</span>;
            }
          },
          {
            id: 'size',
            accessor: 'size',
            colName: 'Size',
            type: 'text'
          }
        ];

    if(!uiOptions.skipLastRecord) {
        topicCols.push({
          id: 'lastWrite',
          accessor: 'lastWrite',
          colName: 'Last Record',
          type: 'text'
        });
    }

    const partitionCols =
        [
          {
            id: 'partitionsTotal',
            accessor: 'partitionsTotal',
            colName: 'Total',
            type: 'text'
          }];
    const replicationCols =
         [
          {
            id: 'replicationFactor',
            accessor: 'replicationFactor',
            colName: 'Factor',
            type: 'text'
          },
          {
            id: 'replicationInSync',
            accessor: 'replicationInSync',
            colName: 'In Sync',
            type: 'text',
            cell: (obj, col) => {
              return <span>{obj[col.accessor]}</span>;
            }
          }];

    const consumerGprCols =
        [
          {
            id: 'groupComponent',
            accessor: 'groupComponent.id',
            colName: 'Consumer Groups',
            type: 'text',
            cell: obj => {
              if (obj.groupComponent && obj.groupComponent.length > 0) {
                const consumerGroups = this.handleConsumerGroups(obj.groupComponent, obj.id);
                let i = 0;
                return (
                    <>
                      {consumerGroups[0]}
                      {consumerGroups.length > 1 &&
                      <span>
                                  <span
                                      onClick={() => this.handleCollapseConsumerGroups(obj.id)}
                                      aria-expanded={collapseConsumerGroups[obj.id]}
                                  >
                                    {collapseConsumerGroups[obj.id] && <i className="fa fa-fw fa-chevron-up"/>}
                                    {!collapseConsumerGroups[obj.id] && <i className="fa fa-fw fa-chevron-down"/>}
                                  </span>
                                  <span className="badge badge-secondary">{consumerGroups.length}</span>
                                  <Collapse in={collapseConsumerGroups[obj.id]}>
                                    <div>
                                      {consumerGroups.splice(1, consumerGroups.length).map(group => {
                                        return (<div key={i++}>{group}</div>);
                                      })}
                                    </div>
                                  </Collapse>
                                </span>
                      }
                    </>
                );
              } else if (obj.groupComponent) {
                return <div className="empty-consumergroups"/>
              }
            }
          }
        ]

    const firstColumns = [
      {colName: 'Topics', colSpan: topicCols.length},
      {colName: 'Partitions', colSpan: partitionCols.length},
      {colName: 'Replications', colSpan: replicationCols.length}
    ];

    if(!uiOptions.skipConsumerGroups) {
      firstColumns.push({colName: 'Consumer Groups', colSpan: 1});
    }

    let onDetailsFunction = undefined;
    const actions = [constants.TABLE_CONFIG];
    if(roles.topic && roles.topic['topic/data/read']) {
      actions.push(constants.TABLE_DETAILS);
      onDetailsFunction = (id) => `/ui/${selectedCluster}/topic/${id}/data`;
    }
    if(roles.topic && roles.topic['topic/delete']) {
      actions.push(constants.TABLE_DELETE);
    }

    return (
      <div>
        <Header title="Topics" history={this.props.history} />
        <nav
          className="navbar navbar-expand-lg navbar-light
        bg-light mr-auto khq-data-filter khq-sticky khq-nav"
        >
          <SearchBar
            showSearch={true}
            search={searchData.search}
            showPagination={true}
            pagination={pageNumber}
            showTopicListView={true}
            topicListView={searchData.topicListView}
            showKeepSearch={true}
            keepSearch={keepSearch}
            onTopicListViewChange={value => {
              let { searchData } = { ...this.state };
              searchData.topicListView = value;
              this.setState(searchData);
            }}
            onKeepSearchChange={value => {
                this.handleKeepSearchChange(value);
              }}
              doSubmit={this.handleSearch}
          />
          <Pagination
            pageNumber={pageNumber}
            totalPageNumber={totalPageNumber}
            onChange={handlePageChange}
            onSubmit={this.handlePageChangeSubmission}
          />
        </nav>

        <Table
          loading={loading}
          history={this.props.history}
          has2Headers
          firstHeader={firstColumns}
          columns={topicCols.concat(partitionCols, replicationCols, (uiOptions.skipConsumerGroups)?[]: consumerGprCols)}
          data={topics}
          updateData={data => {
            this.setState({ topics: data });
          }}
          onDelete={topic => {
            this.handleOnDelete(topic);
          }}
          onDetails={onDetailsFunction}
          onConfig={(id) => `/ui/${selectedCluster}/topic/${id}/configs`}
          actions={actions}
        />

        {roles.topic['topic/insert'] && (
          <aside>
            <Link
              to={{
                pathname: `/ui/${clusterId}/topic/create`,
                state: { formData: this.state.createTopicFormData }
              }}
              className="btn btn-primary"
            >
              Create a topic
            </Link>
          </aside>
        )}

        <ConfirmModal
          show={this.state.showDeleteModal}
          handleCancel={this.closeDeleteModal}
          handleConfirm={this.deleteTopic}
          message={this.state.deleteMessage}
        />
      </div>
    );
  }
}

export default TopicList;
