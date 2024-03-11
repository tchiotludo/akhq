import React from 'react';
import Table from '../../../components/Table';
import endpoints, { uriDeleteSchema } from '../../../utils/endpoints';
import constants from '../../../utils/constants';
import { Link } from 'react-router-dom';
import Header from '../../Header';
import SearchBar from '../../../components/SearchBar';
import Pagination from '../../../components/Pagination';
import ConfirmModal from '../../../components/Modal/ConfirmModal';
import './styles.scss';
import AceEditor from 'react-ace';
import 'ace-builds/webpack-resolver';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/mode-protobuf';
import 'ace-builds/src-noconflict/theme-merbivore_soft';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Root from '../../../components/Root';
import { handlePageChange, getPageNumber } from './../../../utils/pagination';
import { withRouter } from '../../../utils/withRouter';

class SchemaList extends Root {
  state = {
    schemasRegistry: [],
    showDeleteModal: false,
    selectedCluster: '',
    deleteMessage: '',
    schemaToDelete: {},
    deleteData: {},
    pageNumber: 1,
    totalPageNumber: 1,
    searchData: {
      search: ''
    },
    createSubjectFormData: {
      subject: '',
      compatibilityLevel: '',
      schema: ''
    },
    roles: JSON.parse(sessionStorage.getItem('roles')),
    loading: true
  };

  componentDidMount() {
    let { clusterId } = this.props.params;
    const { searchData, pageNumber } = this.state;
    const query = new URLSearchParams(this.props.location.search);

    this.setState(
      {
        selectedCluster: clusterId,
        searchData: { search: query.get('search') ? query.get('search') : searchData.search },
        pageNumber: query.get('page') ? parseInt(query.get('page')) : parseInt(pageNumber)
      },
      () => {
        this.getSchemaRegistry();
      }
    );
  }

  handleSearch = data => {
    const { searchData } = data;

    // Cancel previous requests is there are some to prevent UI issues
    this.cancelAxiosRequests();
    this.renewCancelToken();

    this.setState({ pageNumber: 1, searchData }, () => {
      this.getSchemaRegistry();
    });
  };

  handlePageChangeSubmission = value => {
    let pageNumber = parseInt(getPageNumber(value, this.state.totalPageNumber));
    this.setState({ pageNumber: pageNumber }, () => {
      this.getSchemaRegistry();
    });
  };

  async getSchemaRegistry() {
    const { selectedCluster, pageNumber } = this.state;
    const { search } = this.state.searchData;

    this.setState({ loading: true });

    let response = await this.getApi(
      endpoints.uriSchemaRegistry(selectedCluster, search, pageNumber)
    );

    let data = response.data;
    if (data.results) {
      this.handleSchemaRegistry(data.results);
      this.setState({ selectedCluster, totalPageNumber: data.page }, () => {
        this.props.router.navigate({
          pathname: `/ui/${this.state.selectedCluster}/schema`,
          search: `search=${this.state.searchData.search}&page=${pageNumber}`
        });
      });
    } else {
      this.setState({ selectedCluster, schemasRegistry: [], totalPageNumber: 0, loading: false });
    }
  }

  handleSchemaRegistry(schemas) {
    let tableSchemaRegistry = [];
    schemas.forEach(schema => {
      schema.size = 0;
      schema.logDirSize = 0;

      tableSchemaRegistry.push({
        id: schema.id,
        subject: schema.subject,
        version: schema.version,
        exception: schema.exception,
        schemaType: schema.schemaType,
        schema:
          schema.schemaType === 'PROTOBUF'
            ? schema.schema
            : schema.schema
              ? JSON.stringify(JSON.parse(schema.schema), null, 2)
              : null
      });
    });
    this.setState({ schemasRegistry: tableSchemaRegistry, loading: false });
  }

  handleVersion(version) {
    return <span className="badge badge-primary"> {version}</span>;
  }

  handleOnDelete(schema) {
    this.setState({ schemaToDelete: schema }, () => {
      this.showDeleteModal(`Delete SchemaRegistry ${schema.subject}?`);
    });
  }

  showDeleteModal = deleteMessage => {
    this.setState({ showDeleteModal: true, deleteMessage });
  };

  closeDeleteModal = () => {
    this.setState({ showDeleteModal: false, deleteMessage: '' });
  };

