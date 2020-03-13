import React, { Component } from 'react';
import Table from '../../../../components/Table';
import { get } from '../../../../utils/api';
import endpoints from '../../../../utils/endpoints';
import constants from '../../../../utils/constants';

class ConsumerGroupTopics extends Component {
  state = {
    data: [],
    selectedCluster: this.props.clusterId,
    selectedConsumerGroup: this.props.consumerGroupId
  };

  componentDidMount() {
    this.getConsumerGroupTopics();
  }

  async getConsumerGroupTopics() {
    let topics = [];
    const { selectedCluster, selectedConsumerGroup } = this.state;
    const { history } = this.props;
    history.push({
      loading: true
    });
    try {
      topics = await get(endpoints.uriConsumerGroup(selectedCluster, selectedConsumerGroup));

      this.handleData(topics.data);
    } catch (err) {
      console.error('Error:', err);
    } finally {
      history.push({
        loading: false
      });
    }
  }
  
  
  handleData(topics) {
    let data = topics.map(topic => {
      return {
        name: topic.name,
        partition: topic.partition,
        member: topic.member,
        offset: topic.offset,
        lag: topic.lag
      };
    });
    this.setState({ data });
  }

  handleOptional(optional) {
    if (optional) {
      return <label>{optional}</label>;
    } else {
      return <label>-</label>;
    }
  }

  render() {
    const { data } = this.state;
    return (
      <div>
        <Table
          columns={[
            {
              id: 'name',
              accessor: 'name',
              colName: 'Name',
              type: 'text'
            },
            {
              id: 'partition',
              accessor: 'partition',
              colName: 'Partition',
              type: 'text'
            },
            {
              id: 'member',
              accessor: 'member',
              colName: 'Member',
              type: 'text',
              cell: obj => {
                return this.handleOptional(obj.member);
              }
            },
            {
              id: 'offset',
              accessor: 'offset',
              colName: 'Offset',
              type: 'text',
              cell: obj => {
                return this.handleOptional(obj.offset);
              }
            },
            {
              id: 'lag',
              accessor: 'lag',
              colName: 'Lag',
              type: 'text',
              cell: obj => {
                return this.handleOptional(obj.lag);
              }
            }
          ]}
          data={data}
        />
      </div>
    );
  }
}
export default ConsumerGroupTopics;
