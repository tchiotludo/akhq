import React, { Component } from 'react';
import Header from '../../Header';
import AclGroups from './AclGroups/AclGroups';
import AclTopics from './AclTopics/AclTopics';
import AclClusters from './AclClusters/AclClusters';
import AclTransactionalIds from './AclTransactionalIds/AclTransactionalIds';
import {getSelectedTab} from "../../../utils/functions";
import { Link } from 'react-router-dom';

class AclDetails extends Component {
  state = {
    clusterId: this.props.match.params.clusterId,
    principalEncoded: this.props.match.params.principalEncoded,
    selectedTab: 'topics'
  };

  tabs = ['topics', 'groups', 'clusters', 'transactionalids'];

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
      case 'clusters':
        return (
          <AclClusters clusterId={clusterId} principalEncoded={principalEncoded} history={history} />
        );
      case 'transactionalids':
        return (
          <AclTransactionalIds clusterId={clusterId} principalEncoded={principalEncoded} history={history} />
        );
      default:
        return (
          <AclTopics clusterId={clusterId} principalEncoded={principalEncoded} history={history} />
        );
    }
  }

  render() {
    const { principalEncoded, clusterId } = this.state;
    const { history } = this.props;
    const principal = atob(principalEncoded);
    return (
      <div>
        <Header title={`Acl: ${principal}`} history={history} />
        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            <li className="nav-item">
              <Link to={`/ui/${clusterId}/acls/${principalEncoded}/topics`}
                className={this.tabClassName('topics')}
              >
                Topics
              </Link>
            </li>
            <li className="nav-item">
              <Link to={`/ui/${clusterId}/acls/${principalEncoded}/groups`}
                className={this.tabClassName('groups')}
              >
                Groups
              </Link>
            </li>
            <li className="nav-item">
              <Link to={`/ui/${clusterId}/acls/${principalEncoded}/clusters`}
                className={this.tabClassName('clusters')}
              >
                Clusters
              </Link>
            </li>
            <li className="nav-item">
              <Link to={`/ui/${clusterId}/acls/${principalEncoded}/transactionalids`}
                className={this.tabClassName('transactionalids')}
              >
                Transactional Ids
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
