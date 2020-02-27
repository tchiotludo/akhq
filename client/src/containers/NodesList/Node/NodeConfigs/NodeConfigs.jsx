import React, { Component } from 'react';
import Header from '../../../Header/Header';
import { get, post } from '../../../../utils/api';
import { uriNodesConfigs, uriNodesUpdateConfigs } from '../../../../utils/endpoints';
import Table from '../../../../components/Table';
import Form from '../../../../components/Form/Form';
import converters from '../../../../utils/converters';
import _ from 'lodash';
import Joi from 'joi-browser';

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
    configs: []
  };

  schema = {};

  componentDidMount() {
    this.getNodesConfig();
  }

  async getNodesConfig() {
    let configs = [];
    const { selectedCluster, selectedNode } = this.state;

    try {
      configs = await get(uriNodesConfigs(selectedCluster, selectedNode));
      this.handleData(configs.data);
    } catch (err) {
      console.error('Error:', err);
    }
  }

  handleData(configs) {
    configs.map(config => {
      this.createValidationSchema(config);
    });

    let tableNodes = configs.map(config => {
      // this.setState({
      //   formData: {
      //     [config.name]: isNaN(+config.value) ? config.value : +config.value
      //   }
      // });
      return {
        id: config.name,
        nameAndDescription: this.handleNameAndDescription(config.name, config.description),
        value: this.getInput(
          this.state.formData[config.name],
          config.name,
          config.readOnly,
          config.dataType
        ),
        typeAndSensitive: this.handleTypeAndSensitive(config.type, config.sensitive)
      };
    });
    this.setState({ data: tableNodes, configs });
  }

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
          nameAndDescription: this.handleNameAndDescription(config.name, config.description),
          value: this.getInput(
            this.state.formData[config.name],
            config.name,
            config.readOnly,
            config.dataType
          ),
          typeAndSensitive: this.handleTypeAndSensitive(config.type, config.sensitive)
        };
      }
      return row;
    });

    this.setState({ data: newData });
  }

  async doSubmit() {
    const { selectedCluster, selectedNode, changedConfigs } = this.state;
    try {
      await post(uriNodesUpdateConfigs(), {
        clusterId: selectedCluster,
        nodeId: selectedNode,
        configs: changedConfigs
      });

      this.setState({ state: this.state });
    } catch (err) {
      console.error('Error:', err);
    }
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
      <a className="text-secondary" data-toggle="tooltip" title={description}>
        <i className="fa fa-question-circle" aria-hidden="true"></i>
      </a>
    ) : (
      ''
    );
    return (
      <div className="name-color">
        {name} {descript}
      </div>
    );
  }

  renderTabs(tabName, isActive) {
    const active = isActive ? 'active' : '';
    return (
      <li className="nav-item">
        <a className={`nav-link ${active}`} href="#" role="tab">
          {tabName}
        </a>
      </li>
    );
  }

  render() {
    const { data, selectedNode, selectedCluster } = this.state;
    return (
      <form
        encType="multipart/form-data"
        className="khq-form mb-0"
        onSubmit={() => this.handleSubmit()}
      >
        <div>
          <Table
            colNames={['Name', 'Value', 'Type']}
            toPresent={['nameAndDescription', 'value', 'typeAndSensitive']}
            data={data}
          />
          {this.renderButton('Update configs', this.handleSubmit, undefined, 'submit')}
        </div>
      </form>
    );
  }
}

export default NodeConfigs;
