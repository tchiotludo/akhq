import React, { Component } from 'react';
import Header from '../../../Header/Header';
import { get } from '../../../../utils/api';
import { uriNodesConfigs } from '../../../../utils/endpoints';
import Table from '../../../../components/Table';
import converters from '../../../../utils/converters';

class NodeLogs extends Component {
  state = {
    host: '',
    port: '',
    data: [],
    selectedCluster: this.props.clusterId,
    selectedNode: this.props.nodeId
  };

  componentDidMount() {
    this.getNodesLogs();
    console.log('props: ',this.props);
  }

  async getNodesLogs() {
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
          colNames={['Broker', 'Topic', 'Partition','Size','OffsetLag']}
          toPresent={['nameAndDescription', 'value', 'typeAndSensitive']}
          data={data}
        />
      </div>
    );
  }
}

export default NodeLogs;
