import React from 'react';
import Header from '../../Header';
import Dropdown from 'react-bootstrap/Dropdown';
import TopicData from './TopicData';
import TopicPartitions from './TopicPartitions';
import TopicGroups from './TopicGroups';
import TopicConfigs from './TopicConfigs';
import TopicAcls from './TopicAcls';
import TopicLogs from './TopicLogs';
import { uriTopicsConfigs, uriTopicDataEmpty } from '../../../utils/endpoints';
import ConfirmModal from '../../../components/Modal/ConfirmModal';
import { toast } from 'react-toastify';
import { getSelectedTab } from '../../../utils/functions';
import { Link } from 'react-router-dom';
import Root from '../../../components/Root';
import { withRouter } from '../../../utils/withRouter';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEraser, faLevelDown, faPlus } from '@fortawesome/free-solid-svg-icons';

class Topic extends Root {
  state = {
    clusterId: this.props.clusterId,
    topicId: this.props.topicId,
    registryType: '',
    topic: {},
    selectedTab: '',
    showDeleteModal: false,
    deleteMessage: '',
    compactMessageToDelete: '',
    roles: JSON.parse(sessionStorage.getItem('roles')),
    topicInternal: false,
    configs: [],
    exportSome: false,
    exportMessages: [],
    downloadFormat: 'Select',
    downloadOptions: ['csv', 'json'],
    messages: []
  };

  tabs = ['data', 'partitions', 'groups', 'configs', 'acls', 'logs'];

  constructor(props) {
    super(props);
    this.topicData = React.createRef();
    this._handleSelectCheckboxChange = this._handleSelectCheckboxChange.bind(this);
  }

  static getDerivedStateFromProps(props, state) {
    return state;
  }

