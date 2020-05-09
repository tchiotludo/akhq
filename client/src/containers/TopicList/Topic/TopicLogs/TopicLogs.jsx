import React, { Component } from 'react';
import { uriTopicsLogs } from '../../../../utils/endpoints';
import Table from '../../../../components/Table';
import { get } from '../../../../utils/api';
import converters from '../../../../utils/converters';

class TopicLogs extends Component {
  state = {
    data: [],
    selectedCluster: this.props.clusterId,
    selectedTopic: this.props.topic
  };

  componentDidMount() {
    this.getTopicLogs();
  }

  async getTopicLogs() {
    let logs = [];
    const { selectedCluster, selectedTopic } = this.state;
    const { history } = this.props;
    history.replace({
      loading: true
    });
    try {
      logs = await get(uriTopicsLogs(selectedCluster, selectedTopic));
      this.handleData(logs.data);
    } catch (err) {
      console.error('Error:', err);
    } finally {
      history.replace({
        loading: false
      });
    }
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
    this.setState({ data: tableLogs });
  }

  handleSize(size) {
    return <label>{converters.showBytes(size, 0)}</label>;
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
              type: 'text',
              cell: (obj, col) => {
                return this.handleSize(obj[col.accessor]);
              }
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

export default TopicLogs;
