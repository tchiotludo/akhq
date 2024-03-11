import React from 'react';
import Header from '../../Header';
import Table from '../../../components/Table';
import * as constants from '../../../utils/constants';
import { uriNodes, uriNodePartitions } from '../../../utils/endpoints';
import Root from '../../../components/Root';
import { withRouter } from '../../../utils/withRouter';
import { Badge } from 'react-bootstrap';

class NodesList extends Root {
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
    const { clusterId } = this.props.params;
    nodes = await this.getApi(uriNodes(clusterId));
    this.handleData(nodes.data);
    this.setState({ selectedCluster: clusterId });
  }

  handleData(nodes) {
    const { clusterId } = this.props.params;
    let tableNodes = {};
    const setState = () => {
      this.setState({ data: Object.values(tableNodes), loading: false });
    };

    nodes.nodes.forEach(node => {
      tableNodes[node.id] = {
        id: JSON.stringify(node.id) || '',
        host: `${node.host}:${node.port}` || '',
        rack: node.rack || '',
        controller: nodes.controller && nodes.controller.id === node.id ? 'True' : 'False' || '',
        partition: undefined
      };
    });

    setState();

    this.getApi(uriNodePartitions(clusterId)).then(value => {
      for (let node of value.data) {
        const topicNode = tableNodes[node.id];
        tableNodes[node.id].partition = topicNode
          ? node.countLeader +
            ' (' +
            ((node.countLeader / node.totalPartitions) * 100).toFixed(2) +
            '%)'
          : '';
      }

      setState();
    });

    return Object.values(tableNodes);
  }

  render() {
    const { data, selectedCluster, loading } = this.state;
    return (
      <div>
        <Header title="Nodes" />
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
                return <Badge bg="primary">{obj[col.accessor] || ''}</Badge>;
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
              id: 'controller',
              accessor: 'controller',
              colName: 'Controller',
              type: 'text',
              sortable: true
            },
            {
              id: 'partition',
              accessor: 'partition',
              colName: 'Partitions (% of total)',
              type: 'text',
              sortable: false
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
          onDetails={id => `/ui/${selectedCluster}/node/${id}`}
        />
      </div>
    );
  }
}

export default withRouter(NodesList);
