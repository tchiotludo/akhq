import React, { Component } from 'react';
import Table from '../../../../components/Table';
import constants from '../../../../utils/constants';
import ConfirmModal from '../../../../components/Modal/ConfirmModal';
import { remove } from '../../../../utils/api';
import { uriDeleteSchemaVersion } from '../../../../utils/endpoints';
import AceEditor from 'react-ace';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import 'ace-builds/webpack-resolver';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-merbivore_soft';

class SchemaVersions extends Component {
  state = {
    data: [],
    selectedCluster: this.props.clusterId,
    selectedSchema: this.props.schemaName,
    schemaVersions: this.props.schemas,
    deleteMessage: '',
    schemaToDelete: {},
    deleteData: { clusterId: '', subject: '', versionId: 1 },
    roles: JSON.parse(sessionStorage.getItem('roles'))
  };

  componentDidMount() {
    this.handleData(this.state.schemaVersions);
  }

  handleData(schemas) {

    if (schemas) {
      let data = schemas.map(schema => {
        return {
          id: schema.id,
          version: schema.version,
          schema: JSON.stringify(JSON.parse(schema.schema), null, 2)
        };
      });
      this.setState({ data });
    } else {
      this.setState({ data: [] });
    }
  }

  handleOnDelete(schema) {
    this.setState({ schemaToDelete: schema }, () => {
      this.showDeleteModal(
        <React.Fragment>
          Do you want to delete version:{' '}
          {
            <code>
              {schema.id} from {this.state.selectedSchema}
            </code>
          }{' '}
          ?
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

  deleteSchemaRegistry = () => {
    const { selectedCluster, schemaToDelete, selectedSchema } = this.state;
    const { history } = this.props;
    const deleteData = {
      clusterId: selectedCluster,
      subject: selectedSchema,
      versionId: schemaToDelete.version
    };

    remove(
      uriDeleteSchemaVersion(selectedCluster, selectedSchema, schemaToDelete.version),
      deleteData
    )
      .then(res => {
        toast.success(`Version'${schemaToDelete.version}' is deleted`);
        this.setState({ showDeleteModal: false, schemaToDelete: {} });
        history.push({
          pathname: `/ui/${selectedCluster}/schema`
        });
      })
      .catch(() => {
        this.setState({ showDeleteModal: false, schemaToDelete: {} });
      });
  };

  render() {
    const roles = this.state.roles || {};
    return (
      <div>
        <Table
          columns={[
            {
              id: 'id',
              accessor: 'id',
              colName: 'Id',
              type: 'text',
              sortable: true
            },
            {
              id: 'version',
              accessor: 'version',
              colName: 'Version',
              type: 'text',
              sortable: true,
              cell: (obj, col) => {
                return <span className="badge badge-primary">{obj[col.accessor] || ''}</span>;
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
                    mode="json"
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
                return (
                  <pre className="mb-0 khq-data-highlight">
                    <code>
                      {JSON.stringify(JSON.parse(obj[col.accessor]))}
                    </code>
                  </pre>
                );
              }
            }
          ]}
          data={this.state.data}
          updateData={data => {
            this.setState({ data });
          }}
          onDelete={schema => {
            this.handleOnDelete(schema);
          }}
          actions={
            roles.registry && roles.registry['registry/version/delete']
              ? [constants.TABLE_DELETE]
              : []
          }
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
        />
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
export default SchemaVersions;
