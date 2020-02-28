import React, { Component } from 'react';
import { get } from '../../../../utils/api';
import { uriNodesLogs } from '../../../../utils/endpoints';
import Table from '../../../../components/Table';

class NodeLogs extends Component {
  state = {
    host: '',
    port: '',
    data: [],
    selectedCluster: this.props.clusterId,
    selectedNode: this.props.nodeId
  };

  componentDidMount() {
    this.getNodesLogs();
  }

  async getNodesLogs() {
    let logs = [];
    const { selectedCluster, selectedNode } = this.state;

    try {
      logs = await get(uriNodesLogs(selectedCluster, selectedNode));
      this.handleData(logs.data);
    } catch (err) {
      console.error('Error:', err);
    }
  }

  handleData(logs) {
    let tableNodes = logs.map(log => {
      return {
        broker: log.broker,
        topic: log.topic,
        partition: log.partition,
        size: log.size,
        offsetLag: log.offsetLag
      };
    });
    this.setState({ data: tableNodes });
  }

  render() {
    const { data } = this.state;
    return (
      <div>
        <Table
          colNames={['Broker', 'Topic', 'Partition', 'Size', 'OffsetLag']}
          toPresent={['broker', 'topic', 'partition', 'size', 'offsetLag']}
          data={data}
        />
      </div>
    );
  }
}

export default NodeLogs;