  deleteSchemaRegistry = () => {
    const { selectedCluster, schemaToDelete } = this.state;
    const deleteData = {
      clusterId: selectedCluster,
      subject: schemaToDelete.subject
    };

    this.removeApi(uriDeleteSchema(selectedCluster, schemaToDelete.subject), deleteData)
      .then(() => {
        toast.success(`Schema '${schemaToDelete.subject}' is deleted`);
        this.setState({ showDeleteModal: false, schemaToDelete: {} });
        this.getSchemaRegistry();
      })
      .catch(() => {
        this.setState({ showDeleteModal: false, schemaToDelete: {} });
      });
  };
  render() {
    const { selectedCluster, searchData, pageNumber, totalPageNumber, loading } = this.state;
    const roles = this.state.roles || {};
    const { clusterId } = this.props.params;

    return (
      <div>
        <Header title="Schema Registry" />
        <nav className="navbar navbar-expand-lg mr-auto khq-data-filter khq-sticky khq-nav">
          <SearchBar
            showSearch={true}
            search={searchData.search}
            showPagination={true}
            pagination={pageNumber}
            showTopicListView={false}
            showSchemaRegistry
            schemaListView={'ALL'}
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
          columns={[
            {
              id: 'id',
              accessor: 'id',
              colName: 'Id'
            },
            {
              id: 'subject',
              accessor: 'subject',
              colName: 'Subject'
            },
            {
              id: 'schemaType',
              accessor: 'schemaType',
              colName: 'Schema Type'
            },
            {
              id: 'version',
              accessor: 'version',
              colName: 'Version',
              cell: obj => {
                return this.handleVersion(obj.version);
              }
            },
            {
              id: 'schema',
              name: 'schema',
              accessor: 'schema',
              colName: 'Schema',
              type: 'text',
              extraRow: true,
              extraRowContent: (obj, col, index) => {
                return (
                  <AceEditor
                    mode={obj.schemaType === 'PROTOBUF' ? 'protobuf' : 'json'}
                    id={'value' + index}
                    theme="merbivore_soft"
                    value={obj[col.accessor]}
                    readOnly
                    name="UNIQUE_ID_OF_DIV"
                    editorProps={{ $blockScrolling: true }}
                    style={{ width: '100%', minHeight: '25vh' }}
                  />
                );
              },
              cell: (obj, col) => {
                if (obj[col.accessor]) {
                  return (
                    <pre className="mb-0 khq-data-highlight">
                      <code>
                        {obj.schemaType === 'PROTOBUF'
                          ? obj[col.accessor]
                          : JSON.stringify(JSON.parse(obj[col.accessor]))}
                      </code>
                    </pre>
                  );
                } else if (obj.exception) {
                  return (
                    <div className="alert alert-warning mb-0" role="alert">
                      {obj.exception}
                    </div>
                  );
                }
              }
            }
          ]}
          data={this.state.schemasRegistry}
          updateData={data => {
            this.setState({ schemasRegistry: data });
          }}
          onDelete={schema => {
            this.handleOnDelete(schema);
          }}
          idCol="subject"
          onDetails={subject => {
            return `/ui/${selectedCluster}/schema/details/${encodeURIComponent(subject)}`;
          }}
          actions={
            roles.SCHEMA && roles.SCHEMA.includes('DELETE')
              ? [constants.TABLE_DELETE, constants.TABLE_DETAILS]
              : [constants.TABLE_DETAILS]
          }
          extraRow
          noStripes
          handleExtraExpand={(extraExpanded, el) => {
            const currentExpandedRows = extraExpanded;
            const isRowCurrentlyExpanded = currentExpandedRows.includes(el.subject);

            const newExpandedRows = isRowCurrentlyExpanded
              ? currentExpandedRows
              : currentExpandedRows.concat({ id: el.id, subject: el.subject });
            return newExpandedRows;
          }}
          handleExtraCollapse={(extraExpanded, el) => {
            const currentExpandedRows = extraExpanded;
            const isRowCurrentlyExpanded = currentExpandedRows.some(
              obj => obj.subject === el.subject
            );

            const newExpandedRows = !isRowCurrentlyExpanded
              ? currentExpandedRows
              : currentExpandedRows.filter(
                  obj => !(obj.id === el.id && obj.subject === el.subject)
                );
            return newExpandedRows;
          }}
          noContent={'No schemas available'}
        />
        {roles.SCHEMA && roles.SCHEMA.includes('CREATE') && (
          <footer>
            <Link
              to={{
                pathname: `/ui/${clusterId}/schema/create`,
                state: { formData: this.state.createSubjectFormData }
              }}
              className="btn btn-primary"
            >
              Create a Subject
            </Link>
          </footer>
        )}

        <ConfirmModal
          show={this.state.showDeleteModal}
          handleCancel={this.closeDeleteModal}
          handleConfirm={this.deleteSchemaRegistry}
          message={this.state.deleteMessage}
        />
      </div>
    );
  }
}

export default withRouter(SchemaList);
