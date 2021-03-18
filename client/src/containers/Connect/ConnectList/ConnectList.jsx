import React from 'react';
import Header from '../../Header';
import Table from '../../../components/Table/Table';
import constants from '../../../utils/constants';
import { Link } from 'react-router-dom';
import { uriConnectDefinitions, uriDeleteDefinition } from '../../../utils/endpoints';
import ConfirmModal from '../../../components/Modal/ConfirmModal/ConfirmModal';
import AceEditor from 'react-ace';
import 'ace-builds/webpack-resolver';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-merbivore_soft';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Root from "../../../components/Root";
import SearchBar from "../../../components/SearchBar";
import Pagination from "../../../components/Pagination";
import {handlePageChange, getPageNumber} from "./../../../utils/pagination"

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
    history: this.props,
    searchData: {
      search: ''
    },
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
    const { searchData, pageNumber } = this.state;
    const query =  new URLSearchParams(this.props.location.search);
    this.setState({
      searchData: { search: (query.get('search'))? query.get('search') : searchData.search },
      pageNumber: (query.get('page'))? parseInt(query.get('page')) : parseInt(pageNumber)
    }, () => {
      this.getConnectDefinitions();
    });
  }

  async getConnectDefinitions() {
    const { clusterId, connectId, pageNumber } = this.state;
    const { search } = this.state.searchData;

    this.setState({ loading: true });

    let response = await this.getApi(uriConnectDefinitions(clusterId, connectId, search, pageNumber));
    let data = response.data;
    if (data.results) {
      this.handleData(data);
      this.setState({ selectedCluster: clusterId, totalPageNumber: data.page }, () => {
        this.props.history.push({
          pathname: `/ui/${this.state.clusterId}/connect/${this.state.connectId}`,
          search: `search=${this.state.searchData.search}&page=${pageNumber}`
        })
      });
    } else {
      this.setState({ clusterId, tableData: [], totalPageNumber: 0, loading: false });
    }
  }

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

    if (roles.connect && roles.connect['connect/read']) {
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

  handleSearch = data => {
    const { searchData } = data;
    this.setState({ pageNumber: 1, searchData }, () => {
      this.getConnectDefinitions();
    });
  };

  handlePageChangeSubmission = value => {
    let pageNumber = getPageNumber(value, this.state.totalPageNumber);
    this.setState({ pageNumber: pageNumber }, () => {
      this.getConnectDefinitions();
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
            <span className="badge badge-light">{task.state}</span>
          </span>
          <br />
        </React.Fragment>
      );
    }

    return renderedTasks;
  };

  render() {
    const { clusterId, connectId, tableData, loading, searchData, pageNumber, totalPageNumber } = this.state;
    const roles = this.state.roles || {};
    const { history } = this.props;

    return (
      <div>
        <Header title={`Connect: ${connectId}`} history={history} />
        <nav
            className="navbar navbar-expand-lg navbar-light bg-light mr-auto
         khq-data-filter khq-sticky khq-nav"
        >
          <SearchBar
              showSearch={true}
              search={searchData.search}
              showPagination={true}
              pagination={pageNumber}
              doSubmit={this.handleSearch}
          />

          <Pagination
              pageNumber={pageNumber}
              totalPageNumber={totalPageNumber}
              onChange={handlePageChange}
              onSubmit={this.handlePageChangeSubmission}
          />
        </nav>

        <Table
          loading={loading}
          history={history}
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
                    theme="merbivore_soft"
                    value={JSON.stringify(JSON.parse(obj[col.accessor]), null, 2)}
                    readOnly
                    name="UNIQUE_ID_OF_DIV"
                    editorProps={{ $blockScrolling: true }}
                    style={{ width: '100%', minHeight: '25vh' }}
                  />
                );
              },
              cell: (obj, col) => {
                return (
                  <pre class="mb-0 khq-data-highlight">
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
                      <i className="fa fa-forward" aria-hidden="true" />
                      {` ${obj[col.accessor].shortClassName}`}
                    </React.Fragment>
                  );
                }
                return (
                  <React.Fragment>
                    <i className="fa fa-backward" aria-hidden="true" />
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
          onDetails={name => `/ui/${clusterId}/connect/${connectId}/definition/${name}` }
          onDelete={row => {
            this.handleOnDelete(row.id);
          }}
          extraRow
          noStripes
          onExpand={obj => {
            return Object.keys(obj.headers).map(header => {
              return (
                <tr
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
      </div>
    );
  }
}

export default ConnectList;
