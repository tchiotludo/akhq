import React, { Component } from 'react';
import Header from '../../Header';
import AclGroups from './AclGroups/AclGroups';
import AclTopics from './AclTopics/AclTopics';
import {getSelectedTab} from "../../../utils/functions";

class AclDetails extends Component {
  state = {
    clusterId: this.props.match.params.clusterId,
    principalEncoded: this.props.match.params.principalEncoded,
    selectedTab: 'topics'
  };

  tabs = ['topics', 'groups'];

  componentDidMount() {
    const { clusterId, principalEncoded } = this.props.match.params;
    const tabSelected = getSelectedTab(this.props, this.tabs);
    this.setState(
        {
          selectedTab: (tabSelected)? tabSelected : 'topics'
        },
        () => {
          this.props.history.replace(`/ui/${clusterId}/acls/${principalEncoded}/${this.state.selectedTab}`);
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
    const { principalEncoded, clusterId } = this.state;
    this.setState({ selectedTab: tab });
    this.props.history.push(`/ui/${clusterId}/acls/${principalEncoded}/${tab}`);
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
