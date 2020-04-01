import React, { Component } from 'react';
import Joi from 'joi-browser';
import Table from '../../../components/Table/Table';
import './styles.scss';
import { get } from '../../../utils/api';
import { uriConnectPlugins } from '../../../utils/endpoints';
import Header from '../../Header/Header';
import Dropdown from 'react-bootstrap/Dropdown';
import constants from '../../../utils/constants';
import Form from '../../../components/Form/Form';
import { red } from '@material-ui/core/colors';
import { Simulate } from 'react-dom/test-utils';

class ConnectCreate extends Form {
  state = {
    clusterId: this.props.match.params.clusterId,
    connectId: this.props.match.params.connectId,
    formData: {},
    errors: {},
    plugins: [],
    selectedType: '',
    display: ''
  };

  schema = {};
  componentDidMount() {
    this.getPlugins();
  }

  async getPlugins() {
    const { connectId, clusterId } = this.state;
    let plugins = [];
    const { history } = this.props;
    history.push({
      loading: true
    });
    try {
      plugins = await get(uriConnectPlugins(clusterId, connectId));
      console.log(plugins.data);
      this.setState({ clusterId, connectId, plugins: plugins.data });
    } catch (err) {
      history.replace('/error', { errorData: err });
    } finally {
      history.push({
        loading: false
      });
    }
  }

  onTypeChange = ({ currentTarget: input }) => {
    let value = input.value;
    this.setState({ selectedType: value }, () => {
      this.renderForm();
    });
  };

  handleShema(definitions) {
    this.schema = {};
    definitions.map(definition => {
      let config = this.handleDefinition(definition);
      this.schema[definition.name] = config;
    });
  }

  handleDefinition(definition) {
    let def = '';
    if (definition.required === true) {
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
    this.setState({ display: allOfIt });
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
    let plugin = this.getPlugin();
    console.log(plugin);
    this.handleShema(plugin.definitions);
    this.handleData(plugin);
  }

  getPlugin() {
    return this.state.plugins.find(plugin => {
      console.log(this.state.selectedType);
      console.log(plugin.className);
      if (this.state.selectedType === plugin.className) {
        console.log(plugin);
        return plugin;
      }
    });
  }

  renderDropdown() {
    let names = [{ _id: '', name: '' }];
    this.state.plugins.map(plugin => {
      names.push({ _id: plugin.className, name: plugin.className });
    });
    return this.renderSelect('type', 'Type', names, this.onTypeChange);
  }

  render() {
    const { clusterId, connectId, formData, selectedType } = this.state;
    const { history } = this.props;

    return (
      <div id="content">
        <form
          encType="multipart/form-data"
          className="khq-form khq-form-config"
          onSubmit={() => this.doSubmit()}
        >
          <Header title={'Create a definition'} />
          {this.renderDropdown()}
          {this.renderInput('name', 'Name', 'Name')}
          {selectedType.length > 0 && this.state.display}
        </form>
      </div>
    );
  }
}

export default ConnectCreate;
