import React, { Component } from 'react';
import PropTypes from 'prop-types';
import Header from '../../Header/Header';
import { getSelectedTab } from '../../../utils/functions';
import { Link } from 'react-router-dom';
import KsqlDBInfo from './KsqlDBInfo/KsqlDBInfo';
import KsqlDBStreams from './KsqlDBStreams/KsqlDBStreams';
import KsqlDBTables from './KsqlDBTables/KsqlDBTables';
import KsqlDBQueries from './KsqlDBQueries/KsqlDBQueries';

class KsqlDBList extends Component {
  state = {
    clusterId: this.props.history.clusterId || this.props.match.params.clusterId,
    ksqlDBId: this.props.history.ksqlDBId || this.props.match.params.ksqlDBId,
    selectedTab: 'streams',
    roles: JSON.parse(sessionStorage.getItem('roles'))
  };

  tabs = {
    streams: KsqlDBStreams,
    tables: KsqlDBTables,
    queries: KsqlDBQueries,
    info: KsqlDBInfo
  };

  componentDidMount() {
    const { clusterId, ksqlDBId } = this.props.match.params;
    const tabSelected = getSelectedTab(this.props, Object.keys(this.tabs));
    this.setState({ selectedTab: tabSelected ? tabSelected : 'streams' }, () => {
      this.props.history.replace(`/ui/${clusterId}/ksqldb/${ksqlDBId}/${this.state.selectedTab}`);
    });
  }

  componentDidUpdate(prevProps) {
    if (this.props.location.pathname !== prevProps.location.pathname) {
      const tabSelected = getSelectedTab(this.props, Object.keys(this.tabs));
      this.setState({ selectedTab: tabSelected });
    }
  }

  tabClassName = tab => {
    const { selectedTab } = this.state;
    return selectedTab === tab ? 'nav-link active' : 'nav-link';
  };

  renderSelectedTab() {
    const { clusterId, ksqlDBId, selectedTab } = this.state;
    const { history, match, location } = this.props;
    const SelectedTab = this.tabs[selectedTab] || KsqlDBStreams;

    return (
      <SelectedTab
        clusterId={clusterId}
        ksqlDBId={ksqlDBId}
        history={history}
        match={match}
        location={location}
      />
    );
  }

  render() {
    const { clusterId, ksqlDBId } = this.state;
    const roles = this.state.roles || {};
    return (
      <div>
        <Header title={`KsqlDB: ${ksqlDBId}`} history={this.props.history} />
        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            <li className="nav-item">
              <Link
                to={`/ui/${clusterId}/ksqldb/${ksqlDBId}/streams`}
                className={this.tabClassName('streams')}
              >
                Streams
              </Link>
            </li>
            <li className="nav-item">
              <Link
                to={`/ui/${clusterId}/ksqldb/${ksqlDBId}/tables`}
                className={this.tabClassName('tables')}
              >
                Tables
              </Link>
            </li>
            <li className="nav-item">
              <Link
                to={`/ui/${clusterId}/ksqldb/${ksqlDBId}/queries`}
                className={this.tabClassName('queries')}
              >
                Queries
              </Link>
            </li>
            <li className="nav-item">
              <Link
                to={`/ui/${clusterId}/ksqldb/${ksqlDBId}/info`}
                className={this.tabClassName('info')}
              >
                Info
              </Link>
            </li>
          </ul>

          <div className="tab-content">
            <div className="tab-pane active" role="tabpanel">
              {this.renderSelectedTab()}
            </div>
          </div>
        </div>
        {roles && roles.KSQDLDB && roles.KSQLDB.includes('EXECUTE') && (
          <aside>
            <li className="aside-button">
              <Link
                to={`/ui/${clusterId}/ksqldb/${ksqlDBId}/query`}
                className="btn btn-primary mr-2"
              >
                Execute queries
              </Link>
            </li>
            <li className="aside-button">
              <Link
                to={`/ui/${clusterId}/ksqldb/${ksqlDBId}/statement`}
                className="btn btn-primary mr-2"
              >
                Execute statements
              </Link>
            </li>
          </aside>
        )}
      </div>
    );
  }
}

KsqlDBList.propTypes = {
  history: PropTypes.object,
  location: PropTypes.object,
  match: PropTypes.object
};

export default KsqlDBList;
