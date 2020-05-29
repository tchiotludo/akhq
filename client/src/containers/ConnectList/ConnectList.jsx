import React, { Component } from 'react';
import './styles.scss';

import Header from '../Header';
import Table from '../../components/Table/Table';
import constants from '../../utils/constants';
import { Link } from 'react-router-dom';
import { get, remove } from '../../utils/api';
import { uriConnectDefinitions, uriDeleteDefinition } from '../../utils/endpoints';
import CodeViewModal from '../../components/Modal/CodeViewModal/CodeViewModal';
import ConfirmModal from '../../components/Modal/ConfirmModal/ConfirmModal';

class ConnectList extends Component {
  state = {
    clusterId: '',
    connectId: '',
    tableData: [],
    showConfigModal: false,
    configModalBody: '',
    showDeleteModal: false,
    definitionToDelete: '',
    deleteMessage: '',
    roles: JSON.parse(localStorage.getItem('roles'))
  };

  static getDerivedStateFromProps(nextProps, prevState) {
    const clusterId = nextProps.match.params.clusterId;
    const connectId = nextProps.match.params.connectId;

    return {
      clusterId: clusterId,
      connectId: connectId
    };
  }

  componentDidMount() {
    this.getConnectDefinitions();
  }

  async getConnectDefinitions() {
    let connectDefinitions = [];
    const { clusterId, connectId } = this.state;
    const { history } = this.props;
    history.replace({
      ...this.props.location,
      loading: true
    });
    try {
      connectDefinitions = await get(uriConnectDefinitions(clusterId, connectId));
      this.handleData(connectDefinitions.data);
      this.setState({ selectedCluster: clusterId });
      history.replace({
        ...this.props.location,
        loading: false
      });
    } catch (err) {
      if (err.response && err.response.status === 404) {
        history.replace('/page-not-found', { errorData: err, loading: false });
      } else {
        history.replace('/error', { errorData: err, loading: false });
      }
    }
  }

  deleteDefinition = () => {
    const { clusterId, connectId, definitionToDelete: definition } = this.state;
    const { history } = this.props;
    history.replace({ loading: true });
    remove(uriDeleteDefinition(clusterId, connectId, definition))
      .then(res => {
        this.props.history.replace({
          ...this.props.location,
          showSuccessToast: true,
          successToastMessage: `Definition '${definition}' is deleted`,
          loading: false
        });
        this.setState({ showDeleteModal: false, definitionToDelete: '' }, () => {
          this.getConnectDefinitions();
        });
      })
      .catch(err => {
        this.props.history.replace({
          ...this.props.location,
          showErrorToast: true,
          errorToastMessage: `Failed to delete definition from '${definition}'`,
          loading: false
        });
        this.setState({ showDeleteModal: false, topicToDelete: {} });
      });
  };

  handleData = data => {
    let tableData = [];
    tableData = data.map(connectDefinition => {
      return {
        id: connectDefinition.name || '',
        config: JSON.stringify(connectDefinition.configs) || '',
        type:
          {
            type: connectDefinition.type,
            shortClassName: connectDefinition.shortClassName
          } || '',
        tasks: connectDefinition.tasks || ''
      };
    });

    this.setState({ tableData });
  };

  showConfigModal = body => {
    this.setState({
      showConfigModal: true,
      configModalBody: body
    });
  };

  closeConfigModal = () => {
    this.setState({ showConfigModal: false, configModalBody: '' });
  };

  showDeleteModal = deleteMessage => {
    this.setState({ showDeleteModal: true, deleteMessage });
  };

  closeDeleteModal = () => {
    this.setState({ showDeleteModal: false, deleteMessage: '' });
  };

  getTableActions = () => {
    const roles = this.state.roles || {};
    let actions = [];

    if (roles.connect && roles.connect['connect/update']) {
      actions.push(constants.TABLE_DETAILS);
    }
    if (roles.connect && roles.connect['connect/delete']) {
      actions.push(constants.TABLE_DELETE);
    }

    return actions;
  };

