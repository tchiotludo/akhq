import React, {Component} from 'react';
import {Link} from 'react-router-dom';
import Header from '../common/header';
import Table from '../Table';

class Node extends Component {
  render() {
    return (
      <div id="content">
        <Header title="Nodes" />
        <Table
          has2Headers
          firstHeader={[
            {colName: 'Node1', colSpan: 2},
            {colName: 'Node2', colSpan: 2}
          ]}
          colNames={['Test', 'Test2', 'Test3', 'Test4']}
        />
      </div>
    );
  }
}

export default Node;
