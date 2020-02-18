import React, { Component } from 'react';
import Header from '../Header';

class NodeDetails extends Component {
  state = {
    host: '',
    port: ''
  };

  componentDidMount() {}

  render() {
    const { host, port } = this.state;
    const { nodeId } = this.props.match.params;
    return (
      <div id="content" style={{ height: '100%' }}>
        <Header title={`Node: ${nodeId}`} />
      </div>
    );
  }
}

export default NodeDetails;
