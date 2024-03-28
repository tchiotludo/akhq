import React from 'react';
import { uriNodesLogs } from '../../../../utils/endpoints';
import Table from '../../../../components/Table';
import { showBytes } from '../../../../utils/converters';
import { sortBy } from '../../../../utils/constants';
import Root from '../../../../components/Root';

class NodeLogs extends Root {
  state = {
    host: '',
    port: '',
    data: [],
    selectedCluster: this.props.clusterId,
    selectedNode: this.props.nodeId,
    loading: true
  };

  componentDidMount() {
    this.getNodesLogs();
  }

  async getNodesLogs() {
    let logs = [];
    const { selectedCluster, selectedNode } = this.state;

    logs = await this.getApi(uriNodesLogs(selectedCluster, selectedNode));
    logs = logs.data.sort(sortBy('partition', false)).sort(sortBy('topic', false));
    this.handleData(logs);
  }

  handleData(logs) {
    let tableNodes = logs.map(log => {
      return {
        broker: log.brokerId,
        topic: log.topic,
        partition: log.partition,
        size: showBytes(log.size),
        offsetLag: Number(log.offsetLag).toLocaleString()
      };
    });
    this.setState({ data: tableNodes, loading: false });
  }

  render() {
    const { data, loading } = this.state;
    return (
      <div>
        <Table
          loading={loading}
          columns={[
            {
              id: 'broker',
              accessor: 'broker',
              colName: 'Broker',
              type: 'text',
              sortable: true
            },
            {
              id: 'topic',
              accessor: 'topic',
              colName: 'Topic',
              type: 'text',
              sortable: true
            },
            {
              id: 'partition',
              accessor: 'partition',
              colName: 'Partition',
              type: 'text',
              sortable: true
            },
            {
              id: 'size',
              accessor: 'size',
              colName: 'Size',
              type: 'text',
              sortable: true
            },
            {
              id: 'offsetLag',
              accessor: 'offsetLag',
              colName: 'OffsetLag',
              type: 'text',
              sortable: true
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

export default NodeLogs;
