import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import Header from '../../Header';
import TopicData from './TopicData';
import TopicPartitions from './TopicPartitions';
import TopicGroups from './TopicGroups';
import TopicConfigs from './TopicConfigs';
import TopicAcls from './TopicAcls';
import TopicLogs from './TopicLogs';
import TopicProduce from './TopicProduce';

// Adaptation of topic.ftl

class Topic extends Component {
  state = {
    clusterId: '',
    topicId: '',
    topic: {},
    selectedTab: '',
    roles: JSON.parse(localStorage.getItem('roles')),
    topicInternal: false
  };

  static getDerivedStateFromProps(props, state) {
    return state;
  }

  componentDidMount() {
    const { clusterId, topicId } = this.props.match.params;
    const { roles } = this.state;
    this.setState({
      clusterId,
      topicId,
      selectedTab: roles.topic && roles.topic['topic/data/read'] ? 'data' : 'partitions',
      topicInternal: this.props.location.internal
    });
  }

  selectTab = tab => {
    this.setState({ selectedTab: tab });
  };

  tabClassName = tab => {
    const { selectedTab } = this.state;
    return selectedTab === tab ? 'nav-link active' : 'nav-link';
  };

  renderSelectedTab() {
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
  }

  render() {
    const { topicId, clusterId, selectedTab, roles } = this.state;
    return (
      <div>
        <Header title={`Topic: ${topicId}`} history={this.props.history} />
        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            {roles.topic && roles.topic['topic/data/read'] && (
              <li className="nav-item">
                <a
                  className={this.tabClassName('data')}
                  onClick={() => this.selectTab('data')}
                  //to="#"
                  role="tab"
                >
                  Data
                </a>
              </li>
            )}
            <li className="nav-item">
              <a
                className={this.tabClassName('partitions')}
                onClick={() => this.selectTab('partitions')}
                //to="#"
                role="tab"
              >
                Partitions
              </a>
            </li>
            <li className="nav-item">
              <a
                className={this.tabClassName('groups')}
                onClick={() => this.selectTab('groups')}
                //to="#"
                role="tab"
              >
                Consumer Groups
              </a>
            </li>
            <li className="nav-item">
              <a
                className={this.tabClassName('configs')}
                onClick={() => this.selectTab('configs')}
                //to="#"
                role="tab"
              >
                Configs
              </a>
            </li>
            {roles.acls && roles.acls['acls/read'] && (
              <li className="nav-item">
                <a
                  className={this.tabClassName('acls')}
                  onClick={() => this.selectTab('acls')}
                  //to="#"
                  role="tab"
                >
                  ACLS
                </a>
              </li>
            )}
            <li className="nav-item">
              <a
                className={this.tabClassName('logs')}
                onClick={() => this.selectTab('logs')}
                //to="#"
                role="tab"
              >
                Logs
              </a>
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
            <a className="btn btn-secondary mr-2">
              <i className="fa fa-fw fa-level-down" aria-hidden={true} /> Live Tail
            </a>

            <a
              className="btn btn-primary"
              to={{
                pathname: `/${clusterId}/topic/${topicId}/produce`
              }}
            >
              <i
                className="fa fa-plus"
                aria-hidden={true}
                onClick={() => <TopicProduce clusterId={clusterId} topic={topicId} />}
              />{' '}
              Produce to topic
            </a>
          </aside>
        )}
      </div>
    );
  }
}

export default Topic;
