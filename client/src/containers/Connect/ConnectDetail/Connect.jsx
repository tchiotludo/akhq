import React, { Component } from 'react';
import PropTypes from 'prop-types';
import Header from '../../Header/Header';
import ConnectTasks from './ConnectTasks/ConnectTasks';
import ConnectConfigs from './ConnectConfigs/ConnectConfigs';
import { getSelectedTab } from '../../../utils/functions';
import { Link } from 'react-router-dom';
import { withRouter } from '../../../utils/withRouter';

class Connect extends Component {
  state = {
    clusterId: this.props.params.clusterId,
    connectId: this.props.params.connectId,
    definitionId: this.props.params.definitionId,
    selectedTab: 'tasks'
  };

  tabs = ['tasks', 'configs'];

  componentDidMount() {
    const { clusterId, connectId, definitionId } = this.props.params;
    const tabSelected = getSelectedTab(this.props, this.tabs);
    this.setState({ selectedTab: tabSelected ? tabSelected : 'tasks' }, () => {
      this.props.router.navigate(
        `/ui/${clusterId}/connect/${connectId}/definition/${definitionId}/${this.state.selectedTab}`,
        {
          replace: true
        }
      );
    });
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
    const { clusterId, connectId, definitionId, selectedTab } = this.state;
    const { history, match } = this.props;

    switch (selectedTab) {
      case 'tasks':
        return (
          <ConnectTasks
            clusterId={clusterId}
            connectId={connectId}
            definitionId={definitionId}
            history={history}
            match={match}
          />
        );
      case 'configs':
        return (
          <ConnectConfigs
            clusterId={clusterId}
            connectId={connectId}
            definitionId={definitionId}
            history={history}
            match={match}
          />
        );
      default:
        return (
          <ConnectTasks
            clusterId={clusterId}
            connectId={connectId}
            definitionId={definitionId}
            history={history}
            match={match}
          />
        );
    }
  }

  render() {
    const { clusterId, connectId, definitionId } = this.state;

    return (
      <div>
        <Header title={`Connect: ${definitionId}`} history={this.props.history} />
        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            <li className="nav-item">
              <Link
                to={`/ui/${clusterId}/connect/${connectId}/definition/${definitionId}/tasks`}
                className={this.tabClassName('tasks')}
              >
                Tasks
              </Link>
            </li>
            <li className="nav-item">
              <Link
                to={`/ui/${clusterId}/connect/${connectId}/definition/${definitionId}/configs`}
                className={this.tabClassName('configs')}
              >
                Configs
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

Connect.propTypes = {
  params: PropTypes.object,
  router: PropTypes.object,
  history: PropTypes.object,
  location: PropTypes.object,
  match: PropTypes.object
};

export default withRouter(Connect);
