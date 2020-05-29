import React, { Component } from 'react';
import Table from '../../../../components/Table';
import { get } from '../../../../utils/api';
import { uriConsumerGroupMembers } from '../../../../utils/endpoints';
import constants from '../../../../utils/constants';
import { Link } from 'react-router-dom';
import './styles.scss';
class ConsumerGroupMembers extends Component {
  state = {
    data: [],
    selectedCluster: this.props.clusterId,
    selectedConsumerGroup: this.props.consumerGroupId
  };

  componentDidMount() {
    this.getConsumerGroupMembers();
  }

  async getConsumerGroupMembers() {
    const { selectedCluster, selectedConsumerGroup } = this.state;
    const { history } = this.props;
    history.replace({
      loading: true
    });
    try {
      const members = await get(uriConsumerGroupMembers(selectedCluster, selectedConsumerGroup));

      this.handleData(members.data);
    } catch (err) {
      console.error('Error:', err);
    } finally {
      history.replace({
        loading: false
      });
    }
  }

  handleData(members) {
    const data = members.map(member => {
      return {
        clientId: member.clientId,
        id: member.id,
        host: member.host,
        assignments: member.assignments
      };
    });
    this.setState({ data });
  }

  handlePartitions(partitions) {
    return partitions.map(partition => {
      return (
        <a href="#" className="badge badge-secondary partition">
          {partition}
        </a>
      );
    });
  }

  handleAssignments(assignments) {
    const { history } = this.props;
    let topics = [];

    assignments.map(assignment => {
      if (!topics.find(topic => topic === assignment.topic)) {
        topics.push(assignment.topic);
      }
    });
    return topics.map(topic => {
      let partitions = [];
      assignments.map(assignment => {
        if (assignment.topic === topic) {
          partitions.push(assignment.partition);
        }
      });

      return (
        <div
          onClick={() => {
            history.push({
              pathname: `/ui/${this.state.selectedCluster}/topic/${topic}`,
              tab: constants.TOPIC
            });
          }}
        >
          <Link
            to={{
              pathname: `/ui/${this.state.selectedCluster}/topic/${topic}`
            }}
            key="topic"
            className="btn btn-primary btn-sm mb-1"
          >
            {topic}
            {this.handlePartitions(partitions)}
          </Link>
        </div>
      );
    });
  }

  render() {
    const { data } = this.state;
    return (
      <div>
        <Table
          columns={[
            {
              id: 'clientId',
              accessor: 'clientId',
              colName: 'ClientId',
              type: 'text'
            },
            {
              id: 'id',
              accessor: 'id',
              colName: 'Id',
              type: 'text'
            },
            {
              id: 'host',
              accessor: 'host',
              colName: 'Host',
              type: 'text'
            },
            {
              id: 'assignments',
              accessor: 'assignments',
              colName: 'Assignments',
              type: 'text',
              cell: obj => {
                return this.handleAssignments(obj.assignments);
              }
            }
          ]}
          data={data}
        />
      </div>
    );
  }
}

export default ConsumerGroupMembers;
