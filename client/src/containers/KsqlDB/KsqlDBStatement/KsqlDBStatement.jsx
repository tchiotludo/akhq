import React from 'react';
import { uriKsqlDBExecuteStatement } from '../../../utils/endpoints';
import Header from '../../Header/Header';
import 'ace-builds/webpack-resolver';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-merbivore_soft';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Root from '../../../components/Root';
import AceEditor from 'react-ace';
import { withRouter } from '../../../utils/withRouter';

class KsqlDBStatement extends Root {
  state = {
    clusterId: this.props.match.params.clusterId,
    ksqlDBId: this.props.match.params.ksqlDBId,
    formData: {},
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

  async doSubmit() {
    const { clusterId, ksqlDBId, formData } = this.state;
    let body = {
      sql: formData.sql
    };

    this.putApi(uriKsqlDBExecuteStatement(clusterId, ksqlDBId), body).then(() => {
      this.props.history.push({
        pathname: `/ui/${clusterId}/ksqldb/${ksqlDBId}`
      });

      toast.success('Statement was executed successfully');
    });
  }

  render() {
    const { formData } = this.state;
    const { history } = this.props;

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
          <Header title={'Execute statement'} history={history} />

          <div className="form-group row">
            <label className="col-sm-2 col-form-label">
              SQL
              <br />
              <span className="text-white font-weight-light">
                This SQL has to conform to
                <a
                  href="https://docs.ksqldb.io/en/latest/developer-guide/ksqldb-clients/java-client/#execute-statement"
                  target="_blank"
                  rel="noreferrer"
                >
                  {' '}
                  executeStatement()
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

export default withRouter(KsqlDBStatement);
