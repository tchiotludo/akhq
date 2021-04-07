import React from 'react';
import Header from '../../Header';
import SchemaVersions from './SchemaVersions';
import SchemaUpdate from './SchemaUpdate';
import endpoints from '../../../utils/endpoints';
import {getSelectedTab} from "../../../utils/functions";
import {Link} from "react-router-dom";
import Root from "../../../components/Root";

class Schema extends Root {
  state = {
    clusterId: this.props.match.params.clusterId,
    schemaId: this.props.history.schemaId || this.props.match.params.schemaId,
    selectedTab: 'update',
    totalVersions: 0,
    schemaVersions: [],
    roles: JSON.parse(sessionStorage.getItem('roles'))
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

  tabClassName = tab => {
    const { selectedTab } = this.state;
    return selectedTab === tab ? 'nav-link active' : 'nav-link';
  };

  async getSchemaVersions() {
    let schemas = [];
    const { clusterId, schemaId, roles } = this.state;
    let tabSelected = getSelectedTab(this.props, this.tabs);

    if(!roles.registry['registry/update'] && tabSelected === 'update') {
      tabSelected = 'versions';
    }

    schemas = await this.getApi(endpoints.uriSchemaVersions(clusterId, schemaId));
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
        return <SchemaVersions
            schemaName={schemaId}
            clusterId={clusterId}
            schemas={schemaVersions}
            history={history}
            match={match}
        />;
    }
  }

  render() {
    const { clusterId, schemaId, totalVersions, roles } = this.state;

    return (
        <div>
          <Header title={`Schema: ${schemaId}`} history={this.props.history} />
          <div className="tabs-container">
            <ul className="nav nav-tabs" role="tablist">
              {roles.registry['registry/update'] && (
                  <li className="nav-item">
                    <Link to={`/ui/${clusterId}/schema/details/${schemaId}/update`}
                          className={this.tabClassName('update')}
                    >
                      Update
                    </Link>
                  </li>)}
              <li className="nav-item">
                <Link to={`/ui/${clusterId}/schema/details/${schemaId}/versions`}
                      className={this.tabClassName('versions')}
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