  componentDidMount() {
    const { clusterId, topicId } = this.props.params;
    const searchParams = this.props.location.search;

    const roles = this.state.roles || {};
    const tabSelected = getSelectedTab(this.props, this.tabs);
    const registryType = this.props.clusters.find(el => el.id === clusterId).registryType;
    this.setState(
      {
        clusterId,
        topicId,
        registryType,
        selectedTab:
          roles.TOPIC_DATA && roles.TOPIC_DATA.includes('READ') ? tabSelected : 'configs',
        topicInternal: this.props.location.internal
      },
      () => {
        this.getTopicsConfig();
        let uri = `/ui/${clusterId}/topic/${topicId}/${this.state.selectedTab}`;
        if (searchParams) {
          uri = uri + searchParams;
        }
        this.props.router.navigate(uri, { replace: true });
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

  _handleSelectCheckboxChange = (exportMessages, selected) => {
    this.setState({ exportMessages, exportSome: selected });
  };

  _renderDownloadFormat = isChecked => {
    const { downloadOptions } = this.state;

    let renderedOptions = [];
    for (let option of downloadOptions) {
      renderedOptions.push(
        <Dropdown.Item
          key={option}
          se
          disabled={isChecked === false}
          onClick={() =>
            this.setState({ downloadFormat: option }, () => {
              this._handleDownloadAll(option);
            })
          }
        >
          {option}
        </Dropdown.Item>
      );
    }
    return renderedOptions;
  };

  _handleDownloadAll(option) {
    let messages = this.state.exportMessages;
    if (messages && messages.length > 0) {
      let allData = [];
      switch (option) {
        case 'json':
          try {
            allData = [
              JSON.stringify(
                messages.map(m => JSON.parse(m.value)),
                null,
                2
              )
            ];
          } catch (e) {
            toast.warn('Unable to export data in JSON. Please use CSV instead');
            return;
          }
          break;
        case 'csv':
          allData = [messages.map(m => m.value).join('\n')];
          break;
      }
      const a = document.createElement('a');
      const type = 'text/' + option;
      a.href = URL.createObjectURL(new Blob(allData, { type: type, endings: 'native' }));
      a.download = `file.${option}`;

      a.click();
      a.remove();
    }
  }

  showDeleteModal = deleteMessage => {
    this.setState({ showDeleteModal: true, deleteMessage });
  };

  closeDeleteModal = () => {
    this.setState({ showDeleteModal: false, deleteMessage: '' });
  };

  canEmptyTopic = () => {
    const { configs } = this.state;
    const res = configs.filter(config => config.name === 'cleanup.policy');
    return res && res.length === 1 && res[0].value.includes('delete');
  };

  emptyTopic = () => {
    const { clusterId, topicId } = this.props.params;

    this.removeApi(uriTopicDataEmpty(clusterId, topicId))
      .then(() => {
        toast.success(`Topic '${topicId}' will be emptied`);
        this.setState({ showDeleteModal: false }, () => {
          this.topicData.current._getMessages(false);
        });
      })
      .catch(() => {
        this.setState({ showDeleteModal: false });
      });
  };

  componentDidUpdate(prevProps) {
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

  tabClassName = tab => {
    const { selectedTab } = this.state;
    return selectedTab === tab ? 'nav-link active' : 'nav-link';
  };

  renderSelectedTab = () => {
    const { selectedTab, topicId, clusterId, roles, topicInternal } = this.state;
    const { location } = this.props;

    switch (selectedTab) {
      case 'data':
        return (
          <TopicData
            ref={this.topicData}
            location={location}
            registryType={this.state.registryType}
            updateExportData={this._handleSelectCheckboxChange}
          />
        );
      case 'partitions':
        return <TopicPartitions clusterId={clusterId} topic={topicId} />;
      case 'groups':
        return <TopicGroups clusterId={clusterId} topicId={topicId} />;
      case 'configs':
        return <TopicConfigs internal={topicInternal} topicId={topicId} clusterId={clusterId} />;
      case 'acls':
        return <TopicAcls topicId={topicId} clusterId={clusterId} />;
      case 'logs':
        return <TopicLogs clusterId={clusterId} topic={topicId} />;
      default:
        return roles.TOPIC_DATA && roles.TOPIC_DATA.includes('READ') ? (
          <TopicData location={location} />
        ) : (
          <TopicPartitions />
        );
    }
  };

  render() {
    const { topicId, clusterId, selectedTab } = this.state;

    const roles = this.state.roles || {};
    return (
      <div>
        <Header title={`Topic: ${topicId}`} />
        <div className="tabs-container" style={{ marginBottom: '4%' }}>
          <ul className="nav nav-tabs" role="tablist">
            {roles.TOPIC_DATA && roles.TOPIC_DATA.includes('READ') && (
              <li className="nav-item">
                <Link
                  to={`/ui/${clusterId}/topic/${topicId}/data`}
                  className={this.tabClassName('data')}
                >
                  Data
                </Link>
              </li>
            )}
            <li className="nav-item">
              <Link
                to={`/ui/${clusterId}/topic/${topicId}/partitions`}
                className={this.tabClassName('partitions')}
              >
                Partitions
              </Link>
            </li>
            <li className="nav-item">
              <Link
                to={`/ui/${clusterId}/topic/${topicId}/groups`}
                className={this.tabClassName('groups')}
              >
                Consumer Groups
              </Link>
            </li>
            <li className="nav-item">
              <Link
                to={`/ui/${clusterId}/topic/${topicId}/configs`}
                className={this.tabClassName('configs')}
              >
                Configs
              </Link>
            </li>
            {roles.ACL && roles.ACL.includes('READ') && (
              <li className="nav-item">
                <Link
                  to={`/ui/${clusterId}/topic/${topicId}/acls`}
                  className={this.tabClassName('acls')}
                >
                  ACLS
                </Link>
              </li>
            )}
            <li className="nav-item">
              <Link
                to={`/ui/${clusterId}/topic/${topicId}/logs`}
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
        {selectedTab !== 'configs' && roles.TOPIC_DATA && roles.TOPIC_DATA.includes('READ') && (
          <aside>
            <li className="aside-button">
              {this.state.exportSome && (
                <div className="btn btn-secondary me-2 p-0">
                  <Dropdown>
                    <Dropdown.Toggle className="btn dropdown-toggle btn-secondary">
                      <strong>Download Format:</strong> ({this.state.downloadFormat})
                    </Dropdown.Toggle>
                    <Dropdown.Menu>
                      <div>{this._renderDownloadFormat(this.state.exportSome)}</div>
                    </Dropdown.Menu>
                  </Dropdown>
                </div>
              )}

              {roles.TOPIC_DATA && roles.TOPIC_DATA.includes('DELETE') ? (
                this.canEmptyTopic() ? (
                  <div
                    onClick={() => {
                      this.handleOnEmpty();
                    }}
                    className="btn btn-secondary me-2"
                  >
                    <FontAwesomeIcon icon={faEraser} aria-hidden={true} /> Empty Topic
                  </div>
                ) : (
                  <div
                    title="Only enabled for topics with Delete Cleanup Policy"
                    className="btn disabled-black-button me-2"
                  >
                    <FontAwesomeIcon icon={faEraser} aria-hidden={true} /> Empty Topic
                  </div>
                )
              ) : (
                <></>
              )}

              {roles.TOPIC_DATA && roles.TOPIC_DATA.includes('CREATE') && (
                <Link
                  to={{
                    pathname: `/ui/${clusterId}/topic/${topicId}/copy`
                  }}
                  className="btn btn-secondary me-2"
                >
                  <FontAwesomeIcon icon={faLevelDown} aria-hidden={true} /> Copy Topic
                </Link>
              )}

              {roles.TOPIC_DATA && roles.TOPIC_DATA.includes('READ') && (
                <Link
                  to={{
                    pathname: `/ui/${clusterId}/tail`,
                    search: `?topicId=${topicId}`
                  }}
                  className="btn btn-secondary me-2"
                >
                  <FontAwesomeIcon icon={faLevelDown} aria-hidden={true} /> Live Tail
                </Link>
              )}

              {selectedTab === 'partitions' &&
                roles.TOPIC_DATA &&
                roles.TOPIC_DATA.includes('CREATE') && (
                  <Link
                    to={`/ui/${clusterId}/topic/${topicId}/increasepartition`}
                    className="btn btn-secondary me-2"
                  >
                    <FontAwesomeIcon icon={faPlus} aria-hidden={true} /> Increase Partition
                  </Link>
                )}

              {roles.TOPIC_DATA && roles.TOPIC_DATA.includes('CREATE') && (
                <Link to={`/ui/${clusterId}/topic/${topicId}/produce`} className="btn btn-primary">
                  <FontAwesomeIcon icon={faPlus} aria-hidden={true} /> Produce to topic
                </Link>
              )}
            </li>
          </aside>
        )}
        <ConfirmModal
          show={this.state.showDeleteModal}
          handleCancel={this.closeDeleteModal}
          handleConfirm={this.emptyTopic}
          message={this.state.deleteMessage}
        />
      </div>
    );
  }
}

export default withRouter(Topic);
