import React from 'react';
import Joi from 'joi-browser';
import './styles.scss';
import { uriConnectPlugins, uriCreateConnect } from '../../../utils/endpoints';
import Header from '../../Header/Header';
import constants from '../../../utils/constants';
import Select from '../../../components/Form/Select';
import AceEditor from 'react-ace';
import filter from 'lodash/filter';
import 'ace-builds/webpack-resolver';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-merbivore_soft';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Root from '../../../components/Root';
import { withRouter } from '../../../utils/withRouter';

class ConnectCreate extends Root {
  state = {
    clusterId: this.props.match.params.clusterId,
    connectId: this.props.match.params.connectId,
    formData: {},
    errors: {},
    plugins: [],
    selectedType: '',
    display: '',
    plugin: {}
  };

  schema = {};
  componentDidMount() {
    this.getPlugins();
  }

  componentDidUpdate(prevProps) {
    if (this.props.location === prevProps.location) {
      let height = document.getElementById('root').offsetHeight;
      document.getElementsByClassName('sidenav---sidenav---_2tBP')[0].style.height = height + 'px';
    }
  }

  async getPlugins() {
    const { connectId, clusterId } = this.state;

    let plugins = await this.getApi(uriConnectPlugins(clusterId, connectId));
    this.setState({ clusterId, connectId, plugins: plugins.data });
  }

  onTypeChange = value => {
    let formData = {};
    this.setState({ selectedType: value, formData }, () => {
      this.renderForm();
    });
  };

  handleSchema(definitions) {
    this.schema = {};
    let formData = {};
    formData.type = this.state.selectedType;
    this.schema['type'] = Joi.string().required();
    formData.subject = '';
    this.schema['subject'] = Joi.string().required();
    definitions.forEach(definition => {
      let config = this.handleDefinition(definition);
      formData[definition.name] = '';
      this.schema[definition.name] = config;
      if (definition.name === 'transforms') {
        formData['transformsprops'] = '{}';
        this.schema['transformsprops'] = Joi.object().required();
      }
    });
    this.setState({ formData });
  }

  handleDefinition(definition) {
    let def = '';
    if (definition.name !== 'name' && definition.name !== 'connect.class') {
      if (definition.required) {
        switch (definition.type) {
          case constants.TYPES.LONG:
          case constants.TYPES.INT:
          case constants.TYPES.DOUBLE:
          case constants.TYPES.SHORT:
            def = Joi.number().required();
            break;
          case constants.TYPES.PASSWORD:
            def = Joi.password().required();
            break;
          case constants.TYPES.BOOLEAN:
            def = Joi.boolean().required();
            break;
          default:
            def = Joi.string().required();
            break;
        }
      } else {
        switch (definition.type) {
          case constants.TYPES.LONG:
          case constants.TYPES.INT:
          case constants.TYPES.DOUBLE:
          case constants.TYPES.SHORT:
            def = Joi.number().allow('');
            break;

          case constants.TYPES.BOOLEAN:
            def = Joi.boolean().allow('');
            break;
          default:
            def = Joi.string().allow('');
            break;
        }
      }
    } else {
      def = Joi.string().allow('');
    }

    return def;
  }

