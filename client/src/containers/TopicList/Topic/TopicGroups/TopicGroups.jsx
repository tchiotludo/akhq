import React, { Component } from 'react';
import Table from '../../../../components/Table';
import api from '../../../../utils/api';
import { uriTopicsGroups } from '../../../../utils/endpoints';
import constants from '../../../../utils/constants';
import history from '../../../../utils/history';
import { Link } from 'react-router-dom';
import Header from '../../../Header';
import SearchBar from '../../../../components/SearchBar';
import Pagination from '../../../../components/Pagination';
import ConfirmModal from '../../../../components/Modal/ConfirmModal';

class TopicGroups extends Component {
  state = {
    consumerGroups: [],
    topicId: this.props.topicId,
    showDeleteModal: false,
    selectedCluster: this.props.clusterId,
    deleteMessage: ''
  };

  componentDidMount() {
    this.getConsumerGroup();
  }

  async getConsumerGroup() {
    const { history } = this.props;
    const { selectedCluster, topicId } = this.state;
    let data = {};
    history.push({
      loading: true
    });
    try {
      data = await api.get(uriTopicsGroups(selectedCluster, topicId));
      data = data.data;
      if (data) {
        if (data) {
          this.handleGroups(data);
        } else {
          this.setState({ consumerGroup: [] });
        }
        this.setState({ selectedCluster });
      }
    } catch (err) {
      history.replace('/error', { errorData: err });
    } finally {
      history.push({
        loading: false
      });
    }
  }

  handleGroups(consumerGroups) {
    let tableConsumerGroups = [];
    consumerGroups.map(consumerGroup => {
      consumerGroup.size = 0;
      consumerGroup.logDirSize = 0;
      tableConsumerGroups.push({
        id: consumerGroup.id,
        state: consumerGroup.state,
        coordinator: consumerGroup.coordinator.id,
        members: consumerGroup.members.length,
        topicLag: consumerGroup.offsets
      });
    });
    this.setState({ consumerGroups: tableConsumerGroups });
  }

  handleState(state) {
    return (
      <span className={state === 'STABLE' ? 'badge badge-success' : 'badge badge-warning'}>
        {state}
      </span>
    );
  }

  handleCoordinator(coordinator) {
    return <span className="badge badge-primary"> {coordinator}</span>;
  }

  handleTopics(topics) {
    return topics.map(lagTopic => {
      return (
        <div
          onClick={() => {
            this.props.changeTab('data');
          }}
        >
          <Link
            to={{
              pathname: `/${this.state.selectedCluster}/topic/${lagTopic.topic}`
            }}
            key="lagTopic.topicId"
            className="btn btn-dark btn-sm mb-1"
          >
            {lagTopic.topic}
            <a href="#" className="badge badge-secondary">
              Lag:{lagTopic.partition}
            </a>
          </Link>
        </div>
      );
    });
  }

  render() {
    const { selectedCluster } = this.state;
    const { history } = this.props;

    return (
      <div>
        <Table
          columns={[
            {
              id: 'id',
              accessor: 'id',
              colName: 'Id'
            },
            {
              id: 'state',
              accessor: 'state',
              colName: 'State',
              cell: obj => {
                return this.handleState(obj.state);
              }
            },
            {
              id: 'coordinator',
              accessor: 'coordinator',
              colName: 'Coordinator',
              cell: obj => {
                return this.handleCoordinator(obj.coordinator);
              }
            },
            {
              id: 'members',
              accessor: 'members',
              colName: 'Members'
            },
            {
              id: 'topics',
              accessor: 'topics',
              colName: 'Topics',
              cell: obj => {
                if (obj.topicLag) {
                  return this.handleTopics(obj.topicLag);
                }
              }
            }
          ]}
          data={this.state.consumerGroups}
          onDetails={id => {
            history.push({ pathname: `/${selectedCluster}/group/${id}`, tab: constants.GROUP });
          }}
          actions={[constants.TABLE_DETAILS]}
        />
      </div>
    );
  }
}
export default TopicGroups;
