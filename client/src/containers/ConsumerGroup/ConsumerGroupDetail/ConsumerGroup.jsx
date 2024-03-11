import React from 'react';
import PropTypes from 'prop-types';
import Header from '../../Header';
import ConsumerGroupTopics from './ConsumerGroupTopics/ConsumerGroupTopics';
import ConsumerGroupMembers from './ConsumerGroupMembers/ConsumerGroupMembers';
import { Link } from 'react-router-dom';
import ConsumerGroupAcls from './ConsumerGroupAcls/ConsumerGroupAcls';
import { getSelectedTab } from '../../../utils/functions';
import Root from '../../../components/Root';
import { withRouter } from '../../../utils/withRouter';

class ConsumerGroup extends Root {
  state = {
    clusterId: this.props.params.clusterId,
    consumerGroupId: this.props.params.consumerGroupId,
    consumerGroup: {},
    selectedTab: 'topics',
    roles: JSON.parse(sessionStorage.getItem('roles'))
  };

  tabs = ['topics', 'members', 'acls'];

  componentDidMount() {
    const { clusterId, consumerGroupId } = this.props.params;
    const tabSelected = getSelectedTab(this.props, this.tabs);
    this.setState(
      {
        selectedTab: tabSelected ? tabSelected : 'topics'
      },
      () => {
        this.props.router.navigate({
          pathname: `/ui/${clusterId}/group/${consumerGroupId}/${this.state.selectedTab}`
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
    const { clusterId, consumerGroupId } = this.props.params;
    switch (selectedTab) {
      case 'topics':
        return <ConsumerGroupTopics clusterId={clusterId} consumerGroupId={consumerGroupId} />;
      case 'members':
        return <ConsumerGroupMembers clusterId={clusterId} consumerGroupId={consumerGroupId} />;
      case 'acls':
        return <ConsumerGroupAcls clusterId={clusterId} consumerGroupId={consumerGroupId} />;

      default:
        return <ConsumerGroupTopics clusterId={clusterId} consumerGroupId={consumerGroupId} />;
    }
  }

  render() {
    const { clusterId, consumerGroupId } = this.state;
    const roles = this.state.roles || {};
    return (
      <div>
        <Header title={`Consumer Group: ${decodeURIComponent(consumerGroupId)}`} />
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
            {roles.ACL && roles.ACL.includes('READ') && (
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

        {roles.CONSUMER_GROUP &&
          (roles.CONSUMER_GROUP.includes('DELETE_OFFSET') ||
            roles.CONSUMER_GROUP.includes('UPDATE_OFFSET')) && (
            <aside>
              <li className="aside-button">
                {roles.CONSUMER_GROUP.includes('DELETE_OFFSET') && (
                  <Link
                    to={`/ui/${clusterId}/group/${consumerGroupId}/offsetsdelete`}
                    className="btn btn-secondary mr-2"
                  >
                    Delete Offsets
                  </Link>
                )}
                {roles.CONSUMER_GROUP.includes('UPDATE_OFFSET') && (
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
  location: PropTypes.object,
  clusters: PropTypes.array,
  children: PropTypes.any
};

export default withRouter(ConsumerGroup);
