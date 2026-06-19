import React from 'react';
import Header from '../../Header';
import Table from '../../../components/Table/Table';
import constants from '../../../utils/constants';
import { Link } from 'react-router-dom';
import { uriConnectDefinitions, uriDeleteDefinition } from '../../../utils/endpoints';
import ConfirmModal from '../../../components/Modal/ConfirmModal/ConfirmModal';
import AceEditor from '../../../components/AceEditor/AceEditor';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Root from '../../../components/Root';
import SearchBar from '../../../components/SearchBar';
import Pagination from '../../../components/Pagination';
import { handlePageChange, getPageNumber } from './../../../utils/pagination';
import { withRouter } from '../../../utils/withRouter';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faBackward, faForward } from '@fortawesome/free-solid-svg-icons';
import { SETTINGS_VALUES } from '../../../utils/constants';
import { Tooltip } from '@mui/material';

class ConnectList extends Root {
  state = {
    clusterId: '',
    connectId: '',
    tableData: [],
    showDeleteModal: false,
    definitionToDelete: '',
    deleteMessage: '',
    roles: JSON.parse(sessionStorage.getItem('roles')),
    loading: true,
    pageNumber: 1,
    totalPageNumber: 1,
    searchData: {
      search: ''
    },
    statusFilter: ''
  };

  static getDerivedStateFromProps(nextProps) {
    const clusterId = nextProps.params.clusterId;
    const connectId = nextProps.params.connectId;

    return {
      clusterId: clusterId,
      connectId: connectId
    };
  }

  componentDidMount() {
    this._initializeVars(() => {
      this.getConnectDefinitions();
    });
  }

  _initializeVars(callbackFunction) {
    const query = new URLSearchParams(this.props.location.search);
    this.setState(
      {
        searchData: { search: query.get('search') ?? '' },
        pageNumber: query.get('page') ? parseInt(query.get('page')) : 1,
        statusFilter: query.get('status') ?? ''
      },
      callbackFunction
    );
  }

  componentDidUpdate(prevProps) {
    const pathnameChanged = this.props.location.pathname !== prevProps.location.pathname;
    const searchChanged = this.props.location.search !== prevProps.location.search;

    if (pathnameChanged || searchChanged) {
      this.cancelAxiosRequests();
      this.renewCancelToken();

      this._initializeVars(() => {
        this.getConnectDefinitions();
      });
    }
  }

  async getConnectDefinitions() {
    const { clusterId, connectId, pageNumber, statusFilter } = this.state;
    const { search } = this.state.searchData;

    this.setState({ loading: true });

    let response = await this.getApi(
      uriConnectDefinitions(clusterId, connectId, search || '', pageNumber, statusFilter)
    );
    let data = response.data;
    if (data.results) {
      this.handleData(data);
      this.setState({ selectedCluster: clusterId, totalPageNumber: data.page });
    } else {
      this.setState({ clusterId, tableData: [], loading: false });
    }
  }

  navigateWithParams = (search, pageNumber, statusFilter, replaceInNavigation = false) => {
    const { clusterId, connectId } = this.state;
    let searchParams = `search=${search || ''}&page=${pageNumber}`;
    if (statusFilter) {
      searchParams += `&status=${statusFilter}`;
    }

    this.props.router.navigate(
      {
        pathname: `/ui/${clusterId}/connect/${connectId}`,
        search: searchParams
      },
      { replace: replaceInNavigation }
    );
  };

  deleteDefinition = () => {
    const { clusterId, connectId, definitionToDelete: definition } = this.state;

    this.removeApi(uriDeleteDefinition(clusterId, connectId, definition))
      .then(() => {
        toast.success(`Definition '${definition}' is deleted`);
        this.setState({ showDeleteModal: false, definitionToDelete: '' }, () => {
          this.getConnectDefinitions();
        });
      })
      .catch(() => {
        this.setState({ showDeleteModal: false, topicToDelete: {} });
      });
  };

