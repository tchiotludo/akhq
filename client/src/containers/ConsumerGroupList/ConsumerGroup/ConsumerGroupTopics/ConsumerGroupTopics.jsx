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
      console.log('ClusterId: ', selectedCluster, ' + ConsumerGroup:  ', selectedConsumerGroup);
      topics = await get(endpoints.uriConsumerGroup(selectedCluster, selectedConsumerGroup));
      console.log('topicsData:        ', topics.data);
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
    console.log(topics);
    let data = topics.map(topic => {
      console.log(topic);
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

  handleMember(member) {
    if (member.empty === 'true') {
      return <div>-</div>;
    } else {
      return <div>{member.present}</div>;
    }
  }
  handleOffset(offset) {
    if (offset.empty === 'true') {
      return <div>-</div>;
    } else {
      return <div>{offset.present}</div>;
    }
  }
  handleLag(lag) {
    if (lag.empty === 'true') {
      return <div>-</div>;
    } else {
      return <div>{lag.present}</div>;
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
                return this.handleMember(obj);
              }
            },
            {
              id: 'offset',
              accessor: 'offset',
              colName: 'Offset',
              type: 'text',
              cell: obj => {
                return this.handleOffset(obj);
              }
            },
            {
              id: 'lag',
              accessor: 'lag',
              colName: 'Lag',
              type: 'text',
              cell: obj => {
                return this.handleLag(obj);
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
