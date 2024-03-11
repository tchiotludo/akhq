import React from 'react';
import { uriKsqlDBExecuteQuery } from '../../../utils/endpoints';
import Header from '../../Header/Header';
import 'ace-builds/webpack-resolver';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-merbivore_soft';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Root from '../../../components/Root';
import AceEditor from 'react-ace';
import './styles.scss';
import Table from '../../../components/Table';
import { withRouter } from '../../../utils/withRouter';

class KsqlDBQuery extends Root {
  state = {
    clusterId: this.props.params.clusterId,
    ksqlDBId: this.props.params.ksqlDBId,
    formData: {},
    properties: {},
    tableData: [],
    tableColumns: [],
    loading: false,
    errors: {}
  };

  componentDidUpdate(prevProps) {
    if (this.props.location === prevProps.location) {
      let height = document.getElementById('root').offsetHeight;
      document.getElementsByClassName('sidenav---sidenav---_2tBP')[0].style.height = height + 'px';
    }
  }

  submitDisabled = () => {
    return !this.state.formData.sql || this.state.formData.sql === '';
  };
  buildQueryProperties = input => {
    let properties = {};
    input
      .split(/\r?\n/)
      .filter(value => value.includes('='))
      .forEach(value => {
        const keyValueProperty = value.split('=');
        properties = { ...properties, [keyValueProperty[0]]: keyValueProperty[1] };
      });
  };

  buildTable = responseData => {
    this.setState({
      tableColumns:
        (Array.isArray(responseData.columnNames) &&
          responseData.columnNames.map(value => ({
            id: value.toLowerCase(),
            name: value.toLowerCase(),
            accessor: value.toLowerCase(),
            colName: value,
            type: 'text',
            sortable: true
          }))) ||
        [],
      tableData:
        (Array.isArray(responseData.columnValues) &&
          responseData.columnValues.map(value => {
            const rowValues = JSON.parse(value);
            let row = {};
            responseData.columnNames.forEach((columnName, index) => {
              row = { ...row, [columnName.toLowerCase()]: rowValues[index] };
            });
            return row;
          })) ||
        [],
      loading: false
    });
  };

  async doSubmit() {
    const { clusterId, ksqlDBId, formData, properties } = this.state;
    let body = {
      sql: formData.sql,
      properties
    };
    this.setState({ loading: true, tableColumns: [], tableData: [] });

    this.putApi(uriKsqlDBExecuteQuery(clusterId, ksqlDBId), body)
      .then(response => {
        this.buildTable(response.data);
        toast.success('Query was executed successfully, see results below.');
      })
      .catch(() => this.setState({ loading: false }));
  }

  render() {
    const { formData, tableData, tableColumns, loading } = this.state;

    return (
      <div>
        <form
          encType="multipart/form-data"
          className="khq-form khq-form-config"
          onSubmit={e => {
            e.preventDefault();
            this.doSubmit();
          }}
        >
          <Header title={'Execute query'} />

          <div className="form-group row">
            <label className="col-sm-2 col-form-label">
              SQL
              <br />
              <span className="text-white font-weight-light">
                This SQL has to conform to
                <a
                  href="https://docs.ksqldb.io/en/latest/developer-guide/ksqldb-clients/java-client/#execute-query"
                  target="_blank"
                  rel="noreferrer"
                >
                  {' '}
                  executeQuery()
                </a>
                .
              </span>
            </label>

            <div className="col-sm-10">
              <AceEditor
                mode="sql"
                id={'sql'}
                theme="merbivore_soft"
                value={formData['sql']}
                placeholder="SELECT * FROM ORDERS LIMIT 5;"
                onChange={value => {
                  let { formData } = this.state;
                  formData['sql'] = value;
                  this.setState({ formData });
                }}
                name="UNIQUE_ID_OF_DIV"
                editorProps={{ $blockScrolling: true }}
                style={{ width: '100%', height: '200px' }}
              />
            </div>
          </div>

          <div className="form-group row">
            <label className="col-sm-2 col-form-label">Properties</label>

            <div className="col-sm-10">
              <AceEditor
                mode="properties"
                id={'properties'}
                theme="merbivore_soft"
                placeholder={'auto.offset.reset=earliest\nksql.query.pull.table.scan.enabled=true'}
                onChange={value => {
                  this.buildQueryProperties(value);
                }}
                name="UNIQUE_ID_OF_DIV"
                editorProps={{ $blockScrolling: true }}
                style={{ width: '100%', height: '100px' }}
              />
            </div>
          </div>

          <div className="form-group row">
            <label className="col-sm-2 col-form-label">Result</label>

            <div className="col-sm-10">
              <Table
                loading={loading}
                columns={tableColumns}
                data={tableData}
                updateData={data => {
                  this.setState({ tableData: data });
                }}
                extraRow
                noStripes
                noContent={'Execute a query to se results'}
              />
            </div>
          </div>

          <div className="khq-submit button-footer" style={{ marginRight: 0 }}>
            <aside>
              <button type={'submit'} className="btn btn-primary" disabled={this.submitDisabled()}>
                Execute
              </button>
            </aside>
          </div>
        </form>
      </div>
    );
  }
}

export default withRouter(KsqlDBQuery);
