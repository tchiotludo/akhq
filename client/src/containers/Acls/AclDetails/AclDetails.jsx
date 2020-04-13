import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import Header from '../../Header';
import AclGroups from './AclGroups/AclGroups';
import AclTopics from './AclTopics/AclTopics';

// Adaptation of topic.ftl

class AclDetails extends Component {
  state = {
    clusterId: this.props.match.params.clusterId,
    principal: this.props.history.principal,
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
    const { history, match } = this.props;
    const { principal } = this.props.history || '';
    return (
      <div id="content">
        <Header title={`Acl: ${principal}`} />
        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            <li className="nav-item">
              <Link
                className={this.tabClassName('topics')}
                onClick={() => this.selectTab('topics')}
                to="#"
                role="tab"
              >
                Topics
              </Link>
            </li>
            <li className="nav-item">
              <Link
                className={this.tabClassName('groups')}
                onClick={() => this.selectTab('groups')}
                to="#"
                role="tab"
              >
                Groups
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

export default AclDetails;
