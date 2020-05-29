import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import Header from '../Header';
import Table from '../../components/Table';
import * as constants from '../../utils/constants';
import { get } from '../../utils/api';
import { uriNodes } from '../../utils/endpoints';

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
    const { clusterId } = this.props.match.params;
    const { history } = this.props;
    history.replace({
      loading: true
    });
    try {
      nodes = await get(uriNodes(clusterId));
      this.handleData(nodes.data);
      this.setState({ selectedCluster: clusterId });
    } catch (err) {
      if (err.response && err.response.status === 404) {
        history.replace('/ui/page-not-found', { errorData: err });
      } else {
        history.replace('/ui/error', { errorData: err });
      }
    } finally {
      history.replace({
        loading: false
      });
    }
  }

  handleData(nodes) {
    let tableNodes = nodes.nodes.map(node => {
      return {
        id: node.id || '',
        host: `${node.host}:${node.port}` || '',
        rack: node.rack || ''
      };
    });
    this.setState({ data: tableNodes });
    return tableNodes;
  }

  render() {
    const { history } = this.props;
    const { data, selectedCluster } = this.state;
    return (
      <div>
        <Header title="Nodes" history={history} />
        <Table
          columns={[
            {
              id: 'id',
              accessor: 'id',
              colName: 'Id',
              type: 'text',
              cell: (obj, col) => {
                return <span className="badge badge-info">{obj[col.accessor] || ''}</span>;
              }
            },
            {
              id: 'host',
              accessor: 'host',
              colName: 'Host',
              type: 'text'
            },
            {
              id: 'rack',
              accessor: 'rack',
              colName: 'Rack',
              type: 'text'
            }
          ]}
          data={data}
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
