import React from 'react';
import { Link } from 'react-router-dom';
import Table from '../../components/Table';
import Header from '../Header';
import SearchBar from '../../components/SearchBar';
import Pagination from '../../components/Pagination';
import Tab from '../Tab/Tab';
import ConfirmModal from '../../components/Modal/ConfirmModal';
import api from '../../utils/api';
import endpoints from '../../utils/endpoints';
import constants from '../../utils/constants';
// Adaptation of topicList.ftl

class TopicList extends Tab {
  state = {
    topics: [],
    showDeleteModal: false,
    selectedCluster: '',
    deleteMessage: '',
    deleteData: {},
    selectedTopic: constants.TOPICS.ALL,
    search: '',
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

  handleOnDetails() {
    console.log('handleOnDetails');
  }

  handleOnDelete() {
    console.log('handleOnDelete');
  }

  async getTopics() {
    let { history } = this.props;
    let topics = {};
    let selectedClusterId = this.state.selectedCluster;
    let selectedTopic = this.state.selectedTopic;
    let search = this.state.search;
    try {
      topics = await api.get(endpoints.uriTopics(selectedClusterId, selectedTopic, search));
      console.log('Topics', topics, selectedClusterId);
      if (topics.data) {
        this.handleTopics(topics.data);
        this.setState({ selectedCluster: selectedClusterId });
      }
    } catch (err) {
      history.replace('/error', { errorData: err });
      console.log('Error :' + err);
    }
  }

  handleTopics(topics) {
    if (!topics) {
      console.log('Not getting anything from backend');
    }
    let tableTopics = [];
    topics.map(topic => {
      topic.size = 0;
      topic.logDirSize = 0;
      tableTopics.push({
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
    const { topics } = this.state;
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
        <SearchBar
          pagination={true}
          topicListView={true}
          value={this.state.value}
          onChangeValue={value => {
            this.setState({ value });
          }}
          topic={this.state.selectedTopic}
          onChangeTopic={topic => {
            this.setState({ topic });
          }}
        />

        <Table
          has2Headers
          firstHeader={firstColumns}
          colNames={columnNames}
          data={topics}
          onDetails={this.handleOnDetails}
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
          actions={[constants.TABLE_DELETE, constants.TABLE_DETAILS]}
        ></Table>

        <Pagination />

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
