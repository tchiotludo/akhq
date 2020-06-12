import React, { Component } from 'react';
import { get } from '../../../../utils/api';
import { uriNodesLogs } from '../../../../utils/endpoints';
import Table from '../../../../components/Table';
import { showBytes } from '../../../../utils/converters';
import { sortBy } from '../../../../utils/constants';

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
    const { history } = this.props;
    history.replace({
      loading: true
    });
    try {
      logs = await get(uriNodesLogs(selectedCluster, selectedNode));
      logs = logs.data.sort(sortBy('topic', false));
      logs = logs.sort(sortBy('partition', false));
      this.handleData(logs);
    } catch (err) {
      console.error('Error:', err);
    } finally {
      history.replace({
        loading: false
      });
    }
  }

  handleData(logs) {
    let tableNodes = logs.map(log => {
      return {
        broker: log.brokerId,
        topic: log.topic,
        partition: log.partition,
        size: showBytes(log.size),
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
          columns={[
            {
              id: 'broker',
              accessor: 'broker',
              colName: 'Broker',
              type: 'text'
            },
            {
              id: 'topic',
              accessor: 'topic',
              colName: 'Topic',
              type: 'text'
            },
            {
              id: 'partition',
              accessor: 'partition',
              colName: 'Partition',
              type: 'text'
            },
            {
              id: 'size',
              accessor: 'size',
              colName: 'Size',
              type: 'text'
            },
            {
              id: 'offsetLag',
              accessor: 'offsetLag',
              colName: 'OffsetLag',
              type: 'text'
            }
          ]}
          data={data}
        />
      </div>
    );
  }
}

export default NodeLogs;
