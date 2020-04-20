import React, { Component } from 'react';
import logo from '../../images/logo.svg';
import TabContainer from 'react-bootstrap/TabContainer';
import { Link, withRouter } from 'react-router-dom';
import { matchPath } from 'react-router';
import api from '../../utils/api';
import endpoints from '../../utils/endpoints';
import constants from '../../utils/constants';
import _ from 'lodash';

// Adaptation of template.ftl
class Sidebar extends Component {
  state = {
    selectedTab: constants.TOPIC,
    selectedCluster: '',
    selectedConnect: '',
    allClusters: [],
    allConnects: [],
    showClusters: false,
    showConnects: false
  };
  static getDerivedStateFromProps(nextProps, prevState) {
    let selectedTab = nextProps.selectedTab || prevState.selectedTab;
    return { selectedTab };
  }
  componentDidMount() {
    let tabs = [
      constants.CLUSTER,
      constants.CONNECT,
      constants.GROUP,
      constants.NODE,
      constants.SCHEMA,
      constants.TAIL,
      constants.TOPIC,
      constants.ACLS
    ];
    let path = this.props.location.pathname.split('/');
    if (tabs.find(el => el === path[2])) {
      this.setState({ selectedTab: path[2] });
    }
    this.handleGetClusters(selectedCluster => {
      this.handleGetConnects(selectedCluster);
    });
  }

  async handleGetClusters(callback = () => {}) {
    const match = matchPath(this.props.history.location.pathname, {
      path: '/:clusterId/',
      exact: false,
      strict: false
    });
    console.log(match);
    const clusterId = match ? match.params.clusterId || '' : '';
    let allClusters = {};
    try {
      allClusters = await api.get(endpoints.uriClusters());
      allClusters = _(allClusters.data)
        .sortBy(cluster => cluster.id)
        .value();
      const cluster = allClusters.find(cluster => cluster.id === clusterId).id;
      this.setState(
        {
          allClusters: allClusters,
          selectedCluster: cluster || allClusters[0].id
        },
        () => {
          const { selectedCluster } = this.state;

          callback(selectedCluster);
        }
      );
    } catch (err) {
      this.props.history.replace('/error', { errorData: err });
    }
  }

  async handleGetConnects(selectedCluster) {
    const { allClusters } = this.state;
    const cluster = allClusters.find(cluster => cluster.id === selectedCluster);
    this.setState({ allConnects: cluster.connects, selectedConnect: cluster.connects[0] });
  }

  setClustersAndConnects = () => {
    const { allClusters, allConnects, selectedCluster, selectedConnect } = this.state;
    const listClusters = allClusters.map(cluster => (
      <li key={cluster.id} onClick={() => this.changeSelectedCluster(cluster)}>
        <a className={selectedCluster === cluster.id ? ' active' : ''}>{cluster.id}</a>
      </li>
    ));
    const listConnects = allConnects.map(connect => (
      <li key={connect} onClick={() => this.changeSelectedConnect(connect)}>
        <a className={selectedConnect === connect ? ' active' : ''}>{connect}</a>
      </li>
    ));

    return { listClusters, listConnects };
  };

  changeSelectedCluster(newSelectedCluster) {
    this.setState(
      {
        selectedCluster: newSelectedCluster.id,
        showClusters: false
      },
      () => {
        const { selectedCluster } = this.state;
        this.props.history.push({
          pathname: `/${selectedCluster}/topic`,
          selectedCluster
        });

        this.handleGetConnects(selectedCluster);
      }
    );
  }

  changeSelectedConnect(connect) {
    this.setState({ selectedConnect: connect, showConnects: false }, () => {
      const { selectedConnect, selectedCluster } = this.state;
      this.props.history.push({
        pathname: `/${selectedCluster}/connect/${selectedConnect}`,
        selectedCluster
      });
    });
  }

