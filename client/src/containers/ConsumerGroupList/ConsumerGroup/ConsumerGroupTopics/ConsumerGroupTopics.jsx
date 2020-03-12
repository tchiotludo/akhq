import React, { Component } from 'react';
import Table from '../../../../components/Table/Table';
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
      console.log('ClusterId: ', selectedCluster, ' + ConsumerGroup:  ', selectedConsumerGroup);
      topics = await get(endpoints.uriConsumerGroup(selectedCluster, selectedConsumerGroup));
      console.log('topicos:        ', topics);
      this.handleData(topics.data);
    } catch (err) {
      console.error('Error:', err);
    } finally {
      history.push({
        loading: false
      });
    }
  }
  //Verificar o que estqa a vir dop backend
  handleData(topics) {
    let tableTopics = topics.map(topic => {
      return {
        name: topic.consumerGroupId,
        partition: topic.partition,
        member: topic.member,
        offset: topic.offset,
        lag: topic.lag
      };
    });
    this.setState({ data: tableTopics });
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
              type: 'text'
            },
            {
              id: 'offset',
              accessor: 'offset',
              colName: 'Offset',
              type: 'text'
            },
            {
              id: 'lag',
              accessor: 'lag',
              colName: 'Lag',
              type: 'text'
            }
          ]}
          data={data}
        />
      </div>
    );
  }
}
export default ConsumerGroupTopics;
