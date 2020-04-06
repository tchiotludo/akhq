import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import Header from '../../Header';

// Adaptation of topic.ftl

class AclDetails extends Component {
  state = {
    clusterId: '',
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
    const { selectedTab } = this.state;

    switch (selectedTab) {
      case 'groups':
        return <div>Groups</div>;
      case 'topics':
        return <div>Topics</div>;
      default:
        return <div>Topics</div>;
    }
  }

  render() {
    const { history, match } = this.props;
    const { aclUser } = match.params;
    return (
      <div id="content">
        <Header title={`Acl: ${aclUser}`} />
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
