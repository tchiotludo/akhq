import React, { Component } from 'react';
import Table from '../../../../components/Table';
import { get } from '../../../../utils/api';
import { uriConsumerGroupOffsets } from '../../../../utils/endpoints';
import { Link } from 'react-router-dom';

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
    let offsets = [];
    const { selectedCluster, selectedConsumerGroup } = this.state;

    offsets = await get(uriConsumerGroupOffsets(selectedCluster, selectedConsumerGroup));
    offsets = offsets.data;
    this.handleData(offsets);
  }

  handleData(offsets) {
    let data = offsets.map(offset => {
      return {
        name: offset.topic,
        partition: offset.partition,
        member: offset.member ? offset.member.host : '',
        offset: offset.offset,
        lag: offset.offsetLag
      };
    });
    this.setState({ data });
  }

  handleOptional(optional) {
    if (optional !== undefined && optional !== '') {
      return <label>{optional}</label>;
    } else {
      return <label>-</label>;
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
              type: 'text',
              sortable: true,
              cell: (obj, col) => {
                return (
                  <Link to={`/ui/${this.state.selectedCluster}/topic/${obj[col.accessor]}`}>
                    {obj[col.accessor]}
                  </Link>
                );
              }
            },
            {
              id: 'partition',
              accessor: 'partition',
              colName: 'Partition',
              type: 'text',
              sortable: true
            },
            {
              id: 'member',
              accessor: 'member',
              colName: 'Member',
              type: 'text',
              cell: obj => {
                return this.handleOptional(obj.member.host);
              }
            },
            {
              id: 'offset',
              accessor: 'offset',
              colName: 'Offset',
              type: 'text',
              cell: obj => {
                return this.handleOptional(obj.offset);
              }
            },
            {
              id: 'lag',
              accessor: 'lag',
              colName: 'Lag',
              type: 'text',
              cell: obj => {
                return this.handleOptional(obj.lag);
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
export default ConsumerGroupTopics;
