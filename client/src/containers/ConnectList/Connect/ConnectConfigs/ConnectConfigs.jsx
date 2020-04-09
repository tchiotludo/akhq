import React, { Component } from 'react';
import Joi from 'joi-browser';
import './styles.scss';
import { get, put } from '../../../../utils/api';
import { uriConnectDefinitionConfigs, uriUpdateDefinition } from '../../../../utils/endpoints';
import constants from '../../../../utils/constants';
import Form from '../../../../components/Form/Form';
import { red } from '@material-ui/core/colors';
import AceEditor from 'react-ace';

class ConnectConfigs extends Form {
  state = {
    clusterId: this.props.match.params.clusterId,
    connectId: this.props.match.params.connectId,
    definitionId: this.props.match.params.definitionId,
    formData: {},
    errors: {},
    configs: {},
    config: {},
    selectedType: '',
    display: ''
  };

  schema = {};
  componentDidMount() {
    this.getConfigs();
  }

  async getConfigs() {
    const { connectId, clusterId, definitionId } = this.state;
    let configs = [];
    const { history } = this.props;
    history.push({
      loading: true
    });
    try {
      configs = await get(uriConnectDefinitionConfigs(clusterId, connectId, definitionId));
      this.setState({ configs: configs.data }, () => {
        this.renderForm();
      });
    } catch (err) {
      history.replace('/error', { errorData: err });
    } finally {
      history.push({
        loading: false
      });
    }
  }

  handleSchema = definitions => {
    this.schema = {};
    let { formData } = { ...this.state };
    formData.type = this.getConfigValue('connector.class');
    this.schema['type'] = Joi.string().required();
    formData.name = this.getConfigValue('name');
    this.schema['name'] = Joi.string().required();

    definitions.map(definition => {
      formData[definition.name] = this.getConfigValue(definition.name);
      let config = this.handleDefinition(definition);
      this.schema[definition.name] = config;
      if (definition.name === 'transforms') {
        formData['transformsprops'] = '{}';
        this.schema['transformsprops'] = Joi.object().required();
      }
    });
    this.setState({ formData }, () => console.log(this.state.configs));
  };

  getConfigValue = name => {
    const { configs } = this.state.configs;
    const existingConfig = Object.keys(configs).find(configKey => configKey === name);

    return existingConfig ? configs[existingConfig] : '';
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
    return def;
  };

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
            {plugin.displayName}{' '}
            <i
              class="fa fa-exclamation text-danger"
              style={{ marginleft: '2%' }}
              aria-hidden="true"
            ></i>
          </span>
        );
        break;
      case 'MEDIUM':
        title = (
          <span>
            {plugin.displayName}{' '}
            <i class="fa fa-info text-warning" style={{ marginleft: '2%' }} aria-hidden="true"></i>
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

    let documentation = <small class="form-text text-muted">{plugin.documentation}</small>;

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
          <small className="humanize form-text text-muted"></small>
        </td>
      </React.Fragment>
    );
    return rows;
  }

  handleData = () => {
    let { plugin } = this.state.configs;
    let actualGroup = '';
    let sameGroup = [];
    let allOfIt = [];
    plugin.definitions.map(definition => {
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
      <tr class="bg-primary">
        <td colspan="3">{group[0].group}</td>
      </tr>
    ];

    group.map(element => {
      const rows = this.renderTableRows(element);
      const name = element.name;
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
              <small class="form-text text-muted">
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
                theme="dracula"
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

  renderForm = () => {
    const { plugin } = this.state.configs;
    this.handleSchema(plugin.definitions);
    this.handleData();
  };

  renderDropdown = () => {
    const shortClassName = this.state.configs.plugin.shortClassName;
    let names = [{ _id: shortClassName, name: shortClassName }];
    return this.renderSelect('type', 'Type', names, undefined, { disabled: true });
  };

  async doSubmit() {
    const { clusterId, connectId, formData, selectedType } = this.state;
    let body = {
      clusterId,
      connectId,
      name: formData.name,
      transformsValue: JSON.stringify(JSON.parse(formData.transformsprops))
    };
    let configs = {};
    Object.keys(formData).map(key => {
      if (key !== 'subject' && key !== 'transformsprops' && key !== 'type' && key !== 'name') {
        configs[`configs[${key}]`] = formData[key];
      } else if (key === 'type') {
        configs['configs[connector.class]'] = formData[key];
      }
    });
    body.configs = configs;

    const { history } = this.props;
    history.push({
      ...this.props.location,
      loading: true
    });
    try {
      await put(uriUpdateDefinition(), body);
      history.push({
        ...this.props.location,
        pathname: `/${clusterId}/connect/${connectId}`,
        showSuccessToast: true,
        successToastMessage: `${`Definition '${formData.name}' is updated`}`,
        loading: false
      });
    } catch (err) {
      history.push({
        ...this.props.location,
        showErrorToast: true,
        errorToastTitle: `${`Failed to update definition '${formData.name}'`}`,
        errorToastMessage: err.response.data.tle,
        loading: false
      });
    }
  }

  render() {
    const { configs, display } = this.state;
    const { name } = this.state.formData;

    return (
      <div>
        <form
          encType="multipart/form-data"
          className="khq-form khq-form-config"
          onSubmit={e => this.handleSubmit(e)}
        >
          {configs.plugin && (
            <React.Fragment>
              <div className="form-group row">
                <label className="col-sm-2 col-form-label">Type</label>
                <div className="col-sm-10">
                  <select disabled className="form-control" name="type" id="type">
                    <option>{this.state.configs.plugin.shortClassName}</option>
                  </select>
                </div>
              </div>
              <div className="form-group row">
                <label className="col-sm-2 col-form-label">{'Name'}</label>
                <div class="col-sm-10">
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
              <div className="khq-submit button-footer" style={{ marginRight: 0 }}>
                <button type={'submit'} className="btn btn-primary" disabled={this.validate()}>
                  Update
                </button>
              </div>
            </React.Fragment>
          )}
        </form>
      </div>
    );
  }
}

export default ConnectConfigs;
