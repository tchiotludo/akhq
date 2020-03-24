import React, { Component } from 'react';
import Table from '../../components/Table';
import endpoints, { uriDeleteSchema } from '../../utils/endpoints';
import constants from '../../utils/constants';
import history from '../../utils/history';
import { Link } from 'react-router-dom';
import SchemaRegistry from './SchemaRegistry/SchemaRegistry';
import Header from '../Header';
import SearchBar from '../../components/SearchBar';
import Pagination from '../../components/Pagination';
import ConfirmModal from '../../components/Modal/ConfirmModal';
import api, { remove } from '../../utils/api';

class SchemaRegistryList extends Component {
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
    }
  };

  componentDidMount() {
    let { clusterId } = this.props.match.params;

    this.setState({ selectedCluster: clusterId }, () => {
      this.getSchemaRegistry();
    });
  }

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
    console.log(selectedCluster, search, pageNumber);
    try {
      let response = await api.get(
        endpoints.uriSchemaRegistry(selectedCluster, search, pageNumber)
      );
      response = response.data;
      console.log(response);
      if (response) {
        let schemasRegistry = response.list || [];
        this.handleSchemaRegistry(schemasRegistry);
        this.setState({ selectedCluster, totalPageNumber: response.totalPageNumber });
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
    console.log('Im here');
    console.log(SchemaRegistry);
    let tableSchemaRegistry = [];
    SchemaRegistry.map(SchemaRegistry => {
      SchemaRegistry.size = 0;
      SchemaRegistry.logDirSize = 0;
      tableSchemaRegistry.push({
        id: SchemaRegistry.id,
        subject: SchemaRegistry.subject,
        version: SchemaRegistry.version,
        schema: SchemaRegistry.schema
      });
    });
    this.setState({ schemasRegistry: tableSchemaRegistry });
  }

  handleVersion(version) {
    return <span className="badge badge-primary"> {version}</span>;
  }

  handleOnDelete(schema) {
    this.setState({ schemaToDelete: schema }, () => {
      this.showDeleteModal(`Delete SchemaRegistry ${schema.id}?`);
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
      schemaId: schemaToDelete.id
    };

    history.push({ loading: true });
    remove(uriDeleteSchema(), deleteData)
      .then(res => {
        this.props.history.push({
          showSuccessToast: true,
          successToastMessage: `Schema '${schemaToDelete.id}' is deleted`,
          loading: false
        });
        this.setState({ showDeleteModal: false, schemaToDelete: {} });
        this.handleSchemaRegistry(res.data.schema);
      })
      .catch(err => {
        this.props.history.push({
          showErrorToast: true,
          errorToastMessage: `Could not delete '${schemaToDelete.id}'`,
          loading: false
        });
        this.setState({ showDeleteModal: false, schemaToDelete: {} });
      });
  };
  render() {
    const { SchemaRegistry, selectedCluster, searchData, pageNumber, totalPageNumber } = this.state;
    const { history } = this.props;
    const { clusterId } = this.props.match.params;

    return (
      <div id="content">
        <Header title="Consumer schemas" />
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
                console.log(obj.version);
                return this.handleVersion(obj.version);
              }
            },

            {
              id: 'schema',
              accessor: 'schema',
              colName: 'Schema'
            }
          ]}
          data={this.state.schemasRegistry}
          onDelete={schema => {
            this.handleOnDelete(schema);
          }}
          onDetails={id => {
            history.push(`/${selectedCluster}/schema/${id}`);
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
      </div>
    );
  }
}
export default SchemaRegistryList;
