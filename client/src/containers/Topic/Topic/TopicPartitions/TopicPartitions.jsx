import React from 'react';
import { uriTopicsPartitions } from '../../../../utils/endpoints';
import Table from '../../../../components/Table';
import converters from '../../../../utils/converters';
import Root from '../../../../components/Root';

class TopicPartitions extends Root {
  state = {
    data: [],
    selectedCluster: this.props.clusterId,
    selectedTopic: this.props.topic,
    loading: true
  };
  componentDidMount() {
    this.getTopicsPartitions();
  }

  async getTopicsPartitions() {
    const { selectedCluster, selectedTopic } = this.state;

    let partitions = await this.getApi(uriTopicsPartitions(selectedCluster, selectedTopic));
    this.handleData(partitions.data);
  }

  handleData(partitions) {
    let tablePartitions = partitions.map(partition => {
      return {
        id: partition.id,
        leader: partition.leader.id,
        replicas: partition.nodes,
        offsets: (
          <label>
            {partition.firstOffset} ⤑ {partition.lastOffset}
          </label>
        ),
        size: partition
      };
    });
    this.setState({ data: tablePartitions, loading: false });
  }

  handleLeader(leader) {
    return <span className="badge badge-primary"> {leader}</span>;
  }

  handleReplicas(replicas) {
    return replicas.map(replica => {
      return (
        <span
          key={replica.id}
          className={replica.inSyncReplicas ? 'badge badge-success' : 'badge badge-danger'}
        >
          {' '}
          {replica.id}
        </span>
      );
    });
  }

  handleSize(size) {
    return (
      <label>
        {size.lastOffset - size.firstOffset} - {converters.showBytes(size.logDirSize, 0)}
      </label>
    );
  }

  render() {
    const { data, loading } = this.state;
    return (
      <div>
        <Table
          loading={loading}
          columns={[
            {
              id: 'id',
              accessor: 'id',
              colName: 'Id',
              type: 'text',
              sortable: true
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
              sortable: true
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
          data={data}
          updateData={data => {
            this.setState({ data });
          }}
        />
      </div>
    );
  }
}

export default TopicPartitions;
