import React from 'react';
import './styles.scss';
import Table from '../../../../components/Table/Table';
import constants from '../../../../utils/constants';
import {
  uriGetDefinition,
  uriPauseDefinition,
  uriResumeDefinition,
  uriRestartDefinition,
  uriRestartTask
} from '../../../../utils/endpoints';
import ConfirmModal from '../../../../components/Modal/ConfirmModal/ConfirmModal';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import AceEditor from 'react-ace';
import Root from '../../../../components/Root';

class ConnectTasks extends Root {
  state = {
    clusterId: this.props.clusterId || this.props.match.params.clusterId,
    connectId: this.props.connectId || this.props.match.params.connectId,
    definitionId: this.props.definitionId || this.props.match.params.clusterId,
    definition: {},
    definitionModifyMessage: '',
    definitionModifyData: {},
    tableData: [],
    showActionModal: false,
    actionMessage: '',
    roles: JSON.parse(sessionStorage.getItem('roles')),
    loading: true
  };

  definitionState = {
    PAUSE: 'PAUSE',
    RESUME: 'RESUME',
    RESTART: 'RESTART',
    RESTART_TASK: 'RESTART_TASK'
  };

  componentDidMount() {
    this.getDefinition();
  }

  handleTasks() {
    const tasks = this.state.definition.tasks || [];
    let tableData = [];
    tasks.forEach(task => {
      tableData.push({
        id: JSON.stringify(task.id),
        worker: task.workerId,
        state: task.state,
        trace: task.trace
      });
    });
    this.setState({ tableData, loading: false });
  }

  async getDefinition() {
    let definition = {};
    const { clusterId, connectId, definitionId } = this.state;

    this.setState({ loading: true });
    definition = await this.getApi(uriGetDefinition(clusterId, connectId, definitionId));
    this.setState({ definition: definition.data }, () => this.handleTasks());
  }

  modifyDefinitionState = () => {
    const { definitionId } = this.state;
    const { uri, action, taskId } = this.state.definitionModifyData;

    this.getApi(uri)
      .then(() => this.getDefinition())
      .then(() => {
        toast.success(
          `${
            taskId !== undefined
              ? `Definition '${definitionId}' tasks ${taskId} is restarted`
              : `Definition '${definitionId}' is ${action}`
          }`
        );
        this.closeActionModal();
      });
  };

  showActionModal = definitionModifyMessage => {
    this.setState({ showActionModal: true, definitionModifyMessage });
  };

  closeActionModal = () => {
    this.setState({
      showActionModal: false,
      definitionModifyMessage: '',
      definitionModifyData: {}
    });
  };

  handleAction = (option, taskId) => {
    const { clusterId, connectId, definitionId } = this.state;
    let uri = '';
    let action = '';
    let failedAction = '';

    switch (option) {
      case 'PAUSE':
        uri = uriPauseDefinition(clusterId, connectId, definitionId);
        action = 'paused';
        failedAction = 'pause';
        break;
      case 'RESUME':
        uri = uriResumeDefinition(clusterId, connectId, definitionId);
        action = 'resumed';
        failedAction = 'resume';
        break;
      case 'RESTART':
        uri = uriRestartDefinition(clusterId, connectId, definitionId);
        action = 'restarted';
        failedAction = 'restart';
        break;
      case 'RESTART_TASK':
        uri = uriRestartTask(clusterId, connectId, definitionId, taskId);
        action = 'restarted';
        failedAction = 'restart';
        break;
      default:
        uri = uriResumeDefinition(clusterId, connectId, definitionId);
        action = 'resumed';
        failedAction = 'resume';
        break;
    }

    let definitionModifyData = {
      uri,
      action,
      failedAction
    };

    if (taskId !== undefined) definitionModifyData.taskId = taskId;

    this.setState({ definitionModifyData }, () => {
      this.showActionModal(
        taskId !== undefined ? (
          <React.Fragment>
            Do you want to restart task:{' '}
            {
              <code>
                {taskId} from {this.state.definitionId}
              </code>
            }{' '}
            ?
          </React.Fragment>
        ) : (
          <React.Fragment>
            Do you want to {failedAction} definition: {<code>{this.state.definitionId}</code>} ?
          </React.Fragment>
        )
      );
    });
  };