  handleData = data => {
    let tableData = [];
    tableData = data.results.map(connectDefinition => {
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

    this.setState({ tableData, loading: false, totalPageNumber: data.page });
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

    if (roles.CONNECTOR && roles.CONNECTOR.includes('READ')) {
      actions.push(constants.TABLE_DETAILS);
    }
    if (roles.CONNECTOR && roles.CONNECTOR.includes('DELETE')) {
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

  handleSearch = data => {
    const { searchData } = data;
    this.setState({ pageNumber: 1, searchData }, () => {
      this.navigateWithParams(searchData.search, 1, this.state.statusFilter, false);
    });
  };

  handleStatusFilterChange = e => {
    const nextStatusFilter = e.target.value;
    this.setState({ pageNumber: 1, statusFilter: nextStatusFilter }, () => {
      this.navigateWithParams(this.state.searchData.search, 1, nextStatusFilter, false);
    });
  };

  handlePageChangeSubmission = (value, replaceInNavigation) => {
    let pageNumber = getPageNumber(value, this.state.totalPageNumber);
    this.setState({ pageNumber: pageNumber }, () => {
      this.navigateWithParams(
        this.state.searchData.search,
        pageNumber,
        this.state.statusFilter,
        replaceInNavigation
      );
    });
  };

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
          <span className={`btn btn-sm mb-1 ${className}`}>
            {`${task.workerId} (${task.id}) `}
            <span className="badge bg-light">{task.state}</span>
          </span>
          <br />
        </React.Fragment>
      );
    }

    return renderedTasks;
  };

  render() {
    const { clusterId, connectId, tableData, loading, searchData, pageNumber, totalPageNumber, statusFilter } =
      this.state;
    const roles = this.state.roles || {};

    return (
      <div>
        <Header title={`Connect: ${connectId}`} />
        <nav className="navbar navbar-expand-lg navbar-light bg-light me-auto khq-data-filter khq-sticky khq-nav">
          <SearchBar
            showSearch={true}
            search={searchData.search}
            showPagination={true}
            pagination={pageNumber}
            doSubmit={this.handleSearch}
          />

          <Tooltip title="Shows connectors with at least one task in the selected state">
            <select
              className="form-select ms-2"
              value={statusFilter}
              onChange={this.handleStatusFilterChange}
              style={{ width: 'auto' }}
            >
              <option value="">All statuses</option>
              {Object.values(SETTINGS_VALUES.CONNECT.TASK_STATUS_FILTERS).map(status => (
                <option key={status} value={status}>
                  {status.charAt(0) + status.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </Tooltip>

          <Pagination
            pageNumber={pageNumber}
            totalPageNumber={totalPageNumber}
            onChange={handlePageChange}
            onSubmit={value => this.handlePageChangeSubmission(value, false)}
          />
        </nav>

        <Table
          loading={loading}
          columns={[
            {
              id: 'id',
              name: 'id',
              accessor: 'id',
              colName: 'Name',
              type: 'text',
              sortable: true
            },
            {
              id: 'config',
              name: 'config',
              accessor: 'config',
              colName: 'Config',
              type: 'text',
              extraRow: true,
              extraRowContent: (obj, col, index) => {
                return (
                  <AceEditor
                    mode="json"
                    id={'value' + index}
                    value={JSON.stringify(JSON.parse(obj[col.accessor]), null, 2)}
                    readOnly
                    style={{ width: '100%', minHeight: '25vh' }}
                  />
                );
              },
              cell: (obj, col) => {
                return (
                  <pre className="mb-0 khq-data-highlight">
                    <code onClick={() => JSON.stringify(JSON.parse(obj[col.accessor]), null, 2)}>
                      {obj[col.accessor]}
                    </code>
                  </pre>
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
                      <FontAwesomeIcon icon={faForward} aria-hidden={true} />
                      {` ${obj[col.accessor].shortClassName}`}
                    </React.Fragment>
                  );
                }
                return (
                  <React.Fragment>
                    <FontAwesomeIcon icon={faBackward} aria-hidden={true} />
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
          updateData={data => {
            this.setState({ tableData: data });
          }}
          actions={this.getTableActions()}
          detailsHref={name => `/ui/${clusterId}/connect/${connectId}/definition/${name}`}
          onDelete={row => {
            this.handleOnDelete(row.id);
          }}
          extraRow
          noStripes
          onExpand={obj => {
            return Object.keys(obj.headers).map((header, i) => {
              return (
                <tr
                  key={i}
                  style={{
                    display: 'flex',
                    flexDirection: 'row',
                    width: '100%'
                  }}
                >
                  <td
                    style={{
                      width: '100%',
                      display: 'flex',
                      borderStyle: 'dashed',
                      borderWidth: '1px',
                      backgroundColor: '#171819'
                    }}
                  >
                    {header}
                  </td>
                  <td
                    style={{
                      width: '100%',
                      display: 'flex',
                      borderStyle: 'dashed',
                      borderWidth: '1px',
                      backgroundColor: '#171819'
                    }}
                  >
                    {obj.headers[header]}
                  </td>
                </tr>
              );
            });
          }}
          noContent={'No connectors available'}
        />
        {roles.CONNECTOR && roles.CONNECTOR.includes('CREATE') && (
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
      </div>
    );
  }
}

export default withRouter(ConnectList);
