import React, { Component } from 'react';
import Header from '../Header';
import { get } from '../../services/api';
import { uriNodesConfigs } from '../../services/endpoints';
import Table from '../../components/Table';
import './styles.scss';
import converters from '../../utils/converters';

class NodeDetails extends Component {
  state = {
    host: '',
    port: '',
    data: [],
    selectedCluster: this.props.match.params.clusterId,
    selectedNode: this.props.match.params.nodeId
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
      console.log('Error:', err);
    }
  }

  handleData(configs) {
    console.log(configs);
    let tableNodes = configs.map(config => {
      return {
        nameAndDescription: this.handleNameAndDescription(config.name, config.description),
        value: this.getInput(config.value, config.name, config.readOnly),
        typeAndSensitive: this.handleTypeAndSensitive(config.type, config.sensitive)
      };
    });
    this.setState({ data: tableNodes });
  }

  getInput(value, name, readOnly) {
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
        <small className="humanize form-text text-muted"></small>
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
      <div>
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
    const { data, selectedNode: nodeId, selectedCluster: clusterId } = this.state;
    console.log(1500, converters.showBytes(1500));
    console.log(15000, converters.showBytes(150000000));
    console.log(150555, converters.showBytes(15055500000));
    console.log(1500, converters.showTime(1500));
    console.log(15000, converters.showTime(15000));
    console.log(150555, converters.showTime(150555));
    console.log(604800000, converters.showTime(604800000));
    console.log(86400000, converters.showTime(86400000));
    console.log(150000000000, converters.showTime(150000000000));
    console.log(150000000000000, converters.showTime(150000000000000));
    return (
      <div id="content" style={{ height: '100%' }}>
        <Header title={`Node: ${nodeId}`} />
        <ul className="nav nav-tabs" role="tablist">
          {this.renderTabs('Configs', true)}
          {this.renderTabs('Logs', false)}
        </ul>
        <Table
          colNames={['Name', 'Value', 'Type']}
          toPresent={['nameAndDescription', 'value', 'typeAndSensitive']}
          data={data}
        />
      </div>
    );
  }
}

export default NodeDetails;
