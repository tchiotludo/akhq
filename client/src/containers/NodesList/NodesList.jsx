import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import Header from '../Header';
import Table from '../../components/Table';
import * as constants from '../../utils/constants';
import { get } from '../../services/api';
import { uriNodes } from '../../services/endpoints';

class NodesList extends Component {
  state = {
    data: [],
    selectedCluster: ''
  };

  componentDidMount() {
    this.getNodes();
  }

  async getNodes() {
    let nodes = [];
    let selectedClusterId = 'my-cluster';
    try {
      nodes = await get(uriNodes(selectedClusterId));
      this.handleData(nodes.data);
      this.setState({ selectedCluster: selectedClusterId });
    } catch (err) {
      console.log('Error:', err);
    }
  }

  handleData(nodes) {
    let tableNodes = nodes.map(node => {
      return {
        id: node.id || '',
        host: node.host || '',
        idToShow: <span className="badge badge-info">{node.id || ''}</span>,
        port: node.port || '',
        rack: node.rack || ''
      };
    });
    this.setState({ data: tableNodes });
  }

  render() {
    const { history } = this.props;
    const { data, selectedCluster } = this.state;
    return (
      <div id="content">
        <Header title="Nodes" />
        <Table
          colNames={['Id', 'Host', 'Racks']}
          toPresent={['idToShow', 'host', 'rack']}
          data={data}
          actions={[constants.TABLE_DETAILS]}
          onDetails={id => {
            history.push(`/${selectedCluster}/node/${id}`);
          }}
        />
      </div>
    );
  }
}

export default NodesList;
