import React, { Component } from 'react';
import Header from '../../Header';
import ConsumerGroupTopics from './ConsumerGroupTopics/ConsumerGroupTopics';
import ConsumerGroupMembers from './ConsumerGroupMembers/ConsumerGroupMembers';
import { Link } from 'react-router-dom';
import ConsumerGroupAcls from './ConsumerGroupAcls/ConsumerGroupAcls';

class ConsumerGroup extends Component {
  state = {
    clusterId: '',
    consumerGroupId: '',
    consumerGroup: {},
    selectedTab: 'topics',
    roles: JSON.parse(sessionStorage.getItem('roles'))
  };

  componentDidMount() {
    const { clusterId, consumerGroupId } = this.props.match.params;

    this.setState({ clusterId, consumerGroupId });
  }

  selectTab = tab => {
    this.setState({ selectedTab: tab });
  };

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
        <Header title={`Consumer Group: ${consumerGroupId}`} history={this.props.history} />
        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            <li className="nav-item">
              <div
                className={this.tabClassName('topics')}
                onClick={() => this.selectTab('topics')}
                role="tab"
              >
                Topics
              </div>
            </li>
            <li className="nav-item">
              <div
                className={this.tabClassName('members')}
                onClick={() => this.selectTab('members')}
                role="tab"
              >
                Members
              </div>
            </li>
            {roles.acls && roles.acls['acls/read'] && (
              <li className="nav-item">
                <div
                  className={this.tabClassName('acls')}
                  onClick={() => this.selectTab('acls')}
                  role="tab"
                >
                  ACLS
                </div>
              </li>
            )}
          </ul>

          <div className="tab-content">
            <div className="tab-pane active" role="tabpanel">
              {this.renderSelectedTab()}
            </div>
          </div>
        </div>

        {roles.group && roles.group['group/offsets/update'] && (
          <aside>
            <Link
              to={`/ui/${clusterId}/group/${consumerGroupId}/offsets`}
              className="btn btn-primary"
            >
              Update Offsets
            </Link>
          </aside>
        )}
      </div>
    );
  }
}

export default ConsumerGroup;
