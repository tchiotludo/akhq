import React, { Component } from 'react';
import PropTypes from 'prop-types';
import Header from '../../Header';
import ConsumerGroupTopics from './ConsumerGroupTopics/ConsumerGroupTopics';
import ConsumerGroupMembers from './ConsumerGroupMembers/ConsumerGroupMembers';
import { Link } from 'react-router-dom';
import ConsumerGroupAcls from './ConsumerGroupAcls/ConsumerGroupAcls';
import { getSelectedTab } from '../../../utils/functions';

class ConsumerGroup extends Component {
  state = {
    clusterId: this.props.match.params.clusterId,
    consumerGroupId: this.props.match.params.consumerGroupId,
    consumerGroup: {},
    selectedTab: 'topics',
    roles: JSON.parse(sessionStorage.getItem('roles'))
  };

  tabs = ['topics', 'members', 'acls'];

  componentDidMount() {
    const { clusterId, consumerGroupId } = this.props.match.params;
    const tabSelected = getSelectedTab(this.props, this.tabs);
    this.setState(
      {
        selectedTab: tabSelected ? tabSelected : 'topics'
      },
      () => {
        this.props.history.replace(
          `/ui/${clusterId}/group/${consumerGroupId}/${this.state.selectedTab}`
        );
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
    const { history } = this.props;
    const { clusterId, consumerGroupId } = this.props.match.params;
    switch (selectedTab) {
      case 'topics':
        return (
          <ConsumerGroupTopics
            clusterId={clusterId}
            consumerGroupId={consumerGroupId}
            history={history}
          />
        );
      case 'members':
        return (
          <ConsumerGroupMembers
            clusterId={clusterId}
            consumerGroupId={consumerGroupId}
            history={history}
          />
        );
      case 'acls':
        return (
          <ConsumerGroupAcls
            clusterId={clusterId}
            consumerGroupId={consumerGroupId}
            history={history}
          />
        );

      default:
        return (
          <ConsumerGroupTopics
            clusterId={clusterId}
            consumerGroupId={consumerGroupId}
            history={history}
          />
        );
    }
  }

  render() {
    const { clusterId, consumerGroupId } = this.state;
    const roles = this.state.roles || {};
    return (
      <div>
        <Header
          title={`Consumer Group: ${decodeURIComponent(consumerGroupId)}`}
          history={this.props.history}
        />
        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            <li className="nav-item">
              <Link
                to={`/ui/${clusterId}/group/${consumerGroupId}/topics`}
                className={this.tabClassName('topics')}
              >
                Topics
              </Link>
            </li>
            <li className="nav-item">
              <Link
                to={`/ui/${clusterId}/group/${consumerGroupId}/members`}
                className={this.tabClassName('members')}
              >
                Members
              </Link>
            </li>
            {roles.acls && roles.acls['acls/read'] && (
              <li className="nav-item">
                <Link
                  to={`/ui/${clusterId}/group/${consumerGroupId}/acls`}
                  className={this.tabClassName('acls')}
                >
                  ACLS
                </Link>
              </li>
            )}
          </ul>

          <div className="tab-content">
            <div className="tab-pane active" role="tabpanel">
              {this.renderSelectedTab()}
            </div>
          </div>
        </div>

        {roles.group &&
          (roles.group['group/offsets/delete'] || roles.group['group/offsets/update']) && (
            <aside>
              <li className="aside-button">
                {roles.group['group/offsets/delete'] && (
                  <Link
                    to={`/ui/${clusterId}/group/${consumerGroupId}/offsetsdelete`}
                    className="btn btn-secondary mr-2"
                  >
                    Delete Offsets
                  </Link>
                )}
                {roles.group['group/offsets/update'] && (
                  <Link
                    to={`/ui/${clusterId}/group/${consumerGroupId}/offsets`}
                    className="btn btn-primary"
                  >
                    Update Offsets
                  </Link>
                )}
              </li>
            </aside>
          )}
      </div>
    );
  }
}

ConsumerGroup.propTypes = {
  history: PropTypes.object,
  match: PropTypes.object,
  location: PropTypes.object,
  clusters: PropTypes.array,
  children: PropTypes.any
};

export default ConsumerGroup;
