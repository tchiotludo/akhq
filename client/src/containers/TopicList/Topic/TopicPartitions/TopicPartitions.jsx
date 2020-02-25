import React, { Component } from 'react';
import { uriTopicsPartitions } from '../../../../utils/endpoints';
import Table from '../../../../components/Table';
import { get } from '../../../../utils/api';

class TopicPartitions extends Component {
  state = {
    data: [],
    selectedCluster: this.props.clusterId,
    selectedTopic: this.props.topic.id
  };

  componentDidMount() {
    this.getTopicsPartitions();
    console.log('props: ', this.props);
  }

  async getTopicsPartitions() {
    let partitions = [];
    const { selectedCluster, selectedTopic } = this.state;

    try {
        partitions = await get(uriTopicsPartitions(selectedCluster, selectedTopic));
      this.handleData(partitions.data);
    } catch (err) {
      console.error('Error:', err);
    }
  }

  handleData(partitions) {
    console.log('partitions',partitions);
    let tablePartitions = partitions.map(partition => {
      return {
        id: partition.id,
        leader: partition.leader,
        partition: partition.partition,
        size: partition.size,
        offsetLag: partition.offsetLag
      };
    });
    this.setState({ data: tablePartitions });
  }

  render() {
    const { data, selectedTopic, selectedCluster } = this.state;
    return (
      <div>
        <Table
          colNames={['id', 'Leader', 'Replicas', 'Offset', 'Size']}
          toPresent={['id', 'leader', 'replicas', 'offset','size']}
          data={data}
        />
      </div>
    );
  }
}

export default TopicPartitions;