  renderTask = task => {
    let className = 'btn btn-sm mb-1 btn-';
    switch (task) {
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
    return <span className={`btn btn-sm mb-1 ${className}`}>{task}</span>;
  };

  render() {
    const { tableData, definition, loading } = this.state;
    const roles = this.state.roles || {};
    return (
      <div className="tab-pane active" role="tabpanel">
        <div className="table-responsive">
          <Table
            loading={loading}
            history={this.props.history}
            columns={[
              {
                id: 'id',
                name: 'id',
                accessor: 'id',
                colName: 'Id',
                type: 'text',
                sortable: true
              },
              {
                id: 'worker',
                name: 'worker',
                accessor: 'worker',
                colName: 'Worker',
                type: 'text',
                sortable: true
              },
              {
                id: 'state',
                name: 'state',
                accessor: 'state',
                colName: 'State',
                type: 'text',
                cell: (obj, col) => {
                  return this.renderTask(obj[col.accessor]);
                }
              },
              {
                id: 'trace',
                name: 'trace',
                accessor: 'trace',
                colName: 'Trace',
                type: 'text',
                extraRow: true,
                extraRowContent: (obj, index) => {
                  return (
                    <AceEditor
                      mode="text"
                      id={'value' + index}
                      theme="merbivore_soft"
                      value={obj.trace}
                      readOnly
                      name="UNIQUE_ID_OF_DIV"
                      editorProps={{ $blockScrolling: true }}
                      style={{ width: '100%', minHeight: '25vh' }}
                    />
                  );
                },
                cell: obj => {
                  return obj.trace ? (
                    <pre className="mb-0 khq-data-highlight">
                      <code>{obj.trace}</code>
                    </pre>
                  ) : null;
                }
              }
            ]}
            extraRow
            noStripes
            data={tableData}
            updateData={data => {
              this.setState({ tableData: data });
            }}
            actions={
              roles.CONNECTOR &&
              roles.CONNECTOR.includes('UPDATE_STATE') && [constants.TABLE_RESTART]
            }
            onRestart={row => {
              this.handleAction(this.definitionState.RESTART_TASK, row.id);
            }}
          />
        </div>
        {roles.CONNECTOR && roles.CONNECTOR.includes('UPDATE_STATE') && (
          <footer>
            {definition.paused ? (
              <li className="aside-button">
                <div
                  //href="#"
                  className="btn btn-primary mr-2"
                  onClick={() => this.handleAction(this.definitionState.RESUME)}
                >
                  <i className="fa fa-play" aria-hidden="true" /> Resume Definition
                </div>
              </li>
            ) : (
              <React.Fragment>
                <li className="aside-button">
                  <div
                    //href="#"
                    type="pause"
                    className="btn btn-primary mr-2"
                    onClick={() => this.handleAction(this.definitionState.PAUSE)}
                  >
                    <i className="fa fa-pause" aria-hidden="true" /> Pause Definition
                  </div>
                </li>

                <li className="aside-button">
                  <div
                    //href="#"
                    className="btn btn-primary mr-2"
                    onClick={() => this.handleAction(this.definitionState.RESTART)}
                  >
                    <i className="fa fa-refresh" aria-hidden="true" /> Restart Definition
                  </div>
                </li>
              </React.Fragment>
            )}
          </footer>
        )}
        <ConfirmModal
          show={this.state.showActionModal}
          handleCancel={this.closeActionModal}
          handleConfirm={this.modifyDefinitionState}
          message={this.state.definitionModifyMessage}
        />
      </div>
    );
  }
}

export default ConnectTasks;