  renderTableRows(plugin) {
    let rows = '';
    let title = '';
    let { formData } = this.state;
    const errors = [];
    const errorMessage = this.validateProperty({ name: plugin.name, value: formData[plugin.name] });
    if (errorMessage) {
      errors[plugin.name] = errorMessage;
    }

    switch (plugin.importance) {
      case 'HIGH':
        title = (
          <span>
            {plugin.displayName}
            {''}
            <i
              className="fa fa-exclamation text-danger"
              style={{ marginleft: '2%' }}
              aria-hidden="true"
            />
          </span>
        );
        break;
      case 'MEDIUM':
        title = (
          <span>
            {plugin.displayName}
            <i
              className="fa fa-info text-warning"
              style={{ marginleft: '2%' }}
              aria-hidden="true"
            />
          </span>
        );
        break;
      default:
        title = <span>{plugin.displayName}</span>;
        break;
    }
    let name = <code>{plugin.name}</code>;
    let required = {};
    if (plugin.required) {
      required = <code style={{ color: 'red' }}>Required</code>;
    } else {
      required = <React.Fragment></React.Fragment>;
    }

    let documentation = (
      <small
        className="form-text text-muted"
        dangerouslySetInnerHTML={{ __html: plugin.documentation }}
      ></small>
    );

    rows = (
      <React.Fragment key={plugin.name}>
        <td>
          {title}
          <br></br>
          {name}
          <br></br>
          {required}
          <br></br>
          {documentation}
        </td>
        <td>
          <input
            type="text"
            className="form-control"
            value={formData[plugin.name]}
            name={plugin.name}
            disabled={plugin.name === 'name' || plugin.name === 'connector.class'}
            placeholder={plugin.defaultValue > 0 ? plugin.defaultValue : ''}
            onChange={({ currentTarget: input }) => {
              let { formData } = this.state;
              formData[plugin.name] = input.value;
              this.handleData();
              this.setState({ formData });
            }}
          >
            {formData[plugin.required]}
          </input>

          {errors[plugin.name] && (
            <div id="input-error" className="alert alert-danger mt-1 p-1">
              {errors[plugin.name]}
            </div>
          )}
          <small className="humanize form-text text-muted" />
        </td>
      </React.Fragment>
    );
    return rows;
  }

  handleData() {
    let { plugin } = this.state;
    let actualGroup = '';
    let sameGroup = [];
    let allOfIt = [];
    filter(
      plugin.definitions,
      plugin => plugin.name !== 'name' && plugin.name !== 'connector.class'
    ).forEach(definition => {
      if (definition.group !== actualGroup) {
        if (actualGroup === '') {
          actualGroup = definition.group;
          sameGroup = [definition];
        } else {
          allOfIt.push(this.handleGroup(sameGroup));
          sameGroup = [definition];
          actualGroup = definition.group;
        }
      } else {
        sameGroup.push(definition);
      }
    });
    allOfIt.push(this.handleGroup(sameGroup));
    this.setState({ display: allOfIt });
    return allOfIt;
  }

  handleGroup(group) {
    let { formData } = this.state;
    let groupDisplay = [
      <tr key={0} className="bg-primary">
        <td colSpan="3">{group[0].group}</td>
      </tr>
    ];

    if (formData['transformsprops'] === undefined) {
      formData['transformsprops'] = '{}';
    }

    group.forEach(element => {
      const rows = this.renderTableRows(element);
      const errors = [];

      groupDisplay.push(<tr>{rows}</tr>);
      if (element.name === 'transforms') {
        const errorMessage = this.validateProperty({
          name: 'transformsprops',
          value: formData['transformsprops']
        });
        if (errorMessage) {
          errors['transformsprops'] = errorMessage;
        }
        let transform = (
          <React.Fragment>
            <td>
              <code>Transforms additional properties</code>
              <small className="form-text text-muted">
                {`Json object to be added to configurations. example:
                  {
                      "transforms.createKey.type":"org.apache.kafka.connect.transforms.ValueToKey",
                      "transforms.createKey.fields":"c1",
                      "transforms.extractInt.type":"org.apache.kafka.connect.transforms.ExtractField$Key",
                      "transforms.extractInt.field":"c1"
                  }`}
              </small>
            </td>
            <td>
              <AceEditor
                mode="json"
                id={'transformsprops'}
                theme="merbivore_soft"
                value={formData['transformsprops']}
                onChange={value => {
                  let { formData } = this.state;
                  const errors = { ...this.state.errors };
                  const errorMessage = this.validateProperty({ name: 'transformsprops', value });
                  if (errorMessage) {
                    errors['transformsprops'] = errorMessage;
                  } else {
                    delete errors['transformsprops'];
                  }
                  formData['transformsprops'] = value;
                  this.handleData();
                  this.setState({ formData });
                }}
                name="UNIQUE_ID_OF_DIV"
                editorProps={{ $blockScrolling: true }}
                style={{ width: '100%', minHeight: '25vh' }}
              />
              {errors['transformsprops'] && (
                <div id="input-error" className="alert alert-danger mt-1 p-1">
                  {errors['transformsprops']}
                </div>
              )}
            </td>
          </React.Fragment>
        );
        groupDisplay.push(transform);
      }
    });
    return groupDisplay;
  }

