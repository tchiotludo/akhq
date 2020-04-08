import React, { Component } from 'react';
import Joi from 'joi-browser';
import Table from '../../../../components/Table/Table';
import './styles.scss';
import { get } from '../../../../utils/api';
import { uriConnectDefinitionConfigs } from '../../../../utils/endpoints';
import Dropdown from 'react-bootstrap/Dropdown';
import constants from '../../../../utils/constants';
import Form from '../../../../components/Form/Form';
import { red } from '@material-ui/core/colors';
import { Simulate } from 'react-dom/test-utils';
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

  handleShema = definitions => {
    this.schema = {};
    let { formData } = { ...this.state };
    formData['connector.class'] = this.state.configs.configs['connector.class'];
    this.schema['connector.class'] = Joi.string().required();
    formData.name = this.state.configs.configs.name;
    this.schema['name'] = Joi.string().required();

    definitions.map(definition => {
      formData[definition.name] = this.getConfigValue(definition.name);
    });

    this.setState({ formData }, () => {
      definitions.map(definition => {
        let config = this.handleDefinition(definition);
        formData[definition.name] = '';
        this.schema[definition.name] = config;
        if (definition.name === 'transforms') {
          formData['transformsprops'] = {};
          this.schema['transformsprops'] = Joi.object().required();
        }
      });
    });
  };

  getConfigValue = name => {
    const { configs } = this.state.configs;
    const existingConfig = Object.keys(configs).find(configKey => configKey === name);

    return existingConfig ? configs[existingConfig] : '';
  };

  handleDefinition = definition => {
    let def = '';
    if (definition.required === 'true') {
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

  renderTableRows = plugin => {
    let rows = '';
    let title = '';
    let { formData, errors } = { ...this.state };
    switch (plugin.importance) {
      case 'HIGH':
        title =
          plugin.displayName !== '' ? (
            <span>
              {plugin.displayName}{' '}
              <i
                class="fa fa-exclamation text-danger"
                style={{ marginleft: '2%' }}
                aria-hidden="true"
              ></i>
            </span>
          ) : (
            ''
          );
        break;
      case 'MEDIUM':
        title =
          plugin.displayName !== '' ? (
            <span>
              {plugin.displayName}{' '}
              <i
                class="fa fa-info text-warning"
                style={{ marginleft: '2%' }}
                aria-hidden="true"
              ></i>
            </span>
          ) : (
            ''
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
            name={plugin.name}
            value={formData[plugin.name]}
            placeholder={plugin.defaultValue > 0 ? plugin.defaultValue : ''}
            onChange={({ currentTarget: input }) => {
              let { formData } = this.state;
              const errors = { ...this.state.errors };
              const errorMessage = this.validateProperty(input);
              if (errorMessage) {
                errors[input.name] = errorMessage;
              } else {
                delete errors[input.name];
              }
              formData[plugin.name] = input.value;
              this.handleData();
              this.setState({ formData });
            }}
          >
            {formData[plugin.required]}
          </input>

          {errors[name] && (
            <div id="input-error" className="alert alert-danger mt-1 p-1">
              {errors[name]}
            </div>
          )}
          <small className="humanize form-text text-muted"></small>
        </td>
      </React.Fragment>
    );
    return rows;
  };

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

  handleGroup = group => {
    let { formData } = this.state;
    let groupDisplay = [
      <tr className="bg-primary">
        <td colSpan="3">{group[0].group}</td>
      </tr>
    ];

    group.map(element => {
      const rows = this.renderTableRows(element);
      const name = element.name;
      groupDisplay.push(<tr>{rows}</tr>);
      if (element.name === 'transforms') {
        let transform = (
          <tr>
            <td>
              <code>Transforms additional properties</code>
              <small class="form-text text-muted">
                {`
                                            Json object to be added to configurations. example:
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
                value={JSON.stringify(formData['transformsprops'])}
                onChange={value => {
                  let { formData } = this.state;
                  formData['transformsprops'] = value;
                  this.handleData();
                  this.setState({ formData });
                }}
                name="UNIQUE_ID_OF_DIV"
                editorProps={{ $blockScrolling: true }}
                style={{ width: '100%', minHeight: '25vh' }}
              />
            </td>
          </tr>
        );
        groupDisplay.push(transform);
      }
    });
    return groupDisplay;
  };

  renderForm = () => {
    const { plugin } = this.state.configs;
    this.handleShema(plugin.definitions);
    this.handleData();
  };

  renderDropdown = () => {
    const shortClassName = this.state.configs.plugin.shortClassName;
    let names = [{ _id: shortClassName, name: shortClassName }];
    return this.renderSelect('type', 'Type', names, undefined, { disabled: true });
  };

  doSubmit = () => {
    console.log(this.state.formData);
  };

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
