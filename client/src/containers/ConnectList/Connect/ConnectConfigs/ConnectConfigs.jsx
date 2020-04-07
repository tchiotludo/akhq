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

class ConnectConfigs extends Form {
  state = {
    clusterId: this.props.match.params.clusterId,
    connectId: this.props.match.params.connectId,
    definitionId: this.props.match.params.definitionId,
    formData: {},
    errors: {},
    configs: {},
    selectedType: '',
    display: ''
  };

  schema = {};

  componentDidMount() {
    this.getConfigs();
  }

  async getConfigs() {
    const { connectId, clusterId, definitionId } = this.state;
    let configs = {};
    const { history } = this.props;
    history.push({
      loading: true
    });
    try {
      configs = await get(uriConnectDefinitionConfigs(clusterId, connectId, definitionId));
      this.setState({ configs: configs.data });
    } catch (err) {
      history.replace('/error', { errorData: err });
    } finally {
      history.push({
        loading: false
      });
    }
  }

  handleShema(definitions) {
    let { formData } = this.state;
    this.schema = {};
    definitions.map(definition => {
      // formData[definition.name] = '';
      formData[definition.name] = this.getConfigValue(definition.name);
    });

    this.setState({ formData }, () => {
      definitions.map(definition => {
        let config = this.handleDefinition(definition);
        this.schema[definition.name] = config;
      });
    });
  }

  getConfigValue = name => {
    const { configs } = this.state.configs;
    const existingConfig = Object.keys(configs).find(configKey => configKey === name);

    return existingConfig ? configs[existingConfig] : '';
  };

  handleDefinition(definition) {
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
          def = Joi.number();
          break;

        case constants.TYPES.BOOLEAN:
          def = Joi.boolean();
          break;
        default:
          def = Joi.string();
          break;
      }
      return def;
    }
  }

  renderLabel(plugin) {
    let label = '';
    let title = '';
    switch (plugin.importance) {
      case 'HIGH':
        title = (
          <React.Fragment>
            {plugin.displayName}
            {''}
            <i
              class="fa fa-exclamation text-danger"
              style={{ marginleft: '2%' }}
              aria-hidden="true"
            ></i>
          </React.Fragment>
        );
        break;
      case 'MEDIUM':
        title = (
          <React.Fragment>
            {plugin.displayName}
            <i class="fa fa-info text-warning" style={{ marginleft: '2%' }} aria-hidden="true"></i>
          </React.Fragment>
        );
        break;
      default:
        title = <React.Fragment>{plugin.displayName}</React.Fragment>;
        break;
    }
    let name = <code>{plugin.name}</code>;
    let documentation = <small class="form-text text-muted">{plugin.documentation}</small>;
    label = (
      <span>
        {title}
        {name}
        {documentation}
      </span>
    );
    return label;
  }

  handleData(plugin) {
    console.log(plugin);
    let actualGroup = '';
    let sameGroup = [];
    let allOfIt = [];
    plugin.definitions.map(definition => {
      if (definition.group !== actualGroup) {
        if (sameGroup.length === 0) {
          actualGroup = definition.group;
        } else {
          allOfIt.push(this.handleGroup(sameGroup));
          sameGroup = [definition];
          actualGroup = definition.group;
        }
      } else {
        sameGroup.push(definition);
      }
    });

    console.log(allOfIt);

    return allOfIt;
  }

  handleGroup(group) {
    let groupDisplay = [
      <span style={{ backgroundColor: 'Blue', width: '100%' }}>{group[0].group}</span>
    ];
    group.map(element => {
      const label = this.renderLabel(element);
      const name = element.name;
      //No style false, wrapper class false,
      groupDisplay.push(
        this.renderInput(
          name,
          label,
          undefined,
          'text',
          undefined,
          false,
          'connectFormWraper',
          'connectInputWrapper'
        )
      );
    });
    return groupDisplay;
  }

  renderForm() {
    const plugin = this.state.configs.plugin;
    this.handleShema(plugin.definitions);
    return this.handleData(plugin);
  }

  renderDropdown() {
    const className = this.state.configs.plugin.className;
    let names = [{ _id: className, name: className }];
    return this.renderSelect('type', 'Type', names, undefined, { disabled: true });
  }

  render() {
    const { configs } = this.state;

    return (
      <form
        encType="multipart/form-data"
        className="khq-form khq-form-config"
        onSubmit={() => this.doSubmit()}
      >
        {configs.plugin && (
          <React.Fragment>
            {this.renderDropdown()}
            {this.renderInput(
              'name',
              'Name',
              'Name',
              this.state.configs.plugin.shortClassName,
              undefined,
              false,
              undefined,
              undefined,
              { disabled: true }
            )}
            {this.renderForm()}
          </React.Fragment>
        )}
      </form>
    );
  }
}

export default ConnectConfigs;