  renderForm() {
    let plugin = this.getPlugin();
    if (plugin.definitions) {
      this.setState({ plugin }, () => {
        this.handleSchema(
          filter(
            plugin.definitions,
            plugin => plugin.name !== 'name' && plugin.name !== 'connector.class'
          )
        );
        this.handleData();
      });
    }
  }

  getPlugin() {
    return this.state.plugins.find(plugin => this.state.selectedType === plugin.className);
  }

  validate = () => {
    const { error } = Joi.validate(this.state.formData, this.schema);
    if (!error) return null;
    const errors = {};
    for (let item of error.details) {
      if (Object.keys(this.schema).find(el => el === item.path[0])) {
        errors[item.path[0]] = item.message;
      }
    }
    return errors;
  };

  validateProperty = ({ name, value }) => {
    const obj = { [name]: value };
    const schema = { [name]: this.schema[name] };
    const { error } = Joi.validate(obj, schema);

    return error ? error.details[0].message : null;
  };

  renderDropdown() {
    const label = 'Types';
    let items = [{ _id: '', name: '' }];
    this.state.plugins.forEach(plugin => {
      let name = [plugin.className, ' [', plugin.version, ']'];
      items.push({ _id: plugin.className, name: name });
    });
    return (
      <Select
        name={'selectedType'}
        selectClass={'col-sm-10'}
        value={this.state.selectedType}
        label={label}
        items={items}
        onChange={value => {
          this.onTypeChange(value.target.value);
        }}
      />
    );
  }

  async doSubmit() {
    const { clusterId, connectId, formData } = this.state;
    let body = {
      clusterId,
      connectId,
      name: formData.subject
    };
    let configs = {};
    Object.keys(formData).forEach(key => {
      if (
        key !== 'subject' &&
        key !== 'transformsprops' &&
        key !== 'type' &&
        key !== 'name' &&
        formData[key] !== ''
      ) {
        configs[`${key}`] = formData[key];
      } else if (key === 'type') {
        configs['connector.class'] = formData[key];
      }
    });

    const transformsValue = JSON.parse(formData.transformsprops);
    Object.keys(transformsValue).forEach(key => {
      configs[key] = transformsValue[key];
    });

    body.configs = configs;

    this.postApi(uriCreateConnect(clusterId, connectId), body).then(() => {
      this.props.history.push({
        pathname: `/ui/${clusterId}/connect/${connectId}`
      });

      toast.success(`${`Connection '${formData.subject}' was created successfully`}`);
    });
  }

  render() {
    const { formData, selectedType } = this.state;
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
          <Header title={'Create a definition'} history={history} />
          {this.renderDropdown()}
          {selectedType.length > 0 && (
            <React.Fragment>
              <div className="form-group row">
                <label className="col-sm-2 col-form-label">{'Name'}</label>

                <div className="col-sm-10">
                  <input
                    className="form-control"
                    name="subject"
                    id="name"
                    value={formData['subject']}
                    onChange={({ currentTarget: input }) => {
                      let { formData } = this.state;
                      const errors = { ...this.state.errors };
                      const errorMessage = this.validateProperty(input);
                      if (errorMessage) {
                        errors[input.name] = errorMessage;
                      } else {
                        delete errors[input.name];
                      }
                      formData['subject'] = input.value;
                      this.setState({ formData });
                    }}
                    placeholder="Subject"
                  />
                </div>
              </div>
              <div className="table-responsive">
                <table className="table table-bordered table-striped mb-0 khq-form-config">
                  <thead className="thead-dark">
                    <tr>
                      <th style={{ width: '50%' }}>Name</th>
                      <th>Value</th>
                    </tr>
                  </thead>
                  <tbody>{this.state.display}</tbody>
                </table>
              </div>

              <div className="khq-submit button-footer" style={{ marginRight: 0 }}>
                <aside>
                  <button type={'submit'} className="btn btn-primary" disabled={this.validate()}>
                    Create
                  </button>
                </aside>
              </div>
            </React.Fragment>
          )}
        </form>
      </div>
    );
  }
}

export default withRouter(ConnectCreate);
