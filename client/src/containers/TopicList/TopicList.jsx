import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import Table from '../../components/Table';
import Header from '../Header';
import SearchBar from '../../components/SearchBar';
import Pagination from '../../components/Pagination';
import ConfirmModal from '../../components/Modal/ConfirmModal';
import api from '../../utils/api';
import endpoints from '../../utils/endpoints';
import constants from '../../utils/constants';
// Adaptation of topicList.ftl

class TopicList extends Component {
  state = {
    topics: [],
    showDeleteModal: false,
    selectedCluster: '',
    deleteMessage: '',
    deleteData: {},
    pageNumber: 1,
    totalPageNumber: 1,
    searchData: {
      search: '',
      topicListView: 'ALL'
    },
    createTopicFormData: {
      name: '',
      partition: 1,
      replication: 1,
      cleanup: 'delete', // TODO: delete default value not working
      retention: 86400000
    }
  };

  componentDidMount() {
    let { clusterId } = this.props.match.params;
    this.setState({ selectedCluster: clusterId }, () => {
      this.getTopics();
    });
  }

  showDeleteModal = (deleteMessage, deleteData) => {
    this.setState({ showDeleteModal: true, deleteMessage, deleteData });
  };

  closeDeleteModal = () => {
    this.setState({ showDeleteModal: false, deleteMessage: '', deleteData: {} });
  };

  deleteTopic = () => {
    const { clusterId, topic } = this.state.deleteData;
    this.deleteTopic(clusterId, topic.name);
    this.closeDeleteModal();

    this.props.history.push({
      pathname: `/${clusterId}/topic`,
      showSuccessToast: true,
      successToastMessage: `Topic '${topic.name}' is deleted`
    });
  };

  handleOnDelete() {
    console.log('handleOnDelete');
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
    try {
      data = await api.get(endpoints.uriTopics(selectedCluster, topicListView, search, pageNumber));
      data = data.data;
      if (data) {
        if (data.topics) {
          this.handleTopics(data.topics);
        } else {
          this.setState({ topics: [] });
        }
        this.setState({ selectedCluster, totalPageNumber: data.totalPageNumber });
      }
    } catch (err) {
      history.replace('/error', { errorData: err });
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
        size: <span className="text-nowrap">≈ {topic.size}</span>,
        weight: topic.count,
        partitionsTotal: topic.total,
        replicationFactor: topic.factor,
        replicationInSync: <span>{topic.inSync}</span>,
        groupComponent: topic.logDirSize
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
    const columnNames = [
      'Name',
      'Size',
      'Weight',
      'Total',
      'Factor',
      'In Sync',
      'Consumer Groups',
      ''
    ];

    return (
      <div id="content">
        <Header title="Topics" />
        <nav className="navbar navbar-expand-lg navbar-light bg-light mr-auto khq-data-filter khq-sticky khq-nav">
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
          colNames={columnNames}
          data={topics}
          onDelete={this.handleOnDelete}
          toPresent={[
            'name',
            'size',
            'weight',
            'partitionsTotal',
            'replicationFactor',
            'replicationInSync',
            'groupComponent'
          ]}
          onDetails={id => {
            history.push(`/${selectedCluster}/topic/${id}`);
          }}
          actions={[constants.TABLE_DELETE, constants.TABLE_DETAILS]}
        ></Table>

        <div className="navbar navbar-expand-lg navbar-light mr-auto khq-data-filter khq-sticky khq-nav">
          <div className="collapse navbar-collapse"></div>
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
