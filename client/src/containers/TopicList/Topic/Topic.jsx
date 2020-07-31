import React, { Component } from 'react';
import Header from '../../Header';
import TopicData from './TopicData';
import TopicPartitions from './TopicPartitions';
import TopicGroups from './TopicGroups';
import TopicConfigs from './TopicConfigs';
import TopicAcls from './TopicAcls';
import TopicLogs from './TopicLogs';
import { get } from '../../../utils/api';
import { uriTopicsConfigs } from '../../../utils/endpoints';

class Topic extends Component {
  state = {
    clusterId: '',
    topicId: '',
    topic: {},
    selectedTab: '',
    roles: JSON.parse(sessionStorage.getItem('roles')),
    topicInternal: false,
    selectedCluster: this.props.clusterId,
    selectedTopic: this.props.topicId,
    configs: {}
  };

  static getDerivedStateFromProps(props, state) {
    return state;
  }

  componentDidMount() {
    const { clusterId, topicId } = this.props.match.params;
    const roles = this.state.roles || {};
    const url = this.props.location.pathname.split('/');
    const tabSelected = this.props.location.pathname.split('/')[url.length - 1];
    this.setState(
      {
        clusterId,
        topicId,
        selectedTab: roles.topic && roles.topic['topic/data/read'] ? tabSelected : 'partitions',
        topicInternal: this.props.location.internal
      },
      () => {
        this.getTopicsConfig();
      }
    );
  }

  async getTopicsConfig() {
    const { clusterId, topicId } = this.state;
    let configs = [];
    try {
      configs = await get(uriTopicsConfigs(clusterId, topicId));
      this.setState({ configs: configs.data });
    } catch (err) {
      console.error('Error:', err);
    }
  }

  selectTab = tab => {
    this.setState({ selectedTab: tab });
  };

  tabClassName = tab => {
    const { selectedTab } = this.state;
    return selectedTab === tab ? 'nav-link active' : 'nav-link';
  };

  renderSelectedTab = () => {
    const { selectedTab, topicId, clusterId, roles, topicInternal } = this.state;
    const { history, match } = this.props;

    switch (selectedTab) {
      case 'data':
        return <TopicData history={history} match={match} />;
      case 'partitions':
        return <TopicPartitions clusterId={clusterId} topic={topicId} history={history} />;
      case 'groups':
        return (
          <TopicGroups
            changeTab={tab => {
              this.selectTab(tab);
            }}
            clusterId={clusterId}
            topicId={topicId}
            history={history}
          />
        );
      case 'configs':
        return (
          <TopicConfigs
            internal={topicInternal}
            history={history}
            topicId={topicId}
            clusterId={clusterId}
          />
        );
      case 'acls':
        return <TopicAcls history={history} topicId={topicId} clusterId={clusterId} />;
      case 'logs':
        return <TopicLogs clusterId={clusterId} topic={topicId} history={history} />;
      default:
        return roles.topic && roles.topic['topic/data/read'] ? (
          <TopicData history={history} match={match} />
        ) : (
          <TopicPartitions history={history} />
        );
    }
  };

  render() {
    const { topicId, clusterId, selectedTab } = this.state;

    const roles = this.state.roles || {};
    return (
      <div>
        <Header title={`Topic: ${topicId}`} history={this.props.history} />
        <div className="tabs-container" style={{ marginBottom: '4%' }}>
          <ul className="nav nav-tabs" role="tablist">
            {roles.topic && roles.topic['topic/data/read'] && (
              <li className="nav-item">
                <div
                  className={this.tabClassName('data')}
                  onClick={() => this.selectTab('data')}
                  //to="#"
                  role="tab"
                >
                  Data
                </div>
              </li>
            )}
            <li className="nav-item">
              <div
                className={this.tabClassName('partitions')}
                onClick={() => this.selectTab('partitions')}
                //to="#"
                role="tab"
              >
                Partitions
              </div>
            </li>
            <li className="nav-item">
              <div
                className={this.tabClassName('groups')}
                onClick={() => this.selectTab('groups')}
                //to="#"
                role="tab"
              >
                Consumer Groups
              </div>
            </li>
            <li className="nav-item">
              <div
                className={this.tabClassName('configs')}
                onClick={() => this.selectTab('configs')}
                //to="#"
                role="tab"
              >
                Configs
              </div>
            </li>
            {roles.acls && roles.acls['acls/read'] && (
              <li className="nav-item">
                <div
                  className={this.tabClassName('acls')}
                  onClick={() => this.selectTab('acls')}
                  //to="#"
                  role="tab"
                >
                  ACLS
                </div>
              </li>
            )}
            <li className="nav-item">
              <div
                className={this.tabClassName('logs')}
                onClick={() => this.selectTab('logs')}
                //to="#"
                role="tab"
              >
                Logs
              </div>
            </li>
          </ul>

          <div className="tab-content">
            <div className="tab-pane active" role="tabpanel">
              {this.renderSelectedTab()}
            </div>
          </div>
        </div>
        {selectedTab !== 'configs' && roles.topic && roles.topic['topic/data/insert'] && (
          <aside>
            <li className="aside-button">
              <div
                onClick={() => {
                  this.props.history.push({ pathname: `/ui/${clusterId}/tail`, topicId: topicId });
                }}
                className="btn btn-secondary mr-2"
              >
                <i className="fa fa-fw fa-level-down" aria-hidden={true} /> Live Tail
              </div>
            </li>

            <div
              onClick={() => {
                this.props.history.push({ pathname: `/ui/${clusterId}/topic/${topicId}/produce` });
              }}
              className="btn btn-primary">
              <i className="fa fa-plus" aria-hidden={true} /> Produce to topic
            </div>
          </aside>
        )}
      </div>
    );
  }
}

export default Topic;
