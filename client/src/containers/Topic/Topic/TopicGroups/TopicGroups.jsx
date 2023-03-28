import React from 'react';
import Table from '../../../../components/Table';
import { uriTopicsGroups } from '../../../../utils/endpoints';
import constants from '../../../../utils/constants';
import Root from '../../../../components/Root';
import { Link } from 'react-router-dom';

class TopicGroups extends Root {
  state = {
    consumerGroups: [],
    topicId: this.props.topicId,
    showDeleteModal: false,
    selectedCluster: this.props.clusterId,
    deleteMessage: '',
    loading: true
  };

  componentDidMount() {
    this.getConsumerGroup();
  }

  async getConsumerGroup() {
    const { selectedCluster, topicId } = this.state;

    let data = await this.getApi(uriTopicsGroups(selectedCluster, topicId));
    if (data && data.data) {
      this.handleGroups(data.data);
    } else {
      this.setState({ consumerGroup: [], loading: false });
    }
  }

  handleGroups(consumerGroups) {
    let tableConsumerGroups =
      consumerGroups.map(consumerGroup => {
        return {
          id: consumerGroup.id,
          state: consumerGroup.state,
          coordinator: consumerGroup.coordinator.id,
          members: consumerGroup.members ? consumerGroup.members.length : 0,
          topics: this.groupTopics(consumerGroup.offsets)
        };
      }) || [];
    this.setState({ consumerGroups: tableConsumerGroups, loading: false });
  }

  groupTopics(topics) {
    if (!topics) return {};
    return topics.reduce(function (a, e) {
      let key = e.topic;
      a[key] ? (a[key] = a[key] + e.offsetLag) : (a[key] = e.offsetLag || 0);
      return a;
    }, {});
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
    const noPropagation = e => e.stopPropagation();
    return Object.keys(topics).map(topic => {
      return (
        <Link
          to={`/ui/${this.state.selectedCluster}/topic/${topic}`}
          key="lagTopic.topicId"
          className="btn btn-dark btn-sm mb-1 mr-1"
          onClick={noPropagation}
        >
          {topic}
          <div className="badge badge-secondary">Lag: {Number(topics[topic]).toLocaleString()}</div>
        </Link>
      );
    });
  }

  render() {
    const { selectedCluster, loading } = this.state;

    return (
      <div>
        <Table
          loading={loading}
          history={this.props.history}
          columns={[
            {
              id: 'id',
              accessor: 'id',
              colName: 'Id',
              sortable: true
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
              colName: 'Members',
              sortable: true
            },
            {
              id: 'topics',
              accessor: 'topics',
              colName: 'Topics',
              cell: obj => {
                if (obj.topics) {
                  return this.handleTopics(obj.topics);
                }
              }
            }
          ]}
          data={this.state.consumerGroups}
          updateData={data => {
            this.setState({ consumerGroups: data });
          }}
          onDetails={id => `/ui/${selectedCluster}/group/${id}`}
          actions={[constants.TABLE_DETAILS]}
        />
      </div>
    );
  }
}
export default TopicGroups;