  handleOnDelete(definition) {
    this.setState({ definitionToDelete: definition }, () => {
      this.showDeleteModal(
        <React.Fragment>
          Do you want to delete definition: {<code>{definition}</code>} ?
        </React.Fragment>
      );
    });
  }

  renderTasks = tasks => {
    let renderedTasks = [];

    for (let task of tasks) {
      let className = 'btn btn-sm mb-1 btn-';
      switch (task.state) {
        case 'RUNNING':
          className += 'success';
          break;
        case 'FAILED':
          className += 'danger';
          break;
        default:
          className += 'warning';
          break;
      }

      renderedTasks.push(
        <React.Fragment>
          <span class={`btn btn-sm mb-1 ${className}`}>
            {`${task.workerId} (${task.id}) `}
            <span class="badge badge-light">{task.state}</span>
          </span>
          <br />
        </React.Fragment>
      );
    }

    return renderedTasks;
  };

  render() {
    const { clusterId, connectId, tableData, showConfigModal, configModalBody } = this.state;
    const roles = this.state.roles || {};
    const { history } = this.props;

    return (
      <div>
        <Header title={`Connect: ${connectId}`} history={history} />
        <Table
          columns={[
            {
              id: 'id',
              name: 'id',
              accessor: 'id',
              colName: 'Name',
              type: 'text'
            },
            {
              id: 'config',
              name: 'config',
              accessor: 'config',
              colName: 'Config',
              type: 'text',
              cell: (obj, col) => {
                return (
                  <div className="value cell-div" style={{ maxHeight: '100%' }}>
                    <span className="align-cell value-span">
                      {obj[col.accessor] ? obj[col.accessor].substring(0, 100) : 'N/A'}
                      {obj[col.accessor] && obj[col.accessor].length > 100 && '(...)'}{' '}
                    </span>
                    <div className="value-button">
                      <button
                        className="btn btn-secondary headers pull-right"
                        onClick={() =>
                          this.showConfigModal(
                            JSON.stringify(JSON.parse(obj[col.accessor]), null, 2)
                          )
                        }
                      >
                        ...
                      </button>
                    </div>
                  </div>
                );
              }
            },
            {
              id: 'type',
              accessor: 'type',
              colName: 'Type',
              type: 'text',
              cell: (obj, col) => {
                if (obj[col.accessor].type === 'source') {
                  return (
                    <React.Fragment>
                      <i class="fa fa-forward" aria-hidden="true"></i>
                      {` ${obj[col.accessor].shortClassName}`}
                    </React.Fragment>
                  );
                }

                return (
                  <React.Fragment>
                    <i class="fa fa-backward" aria-hidden="true"></i>
                    {` ${obj[col.accessor].shortClassName}`}
                  </React.Fragment>
                );
              }
            },
            {
              id: 'tasks',
              accessor: 'tasks',
              colName: 'Tasks',
              type: 'text',
              cell: (obj, col) => {
                return this.renderTasks(obj[col.accessor]);
              }
            }
          ]}
          data={tableData}
          actions={this.getTableActions()}
          onDetails={name => {
            history.push({
              pathname: `/ui/${clusterId}/connect/${connectId}/definition/${name}`,
              clusterId,
              connectId,
              definitionId: name
            });
          }}
          onDelete={row => {
            this.handleOnDelete(row.id);
          }}
          noContent={'No connectors available'}
        />
        {roles.connect && roles.connect['connect/insert'] && (
          <aside>
            <Link to={`/ui/${clusterId}/connect/${connectId}/create`} className="btn btn-primary">
              Create a definition
            </Link>
          </aside>
        )}
        <ConfirmModal
          show={this.state.showDeleteModal}
          handleCancel={this.closeDeleteModal}
          handleConfirm={this.deleteDefinition}
          message={this.state.deleteMessage}
        />
        <CodeViewModal
          show={showConfigModal}
          body={configModalBody}
          handleClose={this.closeConfigModal}
        />
      </div>
    );
  }
}

export default ConnectList;
