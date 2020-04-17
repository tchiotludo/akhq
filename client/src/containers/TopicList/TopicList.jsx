import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import Table from '../../components/Table';
import Header from '../Header';
import SearchBar from '../../components/SearchBar';
import Pagination from '../../components/Pagination';
import ConfirmModal from '../../components/Modal/ConfirmModal';
import api, { remove } from '../../utils/api';
import endpoints, { uriDeleteTopics } from '../../utils/endpoints';
import constants from '../../utils/constants';
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
    }
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
    const deleteData = {
      clusterId: selectedCluster,
      topicId: topicToDelete.id
    };
    history.push({ loading: true });
    remove(uriDeleteTopics(), deleteData)
      .then(res => {
        this.props.history.push({
          showSuccessToast: true,
          successToastMessage: `Topic '${topicToDelete.name}' is deleted`,
          loading: false
        });
        this.setState({ showDeleteModal: false, topicToDelete: {} });
        this.handleTopics(res.data.topics);
      })
      .catch(err => {
        this.props.history.push({
          showErrorToast: true,
          errorToastMessage: `Could not delete '${topicToDelete.name}'`,
          loading: false
        });
        this.setState({ showDeleteModal: false, topicToDelete: {} });
      });
  };

  handleOnDelete(topic) {
    this.setState({ topicToDelete: topic }, () => {
      this.showDeleteModal(`Delete topic ${topic.id}?`);
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
    history.push({
      ...this.props.location,
      loading: true
    });
    try {
      data = await api.get(endpoints.uriTopics(selectedCluster, search, pageNumber));
      console.log('data', data);
      data = data.data;
      if (data) {
        if (data.results) {
          this.handleTopics(data.results);
        } else {
          this.setState({ topics: [] });
        }
        this.setState({ selectedCluster, totalPageNumber: data.totalPageNumber });
      }
    } catch (err) {
      history.replace('/error', { errorData: err });
    } finally {
      history.push({
        ...this.props.location,
        loading: false
      });
    }
  }

  handleTopics(topics) {
    let tableTopics = [];
    topics.map(topic => {
      topic.size = 0;
      topic.logDirSize = 0;
      tableTopics.push({
        id: topic.name,
        name: topic.name,
        size: topic.size,
        weight: topic.logDirSize,
        partitionsTotal: topic.partitions.length,
        replicationFactor: topic.replicaCount,
        replicationInSync: topic.inSyncReplicaCount,
        groupComponent: topic.logDir[0].offsetLag
      });
    });
    this.setState({ topics: tableTopics });
  }

  render() {
    const { topics, selectedCluster, searchData, pageNumber, totalPageNumber } = this.state;
    const { history } = this.props;
    const { clusterId } = this.props.match.params;
    const firstColumns = [
      { colName: 'Topics', colSpan: 3 },
      { colName: 'Partitions', colSpan: 1 },
      { colName: 'Replications', colSpan: 2 },
      { colName: 'Consumer Groups', colSpan: 1 },
      { colName: '', colSpan: 1 }
    ];

    return (
      <div id="content">
        <Header title="Topics" />
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
              id: 'size',
              accessor: 'size',
              colName: 'Size',
              type: 'text',
              cell: (obj, col) => {
                return <span className="text-nowrap">≈ {obj[col.accessor]}</span>;
              }
            },
            {
              id: 'weight',
              accessor: 'weight',
              colName: 'Weight',
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
              type: 'text'
            }
          ]}
          data={topics}
          onDelete={topic => {
            this.handleOnDelete(topic);
          }}
          onDetails={id => {
            history.push(`/${selectedCluster}/topic/${id}`);
          }}
          actions={[constants.TABLE_DELETE, constants.TABLE_DETAILS]}
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

        <aside>
          <Link
            to={{
              pathname: `/${clusterId}/topic/create`,
              state: { formData: this.state.createTopicFormData }
            }}
            className="btn btn-primary"
          >
            Create a topic
          </Link>
        </aside>

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
