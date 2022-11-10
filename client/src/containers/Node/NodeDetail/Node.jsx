import React, { Component } from 'react';
import PropTypes from 'prop-types';
import Header from '../../Header';
import NodeConfigs from './/NodeConfigs/NodeConfigs';
import NodeLogs from './/NodeLogs/NodeLogs';
import { getSelectedTab } from '../../../utils/functions';
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

  tabs = ['configs', 'logs'];

  componentDidMount() {
    const { clusterId, nodeId } = this.props.match.params;
    const tabSelected = getSelectedTab(this.props, this.tabs);
    this.setState(
      {
        selectedTab: tabSelected ? tabSelected : 'configs'
      },
      () => {
        this.props.history.replace(`/ui/${clusterId}/node/${nodeId}/${this.state.selectedTab}`);
      }
    );
  }

  componentDidUpdate(prevProps) {
    if (this.props.location.pathname !== prevProps.location.pathname) {
      const tabSelected = getSelectedTab(this.props, this.tabs);
      this.setState({ selectedTab: tabSelected });
    }
  }

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
    const { selectedNode, clusterId } = this.state;
    return (
      <div>
        <Header title={`Node ${selectedNode}`} history={this.props.history} />
        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            <li className="nav-item">
              <Link
                to={`/ui/${clusterId}/node/${selectedNode}/configs`}
                className={this.tabClassName('configs')}
              >
                Configs
              </Link>
            </li>
            <li className="nav-item">
              <Link
                to={`/ui/${clusterId}/node/${selectedNode}/logs`}
                className={this.tabClassName('logs')}
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

Node.propTypes = {
  history: PropTypes.object,
  match: PropTypes.object,
  location: PropTypes.object,
  clusters: PropTypes.array,
  children: PropTypes.any
};

export default Node;
