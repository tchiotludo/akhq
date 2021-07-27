import React from 'react';
import { uriNodesConfigs, uriNodesUpdateConfigs } from '../../../../utils/endpoints';
import { BYTES, MILLI, TEXT } from '../../../../utils/constants';
import Table from '../../../../components/Table';
import Form from '../../../../components/Form/Form';
import converters from '../../../../utils/converters';
import Joi from 'joi-browser';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
class NodeConfigs extends Form {
  state = {
    host: '',
    port: '',
    data: [],
    selectedCluster: this.props.clusterId,
    selectedNode: this.props.nodeId,
    formData: {},
    changedConfigs: {},
    errors: {},
    configs: [],
    roles: JSON.parse(sessionStorage.getItem('roles')),
    loading: true
  };

  schema = {};

  componentDidMount() {
    this.getNodesConfig();
  }

  async getNodesConfig() {
    const { selectedCluster, selectedNode } = this.state;

    let configs = await this.getApi(uriNodesConfigs(selectedCluster, selectedNode));
    this.handleData(configs.data);
  }

  handleData(configs) {
    configs.forEach(config => {
      this.createValidationSchema(config);
    });

    let tableNodes = configs.map(config => {
      return {
        id: config.name,
        name: config.name,
        description: config.description,
        readOnly: config.readOnly,
        dataType: this.getConfigDataType(config.name),
        type: config.source,
        sensitive: config.sensitive
      };
    });
    this.setState({ data: tableNodes, configs, loading: false });
  }

  handleDataType(dataType, value) {
    switch (dataType) {
      case MILLI:
        return (
          <small className="humanize form-text text-muted">{converters.showTime(value)}</small>
        );
      case BYTES:
        return (
          <small className="humanize form-text text-muted">{converters.showBytes(value)}</small>
        );
      default:
        return <small></small>;
    }
  }

  getConfigDataType = configName => {
    if (configName === undefined) {
      return TEXT;
    }

    switch (configName.substring(configName.lastIndexOf('.'))) {
      case '.ms':
        return MILLI;
      case '.size':
        return BYTES;
      default:
        return TEXT;
    }
  };

  createValidationSchema(config) {
    let { formData } = this.state;
    let validation;
    if (isNaN(config.value)) {
      validation = Joi.any();
    } else {
      validation = Joi.any();
    }
    this.schema[config.name] = validation;

    formData[config.name] = isNaN(+config.value) ? config.value : +config.value;
    this.setState({ formData });
  }

  onChange({ currentTarget: input }) {
    let { data, configs } = this.state;
    let config = {};
    let newData = data.map(row => {
      if (row.id === input.name) {
        config = configs.find(config => config.name === input.name);
        let { formData, changedConfigs } = this.state;
        formData[input.name] = input.value;
        if (input.value === config.value) {
          delete changedConfigs[input.name];
        } else {
          changedConfigs[input.name] = input.value;
        }

        this.setState({ formData, changedConfigs });
        return {
          id: config.name,
          name: config.name,
          description: config.description,
          readOnly: config.readOnly,
          dataType: this.getConfigDataType(config.name),
          type: config.source,
          sensitive: config.sensitive
        };
      }
      return row;
    });

    this.setState({ data: newData });
  }

  async doSubmit() {
    const { selectedCluster, selectedNode, changedConfigs } = this.state;
    let { configs } = this.state;

    await this.postApi(uriNodesUpdateConfigs(selectedCluster, selectedNode), {
      configs: changedConfigs
    });

    this.setState({ state: this.state });
    toast.success(`Node configs '${selectedNode}' updated successfully.`);
    Object.keys(changedConfigs).forEach(key => {
      const changedConfig = changedConfigs[key];
      const configIndex = configs.findIndex(config => config.name === key);
      configs[configIndex].value = changedConfig;
    });
  }

  getInput(value, name, readOnly, dataType) {
    return (
      <div>
        {dataType === 'TEXT' ? (
          <input
            type="text"
            onChange={value => this.onChange(value)}
            className="form-control"
            autoComplete="off"
            value={value}
            readOnly={readOnly}
            name={name}
            placeholder="Default"
          />
        ) : (
          <input
            type="number"
            onChange={value => this.onChange(value)}
            className="form-control"
            autoComplete="off"
            value={value}
            readOnly={readOnly}
            name={name}
            placeholder="Default"
          />
        )}
        {this.handleDataType(dataType, value)}
      </div>
    );
  }

  handleTypeAndSensitive(configType, configSensitive) {
    const type = configType === 'DEFAULT_CONFIG' ? 'secondary' : 'warning';
    return (
      <div>
        <span className={'badge badge-' + type}> {configType}</span>
        {configSensitive ? (
          <i className="sensitive fa fa-exclamation-triangle text-danger" aria-hidden="true"></i>
        ) : (
          ''
        )}
      </div>
    );
  }

  handleNameAndDescription(name, description) {
    const descript = description ? (
      <span className="text-secondary" data-toggle="tooltip" title={description}>
        <i className="fa fa-question-circle" aria-hidden="true"></i>
      </span>
    ) : (
      ''
    );
    return (
      <div>
        <code>{name}</code> {descript}
      </div>
    );
  }

  renderTabs(tabName, isActive) {
    const active = isActive ? 'active' : '';
    return (
      <li className="nav-item">
        <div className={`nav-link ${active}`} role="tab">
          {tabName}
        </div>
      </li>
    );
  }

  render() {
    const { data, loading } = this.state;
    const roles = this.state.roles || {};
    return (
      <form
        encType="multipart/form-data"
        className="khq-form mb-0"
        onSubmit={() => this.handleSubmit()}
      >
        <div>
          <Table
            loading={loading}
            history={this.props.history}
            columns={[
              {
                id: 'nameAndDescription',
                accessor: 'nameAndDescription',
                colName: 'Name',
                type: 'text',
                cell: obj => {
                  return this.handleNameAndDescription(obj.name, obj.description);
                }
              },
              {
                id: 'value',
                accessor: 'value',
                colName: 'Value',
                type: 'text',
                cell: obj => {
                  return this.getInput(
                    this.state.formData[obj.name],
                    obj.name,
                    obj.readOnly,
                    obj.dataType
                  );
                }
              },
              {
                id: 'typeAndSensitive',
                accessor: 'typeAndSensitive',
                colName: 'Type',
                type: 'text',
                cell: obj => {
                  return this.handleTypeAndSensitive(obj.type, obj.sensitive);
                }
              }
            ]}
            data={data}
            updateData={data => {
              this.setState({ data });
            }}
          />
          {roles.node['node/config/update'] &&
            this.renderButton('Update configs', this.handleSubmit, undefined, 'submit')}
        </div>
      </form>
    );
  }
}

export default NodeConfigs;
