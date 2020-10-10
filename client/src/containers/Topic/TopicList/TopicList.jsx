import React from 'react';
import { Link } from 'react-router-dom';
import Table from '../../../components/Table';
import Header from '../../Header';
import SearchBar from '../../../components/SearchBar';
import Pagination from '../../../components/Pagination';
import ConfirmModal from '../../../components/Modal/ConfirmModal';
import { uriDeleteTopics, uriTopics, uriTopicsGroups } from '../../../utils/endpoints';
import constants from '../../../utils/constants';
import { calculateTopicOffsetLag, showBytes } from '../../../utils/converters';
import './styles.scss';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import {Collapse} from 'react-bootstrap';
import Root from '../../../components/Root';

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
    collapseConsumerGroups: {}
  };

  componentDidMount() {
    const { clusterId } = this.props.match.params;
    const query =  new URLSearchParams(this.props.location.search);
    const {searchData, keepSearch} = this.state;

    let searchDataTmp;
    let keepSearchTmp = keepSearch;

    const topicListSearch = localStorage.getItem('topicListSearch');
    if(topicListSearch) {
      searchDataTmp = JSON.parse(topicListSearch);
      keepSearchTmp = true;
    } else {
      searchDataTmp = {
        search: (query.get('search'))? query.get('search') : searchData.search,
        topicListView: (query.get('topicListView'))? query.get('topicListView') : searchData.topicListView,
      }
    }

    this.setState({selectedCluster: clusterId, searchData: searchDataTmp, keepSearch: keepSearchTmp}, () => {
      this.getTopics();
      this.props.history.replace({
        pathname: `/ui/${this.state.selectedCluster}/topic`,
        search: this.props.location.search
      });
    });

  }

  componentDidUpdate(prevProps, prevState) {
    if (this.props.location.pathname !== prevProps.location.pathname) {
      let { clusterId } = this.props.match.params;
      this.setState({ selectedCluster: clusterId }, this.getTopics);
    }
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
        search: `search=${searchData.search}&topicListView=${searchData.topicListView}`
      });
    });
  };

  handlePageChangeSubmission = value => {
    const { totalPageNumber } = this.state;
    if (value <= 0) {
      value = 1;
    } else if (value > totalPageNumber) {
      value = totalPageNumber;
    }

    this.setState({ pageNumber: value }, () => {
      this.getTopics();
    });
  };

  handlePageChange = ({ currentTarget: input }) => {
    const { value } = input;
    this.setState({ pageNumber: value });
  };

  async getTopics() {
    const { selectedCluster, pageNumber } = this.state;
    const { search, topicListView } = this.state.searchData;
    this.setState({ loading: true } );

    let data = await this.getApi(uriTopics(selectedCluster, search, topicListView, pageNumber));
    data = data.data;

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

    const { selectedCluster } = this.state;

    const setState = () =>  {
      this.setState({ topics: Object.values(tableTopics) });
    }

    topics.forEach(topic => {
      tableTopics[topic.name] = {
        id: topic.name,
        name: topic.name,
        count: topic.size,
        size: showBytes(topic.logDirSize, 0),
        partitionsTotal: topic.partitions.length,
        replicationFactor: topic.replicaCount,
        replicationInSync: topic.inSyncReplicaCount,
        groupComponent: undefined,
        internal: topic.internal
      }

      collapseConsumerGroups[topic.name] = false;

      this.getApi(uriTopicsGroups(selectedCluster, topic.name))
        .then(value => {
          tableTopics[topic.name].groupComponent = value.data
          setState()
        })
    });
    this.setState({collapseConsumerGroups});
    setState()
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
            <a
              key={consumerGroup.id}
              href={`/ui/${this.state.selectedCluster}/group/${consumerGroup.id}`}
              className={className}
              onClick={noPropagation}
            >
              {consumerGroup.id} <div className="badge badge-secondary"> Lag: {offsetLag}</div>
            </a>
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
    const { topics, selectedCluster, searchData, pageNumber, totalPageNumber, loading, collapseConsumerGroups, keepSearch } = this.state;
    const roles = this.state.roles || {};
    const { clusterId } = this.props.match.params;
    const firstColumns = [
      { colName: 'Topics', colSpan: 3 },
      { colName: 'Partitions', colSpan: 1 },
      { colName: 'Replications', colSpan: 2 },
      { colName: 'Consumer Groups', colSpan: 1 }
    ];

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
            onChange={this.handlePageChange}
            onSubmit={this.handlePageChangeSubmission}
          />
        </nav>

        <Table
          loading={loading}
          history={this.props.history}
          has2Headers
          firstHeader={firstColumns}
          columns={[
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
            },
            {
              id: 'partitionsTotal',
              accessor: 'partitionsTotal',
              colName: 'Total',
              type: 'text'
            },
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
            },
            {
              id: 'groupComponent',
              accessor: 'groupComponent.id',
              colName: 'Consumer Groups',
              type: 'text',
              cell: obj => {
                if (obj.groupComponent && obj.groupComponent.length > 0) {
                  const consumerGroups = this.handleConsumerGroups(obj.groupComponent, obj.id);
                  let i=0;
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
          ]}
          data={topics}
          updateData={data => {
            this.setState({ topics: data });
          }}
          onDelete={topic => {
            this.handleOnDelete(topic);
          }}
          onDetails={(id) => `/ui/${selectedCluster}/topic/${id}/data`}
          onConfig={(id) => `/ui/${selectedCluster}/topic/${id}/configs`}
          actions={
            roles.topic && roles.topic['topic/delete']
              ? [constants.TABLE_DELETE, constants.TABLE_DETAILS, constants.TABLE_CONFIG]
              : [constants.TABLE_DETAILS, constants.TABLE_CONFIG]
          }
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
