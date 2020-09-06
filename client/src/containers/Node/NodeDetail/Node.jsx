import React, { Component } from 'react';
import Header from '../../Header';
import NodeConfigs from './/NodeConfigs/NodeConfigs';
import NodeLogs from './/NodeLogs/NodeLogs';
import {getSelectedTab} from "../../../utils/functions";

class Node extends Component {
  state = {
    host: '',
    port: '',
    data: [],
    clusterId: this.props.match.params.clusterId,
    selectedNode: this.props.match.params.nodeId,
    selectedTab: 'configs'
  };

  tabs = ['configs', 'logs'];

  componentDidMount() {
    const { clusterId, nodeId } = this.props.match.params;
    const tabSelected = getSelectedTab(this.props, this.tabs);
    this.setState(
        {
          selectedTab: (tabSelected)? tabSelected : 'configs'
        },
        () => {
          this.props.history.replace(`/ui/${clusterId}/node/${nodeId}/${this.state.selectedTab}`);
        }
    );
  }

  componentDidUpdate(prevProps, prevState) {
    if (this.props.location.pathname !== prevProps.location.pathname) {
      const tabSelected = getSelectedTab(this.props, this.tabs);
      this.setState({ selectedTab: tabSelected });
    }
  }

  selectTab = tab => {
    const { selectedNode, clusterId } = this.state;
    this.setState({ selectedTab: tab });
    this.props.history.push(`/ui/${clusterId}/node/${selectedNode}/${tab}`);
  };

  tabClassName = tab => {
    const { selectedTab } = this.state;
    return selectedTab === tab ? 'nav-link active' : 'nav-link';
  };

  renderSelectedTab() {
    const { selectedTab } = this.state;

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
            history={this.props.history}
          />
        );
    }
  }

  render() {
    const { selectedNode: nodeId } = this.state;
    return (
      <div>
        <Header title={`Node ${nodeId}`} history={this.props.history} />
        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            <li className="nav-item">
              <div
                className={this.tabClassName('configs')}
                onClick={() => this.selectTab('configs')}
                role="tab"
              >
                Configs
              </div>
            </li>
            <li className="nav-item">
              <div
                className={this.tabClassName('logs')}
                onClick={() => this.selectTab('logs')}
                role="tab"
              >
                Logs
              </div>
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
