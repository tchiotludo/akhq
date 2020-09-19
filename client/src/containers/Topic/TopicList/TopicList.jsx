import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import Table from '../../../components/Table';
import Header from '../../Header';
import SearchBar from '../../../components/SearchBar';
import Pagination from '../../../components/Pagination';
import ConfirmModal from '../../../components/Modal/ConfirmModal';
import api, { remove } from '../../../utils/api';
import { uriDeleteTopics, uriTopics, uriTopicsGroups } from '../../../utils/endpoints';
import constants from '../../../utils/constants';
import { calculateTopicOffsetLag, showBytes } from '../../../utils/converters';
import './styles.scss';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import axios from 'axios';

class TopicList extends Component {
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
    createTopicFormData: {
      name: '',
      partition: 1,
      replication: 1,
      cleanup: 'delete',
      retention: 86400000
    },
    roles: JSON.parse(sessionStorage.getItem('roles')),
    loading: true,
    cancel: undefined
  };

  componentDidMount() {
    let { clusterId } = this.props.match.params;
    this.setState({ selectedCluster: clusterId }, this.getTopics);
  }

  componentWillUnmount() {
    const { cancel } = this.state;

    if (cancel !== undefined) {
      cancel.cancel('cancel all');
    }
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

    remove(uriDeleteTopics(selectedCluster, topicToDelete.id))
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

    let data = await api.get(uriTopics(selectedCluster, search, topicListView, pageNumber));
    data = data.data;

    if (data) {
      if (data.results) {
        this.handleTopics(data.results);
      } else {
        this.setState({ topics: [] });
      }
      this.setState({ selectedCluster, totalPageNumber: data.page, loading: false }, () =>
          this.props.history.replace(`/ui/${selectedCluster}/topic`)
      )
    } else {
      this.setState({ topics: [], loading: false, totalPageNumber: 0});
    }
  }

  handleTopics(topics) {
    let tableTopics = {};

    const { selectedCluster } = this.state;

    const setState = () =>  {
      this.setState({ topics: Object.values(tableTopics) });
    }


    let source = axios.CancelToken.source();
    this.setState({cancel: source});

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

      api.get(uriTopicsGroups(selectedCluster, topic.name), {cancelToken: source.token})
        .then(value => {
          tableTopics[topic.name].groupComponent = value.data
          setState()
        })
    });

    setState()
  }



  handleConsumerGroups = (consumerGroups, topicId) => {
    if (consumerGroups) {
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

  render() {
    const { topics, selectedCluster, searchData, pageNumber, totalPageNumber, loading } = this.state;
    const roles = this.state.roles || {};
    const { history } = this.props;
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
            onTopicListViewChange={value => {
              let { searchData } = { ...this.state };
              searchData.topicListView = value;
              this.setState(searchData);
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
                if (obj.groupComponent) {
                  return this.handleConsumerGroups(obj.groupComponent, obj.id);
                } else {

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
          onDetails={(id, row) => {
            history.push({
              pathname: `/ui/${selectedCluster}/topic/${id}/data`,
              internal: row.internal
            });
          }}
          onConfig={(id, row) => {
            history.push({
              pathname: `/ui/${selectedCluster}/topic/${id}/configs`,
              internal: row.internal
            });
          }}
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
