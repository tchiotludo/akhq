import React from 'react';
import { uriTopicsConfigs, uriTopicsUpdateConfigs } from '../../../../utils/endpoints';
import Table from '../../../../components/Table';
import Form from '../../../../components/Form/Form';
import converters from '../../../../utils/converters';
import Joi from 'joi-browser';
import { MILLI, BYTES, TEXT } from '../../../../utils/constants';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
class TopicConfigs extends Form {
  state = {
    host: '',
    port: '',
    data: [],
    selectedCluster: this.props.clusterId,
    selectedTopic: this.props.topicId,
    formData: {},
    changedConfigs: {},
    errors: {},
    configs: [],
    roles: JSON.parse(sessionStorage.getItem('roles')),
    loading: true
  };

  schema = {};

  componentDidMount() {
    this.getTopicsConfig();
  }

  async getTopicsConfig() {
    let configs = [];
    const { selectedCluster, selectedTopic } = this.state;

    configs = await this.getApi(uriTopicsConfigs(selectedCluster, selectedTopic));
    this.handleData(configs.data);
  }

  handleData(configs) {
    configs.forEach(config => {
      this.createValidationSchema(config);
    });

    let tableTopics = configs.map(config => {
      return {
        id: config.name,
        name: config.name,
        description: config.description,
        value: config.value,
        readOnly: config.readOnly,
        dataType: this.getConfigDataType(config.name),
        type: config.source,
        sensitive: config.sensitive
      };
    });
    this.setState({ data: tableTopics, configs, loading: false });
  }
  getConfigDataType = configName => {
    switch (configName.substring(configName.lastIndexOf('.'))) {
      case '.ms':
        return MILLI;
      case '.size':
        return BYTES;
      default:
        return TEXT;
    }
  };
  handleDataType(dataType, value) {
    switch (dataType) {
      case 'MILLI':
        return (
          <small className="humanize form-text text-muted">{converters.showTime(value)}</small>
        );
      case 'BYTES':
        return (
          <small className="humanize form-text text-muted">{converters.showBytes(value)}</small>
        );
      default:
        return <small></small>;
    }
  }

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
          value: config.value,
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
    const { selectedCluster, selectedTopic, changedConfigs } = this.state;

    await this.postApi(uriTopicsUpdateConfigs(selectedCluster, selectedTopic), {
      clusterId: selectedCluster,
      topicId: selectedTopic,
      configs: changedConfigs
    });

    this.setState({ state: this.state }, () => {
      toast.success(`Topic configs '${selectedTopic}' updated successfully`);
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
            name={name}
            placeholder="default"
          />
        ) : (
          <input
            type="number"
            onChange={value => this.onChange(value)}
            className="form-control"
            autoComplete="off"
            value={value}
            name={name}
            placeholder="default"
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
      <span className="name-color">
        <code>{name}</code> {descript}
      </span>
    );
  }

  renderTabs(tabName, isActive) {
    const active = isActive ? 'active' : '';
    return (
      <li className="nav-item">
        <div className={`nav-link ${active}`} href="#" role="tab">
          {tabName}
        </div>
      </li>
    );
  }

  render() {
    const { data, loading } = this.state;
    const roles = this.state.roles || {};
    const isUpdatable = roles.TOPIC && roles.TOPIC.includes('ALTER_CONFIG');
    return (
      <form
        encType="multipart/form-data"
        className="khq-form mb-0"
        onSubmit={() => this.handleSubmit()}
      >
        <div>
          <Table
            loading={loading}
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
                readOnly: !isUpdatable,
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
            noContent={'No acl for configs for current kafka user.'}
          />
          {isUpdatable && !this.props.internal ? (
            <aside>
              {this.renderButton('Update configs', this.handleSubmit, undefined, 'submit')}
            </aside>
          ) : null}
        </div>
      </form>
    );
  }
}

export default TopicConfigs;
