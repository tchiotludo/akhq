import React, { Component } from 'react';
import Table from '../../components/Table';
import endpoints, { uriDeleteSchema } from '../../utils/endpoints';
import constants from '../../utils/constants';
import history from '../../utils/history';
import { Link } from 'react-router-dom';
import Schema from './Schema/Schema';
import Header from '../Header';
import SearchBar from '../../components/SearchBar';
import Pagination from '../../components/Pagination';
import ConfirmModal from '../../components/Modal/ConfirmModal';
import api, { remove } from '../../utils/api';
import './styles.scss';
import CodeViewModal from '../../components/Modal/CodeViewModal/CodeViewModal';
class SchemaList extends Component {
  state = {
    schemasRegistry: [],
    showDeleteModal: false,
    selectedCluster: '',
    deleteMessage: '',
    schemaToDelete: {},
    deleteData: {},
    pageNumber: 1,
    totalPageNumber: 1,
    history: this.props,
    searchData: {
      search: ''
    },
    createSubjectFormData: {
      subject: '',
      compatibilityLevel: '',
      schema: ''
    },
    showSchemaModal: false,
    schemaModalBody: ''
  };

  componentDidMount() {
    let { clusterId } = this.props.match.params;

    this.setState({ selectedCluster: clusterId }, () => {
      this.getSchemaRegistry();
    });
  }

  showSchemaModal = body => {
    this.setState({
      showSchemaModal: true,
      schemaModalBody: body
    });
  };

  closeSchemaModal = () => {
    this.setState({ showSchemaModal: false, schemaModalBody: '' });
  };

  handleSearch = data => {
    const { searchData } = data;
    this.setState({ pageNumber: 1, searchData }, () => {
      this.getSchemaRegistry();
    });
  };

  handlePageChangeSubmission = value => {
    const { totalPageNumber } = this.state;
    if (value <= 0) {
      value = 1;
    } else if (value > totalPageNumber) {
      value = totalPageNumber;
    }
    this.setState({ pageNumber: value }, () => {
      this.getSchemaRegistry();
    });
  };

  handlePageChange = ({ currentTarget: input }) => {
    const { value } = input;
    this.setState({ pageNumber: value });
  };

  async getSchemaRegistry() {
    const { history } = this.props;
    const { selectedCluster, pageNumber } = this.state;
    const { search } = this.state.searchData;

    history.push({
      loading: true
    });
    try {
      let response = await api.get(
        endpoints.uriSchemaRegistry(selectedCluster, search, pageNumber)
      );
      response = response.data;
      if (response.results) {
        let schemasRegistry = response.results || [];
        this.handleSchemaRegistry(schemasRegistry);
        this.setState({ selectedCluster, totalPageNumber: response.page });
      }
    } catch (err) {
      history.replace('/error', { errorData: err });
    } finally {
      history.push({
        loading: false
      });
    }
  }

  handleSchemaRegistry(SchemaRegistry) {
    let tableSchemaRegistry = [];
    SchemaRegistry.map(SchemaRegistry => {
      SchemaRegistry.size = 0;
      SchemaRegistry.logDirSize = 0;
      tableSchemaRegistry.push({
        id: SchemaRegistry.id,
        subject: SchemaRegistry.subject,
        version: SchemaRegistry.version,
        schema: JSON.stringify(JSON.parse(SchemaRegistry.schema), null, 2)
      });
    });
    this.setState({ schemasRegistry: tableSchemaRegistry });
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
    const { history } = this.props;
    const deleteData = {
      clusterId: selectedCluster,
      subject: schemaToDelete.subject
    };
    history.push({ loading: true });
    remove(uriDeleteSchema(selectedCluster, schemaToDelete.subject), deleteData)
      .then(res => {
        this.props.history.push({
          showSuccessToast: true,
          successToastMessage: `Schema '${schemaToDelete.subject}' is deleted`,
          loading: false
        });
        this.setState({ showDeleteModal: false, schemaToDelete: {} });
        this.getSchemaRegistry();
      })
      .catch(err => {
        this.props.history.push({
          showErrorToast: true,
          errorToastMessage: `Could not delete '${schemaToDelete.subject}'`,
          loading: false
        });
        this.setState({ showDeleteModal: false, schemaToDelete: {} });
      });
  };
  render() {
    const {
      SchemaRegistry,
      selectedCluster,
      searchData,
      pageNumber,
      totalPageNumber,
      showSchemaModal,
      schemaModalBody
    } = this.state;
    const { history } = this.props;
    const { clusterId } = this.props.match.params;

    return (
      <div>
        <Header title="Schemas Registry" />
        <nav
          className="navbar navbar-expand-lg navbar-light bg-light mr-auto
         khq-data-filter khq-sticky khq-nav"
        >
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
            onChange={this.handlePageChange}
            onSubmit={this.handlePageChangeSubmission}
          />
        </nav>

        <Table
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
              id: 'version',
              accessor: 'version',
              colName: 'Version',
              cell: obj => {
                return this.handleVersion(obj.version);
              }
            },

            {
              id: 'schema',
              accessor: 'schema',
              colName: 'Schema',
              cell: (obj, col) => {
                return (
                  <div className="value cell-div">
                    <span className="align-cell value-span">
                      {obj[col.accessor] ? obj[col.accessor].substring(0, 150) : 'N/A'}
                      {obj[col.accessor] && obj[col.accessor].length > 100 && '(...)'}{' '}
                    </span>
                    <div className="value-button">
                      <button
                        className="btn btn-secondary headers pull-right"
                        onClick={() => this.showSchemaModal(obj[col.accessor])}
                      >
                        ...
                      </button>
                    </div>
                  </div>
                );
              }
            }
          ]}
          data={this.state.schemasRegistry}
          onDelete={schema => {
            this.handleOnDelete(schema);
          }}
          onDetails={schemaId => {
            let schema = this.state.schemasRegistry.find(schema => {
              return schema.id === schemaId;
            });
            history.push({
              pathname: `/${selectedCluster}/schema/details/${schema.subject}`,
              schemaId: schema.subject
            });
          }}
          actions={[constants.TABLE_DELETE, constants.TABLE_DETAILS]}
        />

        <div
          className="navbar navbar-expand-lg navbar-light mr-auto
         khq-data-filter khq-sticky khq-nav"
        >
          <div className="collapse navbar-collapse" />
          <Pagination
            pageNumber={pageNumber}
            totalPageNumber={totalPageNumber}
            onChange={this.handlePageChange}
            onSubmit={this.handlePageChangeSubmission}
          />
        </div>
        <aside>
          <Link
            to={{
              pathname: `/${clusterId}/schema/create`,
              state: { formData: this.state.createSubjectFormData }
            }}
            className="btn btn-primary"
          >
            Create a Subject
          </Link>
        </aside>

        <ConfirmModal
          show={this.state.showDeleteModal}
          handleCancel={this.closeDeleteModal}
          handleConfirm={this.deleteSchemaRegistry}
          message={this.state.deleteMessage}
        />

        <CodeViewModal
          show={showSchemaModal}
          body={schemaModalBody}
          handleClose={this.closeSchemaModal}
        />
      </div>
    );
  }
}

export default SchemaList;
