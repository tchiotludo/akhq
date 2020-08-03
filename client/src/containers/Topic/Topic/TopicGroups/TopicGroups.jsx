import React, { Component } from 'react';
import Table from '../../../../components/Table';
import api from '../../../../utils/api';
import { uriTopicsGroups } from '../../../../utils/endpoints';
import constants from '../../../../utils/constants';

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
    const { selectedCluster, topicId } = this.state;

    let data  = await api.get(uriTopicsGroups(selectedCluster, topicId));
    data = data.data;
    if (data) {
      if (data) {
        this.handleGroups(data);
      } else {
        this.setState({ consumerGroup: [] });
      }
      this.setState({ selectedCluster });
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
    this.setState({ consumerGroups: tableConsumerGroups });
  }

  groupTopics(topics) {
    if (!topics) return {};
    return topics.reduce(function(a, e) {
      let key = e.topic;
      a[key] ? (a[key] = a[key] + e.offsetLag) : (a[key] = e.offsetLag);
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
        <a
          href={`/ui/${this.state.selectedCluster}/topic/${topic}`}
          key="lagTopic.topicId"
          className="btn btn-dark btn-sm mb-1 mr-1"
          onClick={noPropagation}
        >
          {topic}
          <div className="badge badge-secondary">Lag: {topics[topic]}</div>
        </a>
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
          onDetails={id => {
            history.push({ pathname: `/ui/${selectedCluster}/group/${id}`, tab: constants.GROUP });
          }}
          actions={[constants.TABLE_DETAILS]}
        />
      </div>
    );
  }
}
export default TopicGroups;
