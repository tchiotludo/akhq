import React, { Component } from 'react';
import Header from '../../Header';
import SchemaVersions from './SchemaVersions';
import SchemaUpdate from './SchemaUpdate';
import { get } from '../../../utils/api';
import endpoints from '../../../utils/endpoints';
import {getSelectedTab} from "../../../utils/functions";

class Schema extends Component {
  state = {
    clusterId: this.props.match.params.clusterId,
    schemaId: this.props.history.schemaId || this.props.match.params.schemaId,
    selectedTab: 'update',
    totalVersions: 0,
    schemaVersions: []
  };

  tabs = ['update', 'versions'];

  componentDidMount() {
    this.getSchemaVersions();
  }

  componentDidUpdate(prevProps, prevState) {
    if (this.props.location.pathname !== prevProps.location.pathname) {
      const tabSelected = getSelectedTab(this.props, this.tabs);
      this.setState({ selectedTab: tabSelected });
    }
  }

  selectTab = tab => {
    const { schemaId, clusterId } = this.state;
    this.setState({ selectedTab: tab });
    this.props.history.push(`/ui/${clusterId}/schema/details/${schemaId}/${tab}`);
  };

  tabClassName = tab => {
    const { selectedTab } = this.state;
    return selectedTab === tab ? 'nav-link active' : 'nav-link';
  };

  async getSchemaVersions() {
    let schemas = [];
    const { clusterId, schemaId } = this.state;
    const tabSelected = getSelectedTab(this.props, this.tabs);

    schemas = await get(endpoints.uriSchemaVersions(clusterId, schemaId));
    this.setState({
      schemaVersions: schemas.data,
      totalVersions: schemas.data.length,
      selectedTab: tabSelected === 'versions' ? tabSelected : 'update'
    },
      () => {
        this.props.history.replace(`/ui/${clusterId}/schema/details/${schemaId}/${this.state.selectedTab}`);
      });
  }

  renderSelectedTab() {
    const { selectedTab, schemaVersions } = this.state;
    const { history, match } = this.props;
    const { clusterId } = this.props.match.params;
    const { schemaId } = this.props.history.state || this.props.match.params;

    switch (selectedTab) {
      case 'update':
        return (
          <SchemaUpdate
            getSchemaVersions={() => {
              this.getSchemaVersions();
            }}
            schemaId={schemaId}
            history={history}
            match={match}
          />
        );
      case 'versions':
        return (
          <SchemaVersions
            schemaName={schemaId}
            clusterId={clusterId}
            schemas={schemaVersions}
            history={history}
            match={match}
          />
        );
      default:
        return <SchemaUpdate schemaId={schemaId} history={history} match={match} />;
    }
  }

  render() {
    const { schemaId, totalVersions } = this.state;

    return (
      <div>
        <Header title={`Schema: ${schemaId}`} history={this.props.history} />
        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            <li className="nav-item">
              <div
                className={this.tabClassName('update')}
                onClick={() => this.selectTab('update')}
                role="tab"
              >
                Update
              </div>
            </li>
            <li className="nav-item">
              <div
                className={this.tabClassName('versions')}
                onClick={() => this.selectTab('versions')}
                role="tab"
              >
                Versions <span className="badge badge-secondary">{totalVersions}</span>
              </div>
            </li>
          </ul>

          <div className="tab-content">
            <div className="tab-pane active" role="tabpanel">
              {this.renderSelectedTab()}
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default Schema;
