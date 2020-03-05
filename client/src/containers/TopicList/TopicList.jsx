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
// Adaptation of topicList.ftl

class TopicList extends Component {
  state = {
    topics: [],
    showDeleteModal: false,
    topicToDelete: {},
    selectedCluster: '',
    deleteMessage: '',
    deleteData: {},
    selectedTopic: constants.TOPICS.ALL,
    search: '',
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
    this.setState({ selectedCluster: clusterId }, () => {
      this.getTopics();
    });
  }

  showDeleteModal = deleteMessage => {
    this.setState({ showDeleteModal: true, deleteMessage });
  };

  closeDeleteModal = () => {
    this.setState({ showDeleteModal: false, deleteMessage: '' });
  };

  deleteTopic = () => {
    const { selectedCluster, topicToDelete } = this.state;
    const deleteData = {
      clusterId: selectedCluster,
      topicId: topicToDelete.id
    };
    remove(uriDeleteTopics(), deleteData)
      .then(res => {
        this.props.history.push({
          showSuccessToast: true,
          successToastMessage: `Topic '${topicToDelete.name}' is deleted`
        });
        this.setState({ showDeleteModal: false, topicToDelete: {} });
        this.handleTopics(res.data);
      })
      .catch(err => {
        this.props.history.push({
          showErrorToast: true,
          errorToastMessage: `Could not delete '${topicToDelete.name}'`
        });
        this.setState({ showDeleteModal: false, topicToDelete: {} });
      });
  };

  handleOnDelete(topic) {
    this.setState({ topicToDelete: topic }, () => {
      this.showDeleteModal(`Delete topic ${topic.id}?`);
    });
  }

  async getTopics() {
    let { history } = this.props;
    let topics = {};
    let selectedClusterId = this.state.selectedCluster;
    let selectedTopic = this.state.selectedTopic;
    let search = this.state.search;
    try {
      topics = await api.get(endpoints.uriTopics(selectedClusterId, selectedTopic, search));
      if (topics.data) {
        this.handleTopics(topics.data);
        this.setState({ selectedCluster: selectedClusterId });
      }
    } catch (err) {
      history.replace('/error', { errorData: err });
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
        id: topic.name,
        name: topic.name,
        size: topic.size,
        weight: topic.count,
        partitionsTotal: topic.total,
        replicationFactor: topic.factor,
        replicationInSync: topic.inSync,
        groupComponent: topic.logDirSize
      });
    });
    this.setState({ topics: tableTopics });
  }

  render() {
    const { topics, selectedCluster, selectedTopic } = this.state;
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
