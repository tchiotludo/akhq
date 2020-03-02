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
    let tablePartitions = partitions.map(partition => {
      return {
        id: partition.id,
        leader: partition.leader,
        replicas: partition.replicas,
        offsets: partition.offsets,
        size: partition.size
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
        <span
          key={replica.id}
          className={replica.inSync ? 'badge badge-success' : 'badge badge-danger'}
        >
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
        {size.minSize} - {converters.showBytes(size.maxSize, 0)}
      </label>
    );
  }

  render() {
    const { data } = this.state;
    return (
      <div>
        <Table
          columns={[
            {
              id: 'id',
              accessor: 'id',
              colName: 'Id',
              type: 'text'
            },
            {
              id: 'leader',
              accessor: 'leader',
              colName: 'Leader',
              type: 'text',
              cell: (obj, col) => {
                return this.handleLeader(obj[col.accessor]);
              }
            },
            {
              id: 'replicas',
              accessor: 'replicas',
              colName: 'Replicas',
              type: 'text',
              cell: (obj, col) => {
                return this.handleReplicas(obj[col.accessor]);
              }
            },
            {
              id: 'offsets',
              accessor: 'offsets',
              colName: 'Offsets',
              type: 'text',
              cell: (obj, col) => {
                return this.handleOffsets(obj[col.accessor]);
              }
            },
            {
              id: 'size',
              accessor: 'size',
              colName: 'Size',
              type: 'text',
              cell: (obj, col) => {
                return this.handleSize(obj[col.accessor]);
              }
            }
          ]}
          //colNames={['id', 'Leader', 'Replicas', 'Offsets', 'Size']}
          //toPresent={['id', 'leader', 'replicas', 'offsets', 'size']}
          data={data}
        />
      </div>
    );
  }
}

export default TopicPartitions;
