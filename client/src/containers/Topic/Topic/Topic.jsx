import React from 'react';
import Header from '../../Header';
import TopicData from './TopicData';
import TopicPartitions from './TopicPartitions';
import TopicGroups from './TopicGroups';
import TopicConfigs from './TopicConfigs';
import TopicAcls from './TopicAcls';
import TopicLogs from './TopicLogs';
import { uriTopicsConfigs, uriTopicDataEmpty } from '../../../utils/endpoints';
import ConfirmModal from '../../../components/Modal/ConfirmModal';
import { toast } from 'react-toastify';
import {getSelectedTab} from "../../../utils/functions";
import { Link } from 'react-router-dom';
import Root from "../../../components/Root";

class Topic extends Root {
  state = {
    clusterId: this.props.clusterId,
    topicId: this.props.topicId,
    topic: {},
    selectedTab: '',
    showDeleteModal: false,
    deleteMessage: '',
    compactMessageToDelete: '',
    roles: JSON.parse(sessionStorage.getItem('roles')),
    topicInternal: false,
    configs: []
  };

  tabs = ['data','partitions','groups','configs','acls','logs'];

  constructor(props) {
    super(props);
    this.topicData = React.createRef();
  }

  static getDerivedStateFromProps(props, state) {
    return state;
  }

  componentDidMount() {
    const { clusterId, topicId } = this.props.match.params;
    const searchParams = this.props.location.search;

    const roles = this.state.roles || {};
    const tabSelected = getSelectedTab(this.props, this.tabs);

    this.setState(
      {
        clusterId,
        topicId,
        selectedTab: roles.topic && roles.topic['topic/data/read'] ? tabSelected : 'configs',
        topicInternal: this.props.location.internal
      },
      () => {
        this.getTopicsConfig();
        let uri = `/ui/${clusterId}/topic/${topicId}/${this.state.selectedTab}`;
        if(searchParams) {
          uri = uri + searchParams;
        }
        this.props.history.replace(uri);
      }
    );
  }

  handleOnEmpty() {
    this.setState(() => {
      this.showDeleteModal(
          <React.Fragment>
            Do you want to empty the Topic: {<code>{this.state.topicId}</code>} ?
          </React.Fragment>
      );
    });
  }

  showDeleteModal = deleteMessage => {
    this.setState({ showDeleteModal: true, deleteMessage });
  };

  closeDeleteModal = () => {
    this.setState({ showDeleteModal: false, deleteMessage: '' });
  };

  canEmptyTopic = () => {
    const { configs } = this.state;
    const res = configs.filter( config => config.name === 'cleanup.policy');
    if(res && res.length === 1 && res[0].value === 'delete') return true;
    return false;
  }

  emptyTopic = () => {
    const { clusterId, topicId } = this.props.match.params;

    this.removeApi(
        uriTopicDataEmpty(clusterId, topicId)
    )
        .then(() => {
          toast.success(`Topic '${topicId}' will be emptied`);
          this.setState({ showDeleteModal: false }, () => {
            this.topicData.current.getMessages();
          });
        })
        .catch(() => {
          this.setState({ showDeleteModal: false });
        });
  };

  componentDidUpdate(prevProps, prevState) {
    if (this.props.location.pathname !== prevProps.location.pathname) {
      const tabSelected = getSelectedTab(this.props, this.tabs);
      this.setState({ selectedTab: tabSelected });
    }
  }

  async getTopicsConfig() {
    const { clusterId, topicId } = this.state;
    let configs = [];
    try {
      configs = await this.getApi(uriTopicsConfigs(clusterId, topicId));
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
    const { history, match, location } = this.props;

    switch (selectedTab) {
      case 'data':
        return <TopicData ref={this.topicData} history={history} match={match} location={location} />;
      case 'partitions':
        return <TopicPartitions clusterId={clusterId} topic={topicId} history={history} />;
      case 'groups':
        return (
          <TopicGroups
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
          <TopicData history={history} match={match} location={location}/>
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
                <Link to={`/ui/${clusterId}/topic/${topicId}/data`}
                  className={this.tabClassName('data')}
                >
                  Data
                </Link>
              </li>
            )}
            <li className="nav-item">
              <Link to={`/ui/${clusterId}/topic/${topicId}/partitions`}
                className={this.tabClassName('partitions')}
              >
                Partitions
              </Link>
            </li>
            <li className="nav-item">
              <Link to={`/ui/${clusterId}/topic/${topicId}/groups`}
                className={this.tabClassName('groups')}
              >
                Consumer Groups
              </Link>
            </li>
            <li className="nav-item">
              <Link to={`/ui/${clusterId}/topic/${topicId}/configs`}
                className={this.tabClassName('configs')}
              >
                Configs
              </Link>
            </li>
            {roles.acls && roles.acls['acls/read'] && (
              <li className="nav-item">
                <Link to={`/ui/${clusterId}/topic/${topicId}/acls`}
                  className={this.tabClassName('acls')}
                >
                  ACLS
                </Link>
              </li>
            )}
            <li className="nav-item">
              <Link to={`/ui/${clusterId}/topic/${topicId}/logs`}
                className={this.tabClassName('logs')}
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
        {selectedTab !== 'configs' && roles.topic && roles.topic['topic/data/insert'] && (
          <aside>
            <li className="aside-button">
              { this.canEmptyTopic()?
                  <div
                      onClick={() => {
                        this.handleOnEmpty();
                      }}
                      className="btn btn-secondary mr-2">
                    <i className="fa fa-fw fa-eraser" aria-hidden={true} /> Empty Topic
                  </div>
                  :
                  <div title="Only enabled for topics with Delete Cleanup Policy"
                       className="btn disabled-black-button mr-2">
                    <i className="fa fa-fw fa-eraser" aria-hidden={true} /> Empty Topic
                  </div>
              }

              <Link to={{
                pathname: `/ui/${clusterId}/topic/${topicId}/copy`
              }}
                    className="btn btn-secondary mr-2"
              >
                <i className="fa fa-fw fa-level-down" aria-hidden={true} /> Copy Topic
              </Link>

              <Link to={{  pathname: `/ui/${clusterId}/tail`,
                search: `?topicId=${topicId}` }} className="btn btn-secondary mr-2">

                <i className="fa fa-fw fa-level-down" aria-hidden={true} /> Live Tail
              </Link>

              <Link to={ `/ui/${clusterId}/topic/${topicId}/produce`}
                className="btn btn-primary">
                <i className="fa fa-plus" aria-hidden={true} /> Produce to topic
              </Link>
            </li>
          </aside>
        )}
        <ConfirmModal show={this.state.showDeleteModal}
                      handleCancel={this.closeDeleteModal}
                      handleConfirm={this.emptyTopic}
                      message={this.state.deleteMessage}
        />
      </div>
    );
  }
}

export default Topic;
