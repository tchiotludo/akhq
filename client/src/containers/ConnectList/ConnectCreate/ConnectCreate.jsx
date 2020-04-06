import React, { Component } from 'react';
import Joi from 'joi-browser';
import './styles.scss';
import { get } from '../../../utils/api';
import { uriConnectPlugins } from '../../../utils/endpoints';
import Header from '../../Header/Header';
import constants from '../../../utils/constants';
import Select from '../../../components/Form/Select';
import Form from '../../../components/Form/Form';
import { red } from '@material-ui/core/colors';
class ConnectCreate extends Form {
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

  async getPlugins() {
    const { connectId, clusterId } = this.state;
    let plugins = [];
    const { history } = this.props;
    history.push({
      loading: true
    });
    try {
      plugins = await get(uriConnectPlugins(clusterId, connectId));
      this.setState({ clusterId, connectId, plugins: plugins.data });
    } catch (err) {
      history.replace('/error', { errorData: err });
    } finally {
      history.push({
        loading: false
      });
    }
  }

  onTypeChange = value => {
    this.setState({ selectedType: value }, () => {
      this.renderForm();
    });
  };

  handleShema(definitions) {
    this.schema = {};
    let { formData } = { ...this.state };
    formData.type = this.state.selectedType;
    this.schema['type'] = Joi.string().required();
    formData.subject = '';
    this.schema['subject'] = Joi.string().required();
    definitions.map(definition => {
      let config = this.handleDefinition(definition);
      formData[definition.name] = '';
      this.schema[definition.name] = config;
    });
    this.setState({ formData });
  }

  handleDefinition(definition) {
    let def = '';
    console.log(definition.required);
    console.log(definition);
    if (definition.required) {
      console.log('Required', definition);
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
      console.log('NOT Required', definition);
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
    }
    return def;
  }

  renderTableRows(plugin) {
    let rows = '';
    let title = '';
    let { formData, errors } = { ...this.state };
    switch (plugin.importance) {
      case 'HIGH':
        title = (
          <span>
            {plugin.displayName}
            {''}
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
            {plugin.displayName}
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
            placeholder={plugin.defaultValue > 0 ? plugin.defaultValue : ''}
            onChange={({ currentTarget: input }) => {
              let { formData } = this.state;
              console.log(formData[plugin.name]);
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
  }

  handleData() {
    let { plugin } = this.state;
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

    this.setState({ display: allOfIt });
    return allOfIt;
  }

  handleGroup(group) {
    let groupDisplay = [
      <tr class="bg-primary">
        <td colspan="3">{group[0].group}</td>
      </tr>
    ];
    group.map(element => {
      const rows = this.renderTableRows(element);
      const name = element.name;
      groupDisplay.push(<tr>{rows}</tr>);
    });
    return groupDisplay;
  }

  renderForm() {
    let { plugin } = this.state;
    plugin = this.getPlugin();
    this.setState({ plugin }, () => {
      this.handleShema(plugin.definitions);
      this.handleData();
    });
  }

  getPlugin() {
    return this.state.plugins.find(plugin => {
      if (this.state.selectedType === plugin.className) {
        return plugin;
      }
    });
  }

  renderDropdown() {
    const label = 'Types';
    let items = [{ _id: '', name: '' }];
    this.state.plugins.map(plugin => {
      let name = [plugin.className, ' [', plugin.version, ']'];
      items.push({ _id: plugin.className, name: name });
    });
    return (
      <Select
        name={'selectedType'}
        value={this.state.selectedType}
        label={label}
        items={items}
        onChange={value => {
          this.onTypeChange(value.target.value);
        }}
      />
    );
  }
  doSubmit() {
    console.log(this.state.formData);
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
          {selectedType.length > 0 && (
            <React.Fragment>
              <div className="form-group row">
                <label className="col-sm-2 col-form-label">{'Name'}</label>

                <div className="col-sm-10">
                  <input className="form-control" name="name" id="name" placeholder="Subject" />
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
              {this.renderButton(
                'Create',
                () => {
                  this.doSubmit();
                },
                undefined,
                'button'
              )}
            </React.Fragment>
          )}
        </form>
      </div>
    );
  }
}

export default ConnectCreate;
