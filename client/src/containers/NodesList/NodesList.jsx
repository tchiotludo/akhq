import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import Header from '../Header';
import Table from '../../components/Table';
import * as constants from '../../utils/constants';

class NodesList extends Component {
  state = {
    data: []
  };

  componentDidMount() {
    this.getNodes();
  }

  getNodes() {
    let nodes = [];
    //api call
    this.handleData(nodes);
  }

  handleData(nodes) {
    let tableNodes = nodes.map(node => {
      return {
        id: node.id,
        host: node.host,
        idToShow: <span>{node.id}</span>
      };
    });
    this.setState({ nodes });
  }

  render() {
    const { history } = this.props;
    return (
      <div id="content">
        <Header title="Nodes" />
        <Table
          colNames={['Id', 'Host', 'Racks']}
          toPresent={['idToShow', 'host', 'rack']}
          data={[
            {
              id: 1102,
              idToShow: <span className="badge badge-info">1102</span>,
              host: 'kafka9092',
              rack: '',
              url: ''
            },
            {
              id: 1102,
              idToShow: <span className="badge badge-info">1102</span>,
              host: 'kafka9092',
              rack: '',
              url: ''
            }
          ]}
          actions={[constants.TABLE_DETAILS]}
          onDetails={() => {
            history.push('/');
          }}
        />
      </div>
    );
  }
}

export default NodesList;
