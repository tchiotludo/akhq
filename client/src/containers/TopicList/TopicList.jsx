import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import Table from '../../components/Table';
import Header from '../Header';
import SearchBar from '../../components/SearchBar';
import Pagination from '../../components/Pagination';
import ConfirmModal from '../../components/Modal/ConfirmModal';
import api, { remove } from '../../utils/api';
import { uriTopics, uriDeleteTopics } from '../../utils/endpoints';
import constants from '../../utils/constants';
import { calculateTopicOffsetLag, showBytes } from '../../utils/converters';
import history from '../../utils/history';
import './styles.scss';

// Adaptation of topicList.ftl

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
    roles: JSON.parse(localStorage.getItem('roles'))
  };

  componentDidMount() {
    let { clusterId } = this.props.match.params;
    this.setState({ selectedCluster: clusterId }, this.getTopics);
  }

  showDeleteModal = deleteMessage => {
    this.setState({ showDeleteModal: true, deleteMessage });
  };

  closeDeleteModal = () => {
    this.setState({ showDeleteModal: false, deleteMessage: '' });
  };

  deleteTopic = () => {
    const { selectedCluster, topicToDelete } = this.state;
    const { history } = this.props;

    history.replace({ loading: true });
    remove(uriDeleteTopics(selectedCluster, topicToDelete.id))
      .then(res => {
        this.props.history.replace({
          showSuccessToast: true,
          successToastMessage: `Topic '${topicToDelete.name}' is deleted`,
          loading: false
        });
        this.setState({ showDeleteModal: false, topicToDelete: {} }, () => this.getTopics());
      })
      .catch(err => {
        this.props.history.replace({
          showErrorToast: true,
          errorToastMessage: `Could not delete '${topicToDelete.name}'`,
          loading: false
        });
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
    const { history } = this.props;
    const { selectedCluster, pageNumber } = this.state;
    const { search, topicListView } = this.state.searchData;
    let data = {};
    history.replace({
      ...this.props.location,
      loading: true
    });
    try {
      data = await api.get(uriTopics(selectedCluster, search, topicListView, pageNumber));
      data = data.data;
      if (data) {
        if (data.results) {
          this.handleTopics(data.results);
        } else {
          this.setState({ topics: [] });
        }
        this.setState({ selectedCluster, totalPageNumber: data.page });
      }
    } catch (err) {
      if (err.response && err.response.status === 404) {
        history.replace('/ui/page-not-found', { errorData: err });
      } else {
        history.replace('/ui/error', { errorData: err });
      }
    } finally {
      history.replace({
        ...this.props.location,
        loading: false
      });
    }
  }

  handleTopics(topics) {
    let tableTopics = [];
    topics.map(topic => {
      tableTopics.push({
        id: topic.name,
        name: topic.name,
        count: topic.size,
        size: showBytes(topic.logDirSize, 0),
        partitionsTotal: topic.partitions.length,
        replicationFactor: topic.replicaCount,
        replicationInSync: topic.inSyncReplicaCount,
        groupComponent: topic.consumerGroups,
        internal: topic.internal
      });
    });
    this.setState({ topics: tableTopics });
  }

  handleConsumerGroups = (consumerGroups, topicId) => {
    if (consumerGroups !== undefined) {
      return consumerGroups.map(consumerGroup => {
        let className = 'btn btn-sm mb-1 btn-';
        let offsetLag = calculateTopicOffsetLag(consumerGroup.offsets);

        const activeTopic = consumerGroup.activeTopics.find(activeTopic => activeTopic === topicId);
        activeTopic !== undefined ? (className += 'success') : (className += 'warning');

        return (
          <React.Fragment>
            <a class={className}>
              {consumerGroup.id} <span class="badge badge-light">Lag: {offsetLag}</span>
            </a>
            <br />
          </React.Fragment>
        );
      });
    }

    return '';
  };

  render() {
    const { topics, selectedCluster, searchData, pageNumber, totalPageNumber } = this.state;
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
              accessor: 'groupComponent',
              colName: 'Consumer Groups',
              type: 'text',
              cell: (obj, col) => {
                return this.handleConsumerGroups(obj[col.accessor], obj.id);
              }
            }
          ]}
          data={topics}
          onDelete={topic => {
            this.handleOnDelete(topic);
          }}
          onDetails={(id, row) => {
            history.push({ pathname: `/ui/${selectedCluster}/topic/${id}`, internal: row.internal });
          }}
          actions={
            roles.topic && roles.topic['topic/delete']
              ? [constants.TABLE_DELETE, constants.TABLE_DETAILS]
              : [constants.TABLE_DETAILS]
          }
        />

        <div
          className="navbar navbar-expand-lg navbar-light mr-auto 
        khq-data-filter khq-sticky khq-nav"
        >
          <div className="collapse navbar-collapse" />
          <Pagination
            pageNumber={pageNumber}
            totalPageNumber={totalPageNumber}
            onChange={this.handlePageChange}
            onSubmit={this.handlePageChangeSubmission}
          />
        </div>

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
