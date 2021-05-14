import React from 'react';
import Joi from 'joi-browser';
import './styles.scss';
import {
  uriConnectDefinitionConfigs,
  uriConnectPlugin,
  uriUpdateDefinition
} from '../../../../utils/endpoints';
import constants from '../../../../utils/constants';
import Form from '../../../../components/Form/Form';
import AceEditor from 'react-ace';
import _ from 'lodash';
import 'ace-builds/webpack-resolver';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-merbivore_soft';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

class ConnectConfigs extends Form {
  state = {
    clusterId: this.props.match.params.clusterId,
    connectId: this.props.match.params.connectId,
    definitionId: this.props.match.params.definitionId,
    formData: {},
    errors: {},
    configs: {},
    plugin: {},
    config: {},
    selectedType: '',
    display: '',
    roles: JSON.parse(sessionStorage.getItem('roles'))
  };

  schema = {};
  componentDidMount() {
    this.getConfigs();
  }

  async getConfigs() {
    const { connectId, clusterId, definitionId } = this.state;
    let configs = [];

    configs = await this.getApi(uriConnectDefinitionConfigs(clusterId, connectId, definitionId));
    this.setState({ configs: configs.data }, () => {
      const pluginId = this.state.configs['connector.class'];
      this.getPlugin(pluginId);
    });
  }

  async getPlugin(pluginId) {
    const { connectId, clusterId } = this.state;
    let plugin = {};

    plugin = await this.getApi(uriConnectPlugin(clusterId, connectId, pluginId));
    this.setState({ plugin: plugin.data }, () => {
      this.renderForm();
    });
  }

  handleSchema = definitions => {
    this.schema = {};
    let { formData } = { ...this.state };
    formData.type = this.getConfigValue('connector.class');
    this.schema['type'] = Joi.string().required();
    formData.name = this.getConfigValue('name');
    this.schema['name'] = Joi.string().required();

    definitions.forEach(definition => {
      formData[definition.name] = this.getConfigValue(definition.name);
      this.schema[definition.name] = this.handleDefinition(definition);
      if (definition.name === 'transforms') {
        formData['transformsprops'] = this.getTransformAdditionalProperties() || '{}';
        this.schema['transformsprops'] = Joi.object().required();
      }
    });
    this.setState({ formData });
  };

  getConfigValue = name => {
    const { configs } = this.state;
    const existingConfig = Object.keys(configs).find(configKey => configKey === name);

    return existingConfig ? configs[existingConfig] : '';
  };

  getTransformAdditionalProperties() {
    const { configs } = this.state;
    const filtered = Object.keys(configs).filter(configKey => configKey.startsWith('transforms.'))
        .reduce((obj, configKey) => {
          obj[configKey] = configs[configKey];
          return obj;
        }, {});
    return JSON.stringify(filtered, null, 2);
  };


  handleDefinition = definition => {
    let def = '';
    if (definition.required) {
      switch (definition.type) {
        case constants.TYPES.LONG:
        case constants.TYPES.INT:
        case constants.TYPES.DOUBLE:
        case constants.TYPES.SHORT:
          def = Joi.number().required();
          break;
        case constants.TYPES.PASSWORD:
          def = Joi.string().required();
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
    return def;
  };

  renderTableRows(plugin) {
    let rows = '';
    let title = '';
    let { formData } = this.state;
    const errors = [];
    const roles = this.state.roles || {};
    const errorMessage = this.validateProperty({ name: plugin.name, value: formData[plugin.name] });
    if (errorMessage) {
      errors[plugin.name] = errorMessage;
    }

    switch (plugin.importance) {
      case 'HIGH':
        title = (
          <span>
            {plugin.displayName}{' '}
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
            {plugin.displayName}{' '}
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

    let documentation = <small className="form-text text-muted">{plugin.documentation}</small>;

    rows = (
      <React.Fragment key={plugin.name}>
        <td>
          {title}
          <br />
          {name}
          <br />
          {required}
          <br />
          {documentation}
        </td>
        <td>
          <input
            type="text"
            className="form-control"
            value={formData[plugin.name]}
            name={plugin.name}
            disabled={plugin.name === 'name' || plugin.name === 'connector.class' || !(roles.connect && roles.connect['connect/update']) }
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

  handleData = () => {
    let { plugin } = this.state;
    let actualGroup = '';
    let sameGroup = [];
    let allOfIt = [];
    _(plugin.definitions)
      .filter(plugin => plugin.name !== 'name' && plugin.name !== 'connector.class')
      .value()
      .forEach(definition => {
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
  };

  handleGroup(group) {
    let { formData } = this.state;
    let groupDisplay = [
      <tr className="bg-primary">
        <td colSpan="3">{group[0].group}</td>
      </tr>
    ];

    group.forEach(element => {
      const rows = this.renderTableRows(element);
      const errors = [];
      const roles = this.state.roles || {};

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
                readOnly={!(roles.connect && roles.connect['connect/update'])}
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

  renderForm = () => {
    const { plugin } = this.state;
    this.handleSchema(plugin.definitions);
    this.handleData();
  };

  async doSubmit() {
    const { clusterId, connectId, definitionId, formData } = this.state;
    let body = {
      name: formData.name
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

    const { history } = this.props;

    await this.postApi(uriUpdateDefinition(clusterId, connectId, definitionId), body);

    history.push({
      pathname: `/ui/${clusterId}/connect/${connectId}`,
    });

    toast.success(`${`Definition '${formData.name}' is updated`}`);
  }

  render() {
    const { plugin, display } = this.state;
    const { name } = this.state.formData;
    const roles = this.state.roles || {};
    return (
      <div>
        <form
          encType="multipart/form-data"
          className="khq-form khq-form-config"
          onSubmit={e => this.handleSubmit(e)}
        >
          {plugin && (
            <React.Fragment>
              <div className="form-group row">
                <label className="col-sm-2 col-form-label">Type</label>
                <div className="col-sm-10">
                  <select disabled className="form-control" name="type" id="type">
                    <option>{plugin.shortClassName}</option>
                  </select>
                </div>
              </div>
              <div className="form-group row">
                <label className="col-sm-2 col-form-label">{'Name'}</label>
                <div className="col-sm-10">
                  <input
                    className="form-control"
                    name="name"
                    id="name"
                    value={name}
                    disabled
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
                  <tbody>{display}</tbody>
                </table>
              </div>
              {roles.connect && roles.connect['connect/update'] && (
                <div style={{ left: 0, width: '100%' }} className="khq-submit">
                  <button
                    type={'submit'}
                    className="btn btn-primary"
                    style={{ marginRight: '2%' }}
                    disabled={this.validate()}
                  >
                    Update
                  </button>
                </div>
              )}
            </React.Fragment>
          )}
        </form>
      </div>
    );
  }
}

export default ConnectConfigs;
