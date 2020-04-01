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
    deleteMessage: ''
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
    history.push({
      loading: true
    });
    try {
      connectDefinitions = await get(uriConnectDefinitions(clusterId, connectId));
      this.handleData(connectDefinitions.data);
      this.setState({ selectedCluster: clusterId });
    } catch (err) {
      history.replace('/error', { errorData: err });
    } finally {
      history.push({
        loading: false
      });
    }
  }

  deleteDefinition = () => {
    const { clusterId, connectId, definitionToDelete: definition } = this.state;
    const { history } = this.props;
    const deleteData = {
      clusterId,
      connectId,
      definitionId: definition
    };
    history.push({ loading: true });
    remove(uriDeleteDefinition(), deleteData)
      .then(res => {
        this.props.history.push({
          showSuccessToast: true,
          successToastMessage: `Definition '${definition}' is deleted`,
          loading: false
        });
        this.setState(
          { showDeleteModal: false, definitionToDelete: '' },
          this.handleData(res.data)
        );
      })
      .catch(err => {
        this.props.history.push({
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
        config: connectDefinition.config || '',
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

  handleOnDelete(definition) {
    this.setState({ definitionToDelete: definition }, () => {
      this.showDeleteModal(`Do you want to delete definition: ${definition}?`);
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
            {`${task.name} (${task.id}) `}
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
    const { history } = this.props;

    return (
      <div id="content">
        <Header title={`Connect: ${connectId}`} />
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
                        onClick={() => this.showConfigModal(obj[col.accessor])}
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
          actions={[constants.TABLE_DETAILS, constants.TABLE_DELETE]}
          onDetails={name => {
            history.push({
              pathname: `/${clusterId}/connect/${connectId}/definition/${name}`,
              clusterId,
              connectId,
              definitionId: name
            });
          }}
          onDelete={row => {
            this.handleOnDelete(row.id);
          }}
        />
        <aside>
          <Link to={`/${clusterId}/connect/${connectId}/create`} class="btn btn-primary">
            Create a definition
          </Link>
        </aside>
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
