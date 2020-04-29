import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import Header from '../../Header';
import SchemaVersions from './SchemaVersions';
import SchemaUpdate from './SchemaUpdate';
import api, { get } from '../../../utils/api';
import endpoints, { uriSchemaVersions } from '../../../utils/endpoints';

class Schema extends Component {
  state = {
    clusterId: this.props.match.params.clusterId,
    schemaId: this.props.history.schemaId || this.props.match.params.schemaId,
    selectedTab: 'update',
    totalVersions: 0,
    schemaVersions: []
  };

  componentDidMount() {
    this.getSchemaVersions();
  }

  selectTab = tab => {
    this.setState({ selectedTab: tab });
  };

  tabClassName = tab => {
    const { selectedTab } = this.state;
    return selectedTab === tab ? 'nav-link active' : 'nav-link';
  };

  async getSchemaVersions() {
    let schemas = [];
    const { clusterId, schemaId } = this.state;
    const { history } = this.props;
    history.replace({
      loading: true
    });
    try {
      schemas = await get(endpoints.uriSchemaVersions(clusterId, schemaId));
      this.setState({ schemaVersions: schemas.data, totalVersions: schemas.data.length });
    } catch (err) {
      console.error('Error:', err);
    } finally {
      history.replace({
        loading: false
      });
    }
  }

  renderSelectedTab() {
    const { selectedTab, schemaVersions } = this.state;
    const { history, match } = this.props;
    const { clusterId } = this.props.match.params;
    const { schemaId } = this.props.history.state || this.props.match.params;

    switch (selectedTab) {
      case 'update':
        return <SchemaUpdate schemaId={schemaId} history={history} match={match} />;
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
      <div id="content">
        <Header title={`Schema: ${schemaId}`} />
        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            <li className="nav-item">
              <Link
                className={this.tabClassName('update')}
                onClick={() => this.selectTab('update')}
                to="#"
                role="tab"
              >
                Update
              </Link>
            </li>
            <li className="nav-item">
              <Link
                className={this.tabClassName('versions')}
                onClick={() => this.selectTab('versions')}
                to="#"
                role="tab"
              >
                Versions <span className="badge badge-secondary">{totalVersions}</span>
              </Link>
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
