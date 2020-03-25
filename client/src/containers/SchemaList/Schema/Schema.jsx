import React, { Component } from 'react';

import { Link } from 'react-router-dom';

import Header from '../../Header';
import SchemaVersions from './SchemaVersions';
import SchemaUpdate from './SchemaUpdate';

class Schema extends Component {
  state = {
    clusterId: '',
    schemaId: '',
    selectedTab: 'update'
  };

  componentDidMount() {
    const { clusterId } = this.props.match.params;
    const { schemaId } = this.props.history.state || this.props.match.params;

    this.setState({ clusterId, schemaId });
  }

  selectTab = tab => {
    this.setState({ selectedTab: tab });
  };

  tabClassName = tab => {
    const { selectedTab } = this.state;
    return selectedTab === tab ? 'nav-link active' : 'nav-link';
  };

  renderSelectedTab() {
    const { selectedTab } = this.state;
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
            history={history}
            match={match}
          />
        );
      default:
        return <SchemaUpdate schemaId={schemaId} history={history} match={match} />;
    }
  }

  render() {
    const { schemaId } = this.state;

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
                Versions
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
