import React, { Component } from 'react';
import Header from '../../../Header/Header';
import { get } from '../../../../utils/api';
import { uriNodesConfigs } from '../../../../utils/endpoints';
import Table from '../../../../components/Table';
import converters from '../../../../utils/converters';

class NodeConfigs extends Component {
  state = {
    host: '',
    port: '',
    data: [],
    selectedCluster: this.props.clusterId,
    selectedNode: this.props.nodeId
  };

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
    let tableNodes = configs.map(config => {
      return {
        nameAndDescription: this.handleNameAndDescription(config.name, config.description),
        value: this.getInput(config.value, config.name, config.readOnly, config.dataType),
        typeAndSensitive: this.handleTypeAndSensitive(config.type, config.sensitive)
      };
    });
    this.setState({ data: tableNodes });
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

  getInput(value, name, readOnly, dataType) {
    return (
      <div>
        <input
          type="text"
          onChange={console.log('done')}
          className="form-control"
          autoComplete="off"
          value={value}
          readOnly={readOnly}
        />
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
      <div>
        <Table
          colNames={['Name', 'Value', 'Type']}
          toPresent={['nameAndDescription', 'value', 'typeAndSensitive']}
          data={data}
        />
      </div>
    );
  }
}

export default NodeConfigs;
