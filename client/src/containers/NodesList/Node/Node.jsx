import React, { Component } from 'react';
import Header from '../../Header';
import NodeConfigs from '../Node/NodeConfigs/NodeConfigs';
import NodeLogs from '../Node/NodeLogs/NodeLogs';
import { Link } from 'react-router-dom';

class Node extends Component {
  state = {
    host: '',
    port: '',
    data: [],
    clusterId: this.props.match.params.clusterId,
    selectedNode: this.props.match.params.nodeId,
    selectedTab: 'configs'
  };

  componentDidMount() {
    const { clusterId } = this.props.match.params;

    this.setState({ clusterId });
  }

  selectTab = tab => {
    this.setState({ selectedTab: tab });
  };

  tabClassName = tab => {
    const { selectedTab } = this.state;
    return selectedTab === tab ? 'nav-link active' : 'nav-link';
  };

  renderSelectedTab() {
    const { selectedTab, node } = this.state;

    switch (selectedTab) {
      case 'configs':
        return (
          <NodeConfigs
            nodeId={this.props.match.params.nodeId}
            clusterId={this.props.match.params.clusterId}
            history={this.props.history}
          />
        );
      case 'logs':
        return (
          <NodeLogs
            nodeId={this.props.match.params.nodeId}
            clusterId={this.props.match.params.clusterId}
            history={this.props.history}
          />
        );
      default:
        return (
          <NodeConfigs
            nodeId={this.props.match.params.nodeId}
            clusterId={this.props.match.params.clusterId}
          />
        );
    }
  }

  render() {
    const { data, selectedNode: nodeId, selectedCluster: clusterId } = this.state;
    return (
      <div id="content">
        <Header title={`Node ${nodeId}`} />
        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            <li className="nav-item">
              <Link
                className={this.tabClassName('configs')}
                onClick={() => this.selectTab('configs')}
                to="#"
                role="tab"
              >
                Configs
              </Link>
            </li>
            <li className="nav-item">
              <Link
                className={this.tabClassName('logs')}
                onClick={() => this.selectTab('logs')}
                to="#"
                role="tab"
              >
                Logs
              </Link>
            </li>
          </ul>

          <div className="tab-content">
            <div className="tab-pane active" role="tabpanel">
              {this.renderSelectedTab()}
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default Node;
