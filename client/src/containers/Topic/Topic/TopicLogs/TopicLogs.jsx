import React, { Component } from 'react';
import { uriTopicsLogs } from '../../../../utils/endpoints';
import Table from '../../../../components/Table';
import { get } from '../../../../utils/api';
import converters from '../../../../utils/converters';

class TopicLogs extends Component {
  state = {
    data: [],
    selectedCluster: this.props.clusterId,
    selectedTopic: this.props.topic,
    loading: true
  };

  componentDidMount() {
    this.getTopicLogs();
  }

  async getTopicLogs() {
    const { selectedCluster, selectedTopic } = this.state;

    let logs = await get(uriTopicsLogs(selectedCluster, selectedTopic));
    this.handleData(logs.data);
  }

  handleData(logs) {
    let tableLogs = logs.map(log => {
      return {
        broker: log.brokerId,
        topic: log.topic,
        partition: log.partition,
        size: log.size,
        offsetLag: log.offsetLag
      };
    });
    this.setState({ data: tableLogs, loading: false });
  }

  handleSize(size) {
    return <label>{converters.showBytes(size, 0)}</label>;
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
              cell: (obj, col) => {
                return this.handleSize(obj[col.accessor]);
              }
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

export default TopicLogs;
