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
// Adaptation of topicList.ftl

class TopicList extends Tab {
  state = {
    topics: [],
    showDeleteModal: false,
    selectedCluster: '',
    deleteMessage: '',
    deleteData: {},
    createTopicFormData: {
      name: '',
      partition: 1,
      replication: 1,
      cleanup: 'delete', // TODO: delete default value not working
      retention: 86400000
    }
  };

  componentDidMount() {
    this.getTopics();
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
    let topics = {};
    let selectedClusterId = 'my-cluster';
    try {
      topics = await api.get(endpoints.uriTopics(selectedClusterId));
      this.handleTopics(topics.data);
      this.setState({ selectedCluster: selectedClusterId });
    } catch (err) {
      console.log('Error :' + err);
    }
  }

  handleTopics(topics) {
      console.log(topics);
   /*  let tableTopics = topics.map(topic => {
    
         topic.size = 0;
      topic.logDirSize = 0;
      tableTopics.push({
        id: topic._id,
        name: topic.name,
        size: <span className="text-nowrap">≈ {topic.size}</span>,
        logDirSize: topic.logDirSize ? 'n/a' : topic.logDirSize,
        partition: topic.partition,
        replicationFactor: topic.replication,
        replicationInSync: <span>{topic.replication}</span>
      });
      
    });
    this.setState({ topics: tableTopics });*/
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
        <SearchBar pagination={true} topicListView={true} />

        <Table
          has2Headers
          firstHeader={firstColumns}
          colNames={columnNames}
          onDetails={this.handleOnDetails}
          onDelete={this.handleOnDelete}
        ></Table>

        {this.handleTopics()}

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
