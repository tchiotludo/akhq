import React, { Component } from 'react';
import Header from '../../Header';
import AclGroups from './AclGroups/AclGroups';
import AclTopics from './AclTopics/AclTopics';

class AclDetails extends Component {
  state = {
    clusterId: this.props.match.params.clusterId,
    principalEncoded: this.props.match.params.principalEncoded,
    selectedTab: 'topics'
  };

  componentDidMount() {}

  selectTab = tab => {
    this.setState({ selectedTab: tab });
  };

  tabClassName = tab => {
    const { selectedTab } = this.state;
    return selectedTab === tab ? 'nav-link active' : 'nav-link';
  };

  renderSelectedTab() {
    const { selectedTab, clusterId, principalEncoded } = this.state;
    const { history } = this.props;

    switch (selectedTab) {
      case 'groups':
        return (
          <AclGroups clusterId={clusterId} principalEncoded={principalEncoded} history={history} />
        );
      case 'topics':
        return (
          <AclTopics clusterId={clusterId} principalEncoded={principalEncoded} history={history} />
        );
      default:
        return (
          <AclTopics clusterId={clusterId} principalEncoded={principalEncoded} history={history} />
        );
    }
  }

  render() {
    const { history } = this.props;
    const principal = atob(this.state.principalEncoded);
    return (
      <div>
        <Header title={`Acl: ${principal}`} history={history} />
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
                className={this.tabClassName('groups')}
                onClick={() => this.selectTab('groups')}
                role="tab"
              >
                Groups
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

export default AclDetails;