  renderMenuItem(iconClassName, tab, label) {
    const { selectedCluster, selectedTab } = this.state;
    return (
      <li
        className={selectedTab === tab ? 'active' : ''}
        onClick={() => {
          this.setState({ selectedTab: tab });
        }}
      >
        <Link to={`/${selectedCluster}/${tab}`}>
          <i className={iconClassName} aria-hidden="true" />
          {label}
        </Link>
      </li>
    );
  }

  render() {
    const {
      selectedConnect,
      selectedCluster,
      showClusters,
      showConnects,
      selectedTab
    } = this.state;
    const tag = 'Snapshot';
    let login = localStorage.getItem('login');
    const { listConnects, listClusters } = this.setClustersAndConnects();
    return (
      <div className="wrapper">
        <TabContainer id="khq-sidebar-tabs" defaultActiveKey="first">
          <nav id="khq-sidebar">
            <div className="sidebar-header">
              <div className="version">{tag}</div>
              <a href="#">
                <h3 className="logo">
                  <img src={logo} alt="" />
                </h3>
              </a>
            </div>
            <ul className="list-unstyled components">
              <li className={selectedTab === constants.CLUSTER ? 'active' : ''}>
                <a
                  data-toggle="collapse"
                  aria-expanded={showClusters}
                  className="dropdown-toggle"
                  onClick={() => {
                    this.setState({ showClusters: !showClusters, selectedTab: constants.CLUSTER });
                  }}
                >
                  <i className="fa fa-fw fa fa-database" aria-hidden="true" />
                  Clusters
                  <span className="badge badge-success">{selectedCluster}</span>
                </a>
                <ul
                  className={`list-unstyled ${
                    showClusters && selectedTab === constants.CLUSTER ? 'show' : 'collapse'
                  }`}
                  id="clusters"
                >
                  {listClusters}
                </ul>
              </li>
              {this.renderMenuItem('fa fa-fw fa-laptop', constants.NODE, 'Nodes')}
              {this.renderMenuItem('fa fa-fw fa-list', constants.TOPIC, 'Topics')}
              {this.renderMenuItem('fa fa-fw fa-level-down', constants.TAIL, 'Live Tail')}
              {this.renderMenuItem('fa fa-fw fa-object-group', constants.GROUP, 'Consumer Groups')}
              {this.renderMenuItem('fa fa-fw fa-key', constants.ACLS, 'ACLS')}
              {this.renderMenuItem('fa fa-fw fa-cogs', constants.SCHEMA, 'Schema Registry')}
              <li className={selectedTab === constants.CONNECT ? 'active' : ''}>
                <Link
                  to={`/${selectedCluster}/connect/${selectedConnect}`}
                  data-toggle="collapse"
                  aria-expanded={showConnects}
                  className="dropdown-toggle"
                  onClick={() => {
                    this.setState({ showConnects: !showConnects, selectedTab: constants.CONNECT });
                  }}
                >
                  <i className="fa fa-fw fa fa-exchange" aria-hidden="true" /> Connects
                  <span className="badge badge-success">{selectedConnect}</span>
                </Link>
                <ul className={`list-unstyled ${showConnects ? 'show' : 'collapse'}`} id="connects">
                  {listConnects}
                </ul>
              </li>{' '}
            </ul>
            <div className="sidebar-log">
              {login === 'true' && (
                <a
                  style={{ cursor: 'pointer' }}
                  onClick={() => {
                    localStorage.setItem('login', 'false');
                    this.forceUpdate();
                  }}
                  data-turbolinks="false"
                >
                  <i className="fa fa-fw fa-sign-in" aria-hidden="true" />
                  Logout
                </a>
              )}
              {(login === 'false' || !login) && (
                <Link to="/login" data-turbolinks="false">
                  <i className="fa fa-fw fa-sign-in" aria-hidden="true" />
                  Login
                </Link>
              )}
            </div>
          </nav>
        </TabContainer>
      </div>
    );
  }
}

export default withRouter(Sidebar);
