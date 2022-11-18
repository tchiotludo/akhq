import React from 'react';
import Table from '../../../../components/Table';
import { uriConsumerGroupOffsets } from '../../../../utils/endpoints';
import { Link } from 'react-router-dom';
import Root from '../../../../components/Root';

class ConsumerGroupTopics extends Root {
  state = {
    data: [],
    selectedCluster: this.props.clusterId,
    selectedConsumerGroup: this.props.consumerGroupId,
    loading: true
  };

  componentDidMount() {
    this.getConsumerGroupTopics();
  }

  async getConsumerGroupTopics() {
    let offsets = [];
    const { selectedCluster, selectedConsumerGroup } = this.state;

    offsets = await this.getApi(uriConsumerGroupOffsets(selectedCluster, selectedConsumerGroup));
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
    this.setState({ data, loading: false });
  }

  handleOptional(optional) {
    if (optional !== undefined && optional !== '' && optional !== 'NaN') {
      return <label>{optional}</label>;
    } else {
      return <label>-</label>;
    }
  }

  render() {
    const { data, loading } = this.state;
    return (
      <div>
        <Table
          loading={loading}
          history={this.props.history}
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
                if (obj.offset !== undefined && obj.offset !== '') {
                  return (
                    <Link
                      to={`/ui/${this.state.selectedCluster}/topic/${
                        obj.name
                      }/data?sort=Oldest&partition=${obj.partition}&after=${obj.partition}-${
                        obj.offset - 1
                      }`}
                    >
                      {obj.offset}
                    </Link>
                  );
                }
                return <label>-</label>;
              }
            },
            {
              id: 'metadata',
              accessor: 'metadata',
              colName: 'Metadata',
              type: 'text',
              cell: obj => {
                return this.handleOptional(obj.metadata);
              }
            },
            {
              id: 'lag',
              accessor: 'lag',
              colName: 'Lag',
              type: 'text',
              cell: obj => {
                return this.handleOptional(Number(obj.lag).toLocaleString());
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
