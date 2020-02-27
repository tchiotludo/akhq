import React, { Component } from 'react';
import { uriTopicsPartitions } from '../../../../utils/endpoints';
import Table from '../../../../components/Table';
import { get } from '../../../../utils/api';
import converters from '../../../../utils/converters';

class TopicPartitions extends Component {
  state = {
    data: [],
    selectedCluster: this.props.clusterId,
    selectedTopic: this.props.topic
  };

  componentDidMount() {
    this.getTopicsPartitions();
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

    if (!partitions) {
      console.log('Not getting anything from backend');
    }
    let tablePartitions = partitions.map(partition => {
      return {
        id: partition.id,
        leader: this.handleLeader(partition.leader),
        replicas: this.handleReplicas(partition.replicas),
        offsets: this.handleOffsets(partition.offsets),
        size: this.handleSize(partition.size)
      };
    });
    this.setState({ data: tablePartitions });
  }

  handleLeader(leader) {
    return <span className="badge badge-primary"> {leader}</span>;
  }

  handleReplicas(replicas) {
    return replicas.map(replica => {
      return (
        <span key={replica.id} className={replica.inSync ? 'badge badge-success' : 'badge badge-danger'}>
          {' '}
          {replica.id}
        </span>
      );
    });
  }

  handleOffsets(offsets) {
    return (
      <label>
        {offsets.firstOffset} ⤑ {offsets.lastOffset}
      </label>
    );
  }

  handleSize(size) {
    return (
      <label>
        {size.minSize} - {converters.showBytes(size.maxSize,0)}
      </label>
    );
  }

  render() {
    const { data } = this.state;
    return (
      <div>
        <Table
          colNames={['id', 'Leader', 'Replicas', 'Offsets', 'Size']}
          toPresent={['id', 'leader', 'replicas', 'offsets', 'size']}
          data={data}
        />
      </div>
    );
  }
}

export default TopicPartitions;
