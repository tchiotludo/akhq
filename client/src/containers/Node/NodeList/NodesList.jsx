import React, { Component } from 'react';
import Header from '../../Header';
import Table from '../../../components/Table';
import * as constants from '../../../utils/constants';
import { get } from '../../../utils/api';
import { uriNodes } from '../../../utils/endpoints';

class NodesList extends Component {
  state = {
    data: [],
    selectedCluster: '',
    loading: true
  };

  componentDidMount() {
    this.getNodes();
  }

  async getNodes() {
    let nodes = [];
    const { clusterId } = this.props.match.params;

    nodes = await get(uriNodes(clusterId));
    this.handleData(nodes.data);
    this.setState({ selectedCluster: clusterId });
  }

  handleData(nodes) {
    let tableNodes = nodes.nodes.map(node => {
      return {
        id: JSON.stringify(node.id) || '',
        host: `${node.host}:${node.port}` || '',
        rack: node.rack || ''
      };
    });
    this.setState({ data: tableNodes, loading: false });
    return tableNodes;
  }

  render() {
    const { history } = this.props;
    const { data, selectedCluster, loading } = this.state;
    return (
      <div>
        <Header title="Nodes" history={history} />
        <Table
          loading={loading}
          columns={[
            {
              id: 'id',
              accessor: 'id',
              colName: 'Id',
              type: 'text',
              sortable: true,
              cell: (obj, col) => {
                return <span className="badge badge-info">{obj[col.accessor] || ''}</span>;
              }
            },
            {
              id: 'host',
              accessor: 'host',
              colName: 'Host',
              type: 'text',
              sortable: true
            },
            {
              id: 'rack',
              accessor: 'rack',
              colName: 'Rack',
              type: 'text',
              sortable: true
            }
          ]}
          data={data}
          updateData={data => {
            this.setState({ data });
          }}
          actions={[constants.TABLE_DETAILS]}
          onDetails={id => {
            history.push(`/ui/${selectedCluster}/node/${id}`);
          }}
        />
      </div>
    );
  }
}

export default NodesList;
