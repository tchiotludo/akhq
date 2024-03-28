import React, { Component } from 'react';
import PropTypes from 'prop-types';
import Header from '../../Header';
import NodeConfigs from './/NodeConfigs/NodeConfigs';
import NodeLogs from './/NodeLogs/NodeLogs';
import { getSelectedTab } from '../../../utils/functions';
import { Link } from 'react-router-dom';
import { withRouter } from '../../../utils/withRouter';

class Node extends Component {
  state = {
    host: '',
    port: '',
    data: [],
    clusterId: this.props.params.clusterId,
    selectedNode: this.props.params.nodeId,
    selectedTab: 'logs',
    roles: JSON.parse(sessionStorage.getItem('roles'))
  };

  tabs = ['configs', 'logs'];

  componentDidMount() {
    const { clusterId, nodeId } = this.props.params;
    const { roles } = this.state;
    let tabSelected = getSelectedTab(this.props, this.tabs);

    if (tabSelected === 'configs' && roles.NODE && !roles.NODE.includes('READ_CONFIG')) {
      tabSelected = 'logs';
    }

    this.setState(
      {
        selectedTab: tabSelected
      },
      () => {
        this.props.router.navigate(`/ui/${clusterId}/node/${nodeId}/${this.state.selectedTab}`, {
          replace: true
        });
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
          <NodeConfigs nodeId={this.props.params.nodeId} clusterId={this.props.params.clusterId} />
        );
      case 'logs':
        return (
          <NodeLogs nodeId={this.props.params.nodeId} clusterId={this.props.params.clusterId} />
        );
      default:
        return (
          <NodeConfigs nodeId={this.props.params.nodeId} clusterId={this.props.params.clusterId} />
        );
    }
  }

  render() {
    const { selectedNode, clusterId, roles } = this.state;
    return (
      <div>
        <Header title={`Node ${selectedNode}`} />
        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            {roles.NODE && roles.NODE.includes('READ_CONFIG') && (
              <li className="nav-item">
                <Link
                  to={`/ui/${clusterId}/node/${selectedNode}/configs`}
                  className={this.tabClassName('configs')}
                >
                  Configs
                </Link>
              </li>
            )}
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
  router: PropTypes.object,
  params: PropTypes.object,
  location: PropTypes.object,
  clusters: PropTypes.array,
  children: PropTypes.any
};

export default withRouter(Node);
