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
    selectedTab: 'data'
  };

  componentDidMount() {
    const { clusterId, topicId } = this.props.match.params;
    this.setState({ clusterId, topicId });
  }

  selectTab = tab => {
    this.setState({ selectedTab: tab });
  };

  tabClassName = tab => {
    const { selectedTab } = this.state;
    return selectedTab === tab ? 'nav-link active' : 'nav-link';
  };

  renderSelectedTab() {
    const { selectedTab, topicId, clusterId } = this.state;
    const { history, match } = this.props;

    switch (selectedTab) {
      case 'data':
        return <TopicData history={history} match={match} />;
      case 'partitions':
        return <TopicPartitions clusterId={clusterId} topic={topicId} history={history} />;
      case 'groups':
        return <TopicGroups history={history} />;
      case 'configs':
        return <TopicConfigs history={history} />;
      case 'acls':
        return <TopicAcls history={history} />;
      case 'logs':
        return <TopicLogs history={history} />;
      default:
        return <TopicData history={history} />;
    }
  }

  render() {
    const { topicId, clusterId } = this.state;
    return (
      <div id="content">
        <Header title={`Topic: ${topicId}`} />
        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            <li className="nav-item">
              <Link
                className={this.tabClassName('data')}
                onClick={() => this.selectTab('data')}
                to="#"
                role="tab"
              >
                Data
              </Link>
            </li>
            <li className="nav-item">
              <Link
                className={this.tabClassName('partitions')}
                onClick={() => this.selectTab('partitions')}
                to="#"
                role="tab"
              >
                Partitions
              </Link>
            </li>
            <li className="nav-item">
              <Link
                className={this.tabClassName('groups')}
                onClick={() => this.selectTab('groups')}
                to="#"
                role="tab"
              >
                Consumer Groups
              </Link>
            </li>
            <li className="nav-item">
              <Link
                className={this.tabClassName('configs')}
                onClick={() => this.selectTab('configs')}
                to="#"
                role="tab"
              >
                Configs
              </Link>
            </li>
            <li className="nav-item">
              <Link
                className={this.tabClassName('acls')}
                onClick={() => this.selectTab('acls')}
                to="#"
                role="tab"
              >
                ACLS
              </Link>
            </li>
            <li className="nav-item">
              <Link
                className={this.tabClassName('logs')}
                onClick={() => this.selectTab('logs')}
                to="#"
                role="tab"
              >
                Logs
              </Link>
            </li>
          </ul>

          <div className="tab-content">
            <div className="tab-pane active" role="tabpanel">
              {this.renderSelectedTab()}
            </div>
          </div>
        </div>

        <aside>
          <Link to="#" className="btn btn-secondary mr-2">
            <i className="fa fa-fw fa-level-down" aria-hidden={true} /> Live Tail
          </Link>
          <Link
            to="#"
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
          </Link>
        </aside>
      </div>
    );
  }
}

export default Topic;
