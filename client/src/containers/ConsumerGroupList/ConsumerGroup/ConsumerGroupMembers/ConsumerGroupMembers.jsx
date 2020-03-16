import React, { Component } from 'react';
import Table from '../../../../components/Table';
import { get } from '../../../../utils/api';
import endpoints from '../../../../utils/endpoints';
import constants from '../../../../utils/constants';
import { Link } from 'react-router-dom';
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
    history.push({
      loading: true
    });
    try {
      let members = await get(
        endpoints.uriConsumerGroupMembers(selectedCluster, selectedConsumerGroup)
      );

      this.handleData(members.data);
    } catch (err) {
      console.error('Error:', err);
    } finally {
      history.push({
        loading: false
      });
    }
  }

  handleData(members) {
   
    let data = members.map(member => {
      return {
        clientId: member.clientId,
        id: member.id,
        host: member.host,
        assignments: member.assignments
      };
    });
    this.setState({ data });
  }

  handleAssignments(assignments) {
    return assignments.map(assignment => {
      return (
        <Link
          to={{
            pathname: `/${this.state.selectedCluster}/topic/${assignment.topic}`
          }}
          key="assignment"
          className="btn btn-primary btn-sm mb-1"
        >
          {assignment.topic}
          <a href="#" className="badge badge-secondary">
            {assignment.partition}
          </a>
        </Link>
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
