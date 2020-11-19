import React, { Component } from 'react';
import { Link, withRouter } from 'react-router-dom';
import { matchPath } from 'react-router';
import constants from '../../utils/constants';
import _ from 'lodash';
import './styles.scss';
import SideNav, { NavIcon, NavItem, NavText } from '@trendmicro/react-sidenav';
import '@trendmicro/react-sidenav/dist/react-sidenav.css';

class Sidebar extends Component {
  state = {
    selectedTab: constants.TOPIC,
    selectedCluster: '',
    selectedConnect: '',
    allClusters: [],
    allConnects: [],
    showClusters: false,
    showConnects: false,
    enableRegistry: false,
    enableConnect: false,
    roles: JSON.parse(sessionStorage.getItem('roles')),
    height: 'auto'
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
    if (this.props.clusters && this.props.clusters.length > 0) {
      this.handleGetClusters(this.props.clusters || [], selectedCluster => {
        this.handleRegistryAndConnects(selectedCluster);
      });
    }
  }

  handleGetClusters(clusters, callback = () => {}) {
    const match = matchPath(this.props.history.location.pathname, {
      path: '/ui/:clusterId/',
      exact: false,
      strict: false
    });

    const clusterId = match ? match.params.clusterId || '' : '';
    let allClusters =
      _(clusters)
        .sortBy(cluster => cluster.id)
        .value() || [];
    const cluster = allClusters.find(cluster => cluster.id === clusterId);
    this.setState(
      {
        allClusters: allClusters,
        selectedCluster: (cluster ? cluster.id : allClusters[0].id) || allClusters[0].id
      },
      () => {
        const { selectedCluster } = this.state;
        callback(selectedCluster);
      }
    );
  }

  componentDidUpdate(prevProps) {
    if (this.props.location !== prevProps.location) {
      this.setState({ height: document.getElementById('root').offsetHeight });
    }
    if (this.props.clusters !== prevProps.clusters) {
      this.handleGetClusters(this.props.clusters || [], selectedCluster => {
        this.handleRegistryAndConnects(selectedCluster);
      });
    }
  }

  handleRegistryAndConnects(selectedCluster) {
    const { allClusters } = this.state;
    const cluster = allClusters.find(cluster => cluster.id === selectedCluster);
    const enableConnects = cluster.connects !== undefined;
    if (enableConnects) {
      this.setState({
        enableRegistry: cluster.registry,
        enableConnect: enableConnects,
        allConnects: cluster.connects,
        selectedConnect: cluster.connects[0]
      });
    } else {
      this.setState({
        enableRegistry: cluster.registry,
        enableConnect: enableConnects,
        allConnects: [],
        selectedConnect: ''
      });
    }
  }

  setClustersAndConnects = () => {
    const { allClusters, allConnects, selectedCluster, selectedConnect } = this.state;
    const listClusters = allClusters.map(cluster => (
      <NavItem
        key={`cluster/${cluster.id}`}
        eventKey={`cluster/${cluster.id}`}
        onClick={() => this.changeSelectedCluster(cluster)}
      >
        <NavText style={{ color: '#32a9d4' }}>
          {' '}
          <Link to={`/ui/${cluster.id}/topic`}>
          <div
            className={selectedCluster === cluster.id ? ' active' : ''}
            style={{ color: '#759dac' }}
          >
            {cluster.id}
          </div>
          </Link>
        </NavText>
      </NavItem>
    ));
    const listConnects = allConnects.map(connect => (
      <NavItem
        key={`cluster/${connect}`}
        eventKey={`cluster/${connect}`}
        onClick={() => this.changeSelectedConnect(connect)}
      >
        <NavText>
          <Link to={`/ui/${selectedCluster}/connect/${connect}`}>
          <div
            className={selectedConnect === connect ? ' active' : ''}
            style={{ color: '#759dac' }}
          >
            {connect}
          </div>
          </Link>
        </NavText>
      </NavItem>
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
          pathname: `/ui/${selectedCluster}/topic`,
          selectedCluster
        });

        this.handleRegistryAndConnects(selectedCluster);
      }
    );
  }

  changeSelectedConnect(connect) {
    this.setState({ selectedConnect: connect, showConnects: false }, () => {
      const { selectedConnect, selectedCluster } = this.state;
      this.props.history.push({
        pathname: `/ui/${selectedCluster}/connect/${selectedConnect}`,
        selectedCluster
      });
    });
  }

  renderMenuItem(iconClassName, tab, label) {
    const { selectedCluster } = this.state;
    const pathname = window.location.pathname;
    return (

      <NavItem
        eventKey={label}
        className={pathname.includes(tab) ? 'active' : ''}
        onClick={() => {
          this.setState({ selectedTab: tab });
          this.props.history.push({
            pathname: `/ui/${selectedCluster}/${tab}`
          });
          return false;
        }}
      >
        <NavIcon>
          {' '}
           <Link to={`/ui/${selectedCluster}/${tab}`}
                 onClick={(e) => {
                   this.setState({ selectedTab: tab });
                   e.preventDefault();
           }}>
               <i className={iconClassName} aria-hidden="true" />
           </Link>
        </NavIcon>
        <NavText>
          {' '}
          <Link to={`/ui/${selectedCluster}/${tab}`}
                onClick={(e) => {
                  this.setState({ selectedTab: tab });
                  e.preventDefault();
                }}>
              {label}
          </Link>
        </NavText>
      </NavItem>
    );
  }

  render() {
    const {
      selectedConnect,
      selectedCluster,
      showClusters,
      showConnects,
      selectedTab,
      height,
      enableRegistry,
      enableConnect
    } = this.state;
    const roles = this.state.roles || {};
    const tag = 'Snapshot';
    const { listConnects, listClusters } = this.setClustersAndConnects();
    return (
      <SideNav
        expanded={this.props.expanded}
        onToggle={expanded => {
          this.props.toggleSidebar(expanded);
        }}
        style={{ background: 'black', height: height, position: 'fixed' }}
      >
        <SideNav.Toggle />
        <div className="logo-wrapper">
          <span className="logo" />
        </div>
        <SideNav.Nav defaultSelected={`${constants.TOPIC}`} style={{ background: 'black' }}>
          <NavItem style={{ backgroundColor: 'Black', cursor: 'default' }}>
            <NavIcon />
            <NavText
              style={{
                color: 'grey',
                fontStyle: 'Italic',
                paddingLeft: '9%'
              }}
            >
              {''}
              {tag}
            </NavText>
          </NavItem>
          <NavItem eventKey="cluster">
            <NavIcon>
              <i className="fa fa-fw fa fa-database" aria-hidden="true" />
            </NavIcon>
            <NavText>
              <div
                data-toggle="collapse"
                aria-expanded={showClusters}
                className="dropdown-toggle"
                onClick={() => {
                  this.setState({ showClusters: !showClusters, selectedTab: constants.CLUSTER });
                }}
              >
                Clusters <span className="badge badge-primary clusters">{selectedCluster}</span>
              </div>
            </NavText>
            {listClusters}
          </NavItem>
          {roles &&
            roles.node &&
            this.renderMenuItem('fa fa-fw fa-laptop', constants.NODE, 'Nodes')}
          {roles &&
            roles.topic &&
            roles.topic['topic/read'] &&
            this.renderMenuItem('fa fa-fw fa-list', constants.TOPIC, 'Topics')}
          {roles &&
            roles.topic &&
            roles.topic['topic/data/read'] &&
            this.renderMenuItem('fa fa-fw fa-level-down', constants.TAIL, 'Live Tail')}
          {roles &&
            roles.group &&
            roles.group['group/read'] &&
            this.renderMenuItem('fa fa-fw fa-object-group', constants.GROUP, 'Consumer Groups')}
          {roles &&
            roles.acls &&
            roles.acls['acls/read'] &&
            this.renderMenuItem('fa fa-fw fa-key', constants.ACLS, 'ACLS')}
          {enableRegistry &&
            roles &&
            roles.registry &&
            roles.registry['registry/read'] &&
            this.renderMenuItem('fa fa-fw fa-cogs', constants.SCHEMA, 'Schema Registry')}
          {enableConnect && roles && roles.connect && roles.connect['connect/read'] && (
            <NavItem
              eventKey="connects"
              className={selectedTab === constants.CONNECT ? 'active' : ''}
            >
              <NavIcon>
                <i className="fa fa-fw fa fa-exchange" aria-hidden="true" />
              </NavIcon>
              <NavText>
                <Link
                  to={`/ui/${selectedCluster}/connect/${selectedConnect}`}
                  data-toggle="collapse"
                  aria-expanded={showConnects}
                  className="dropdown-toggle"
                  onClick={() => {
                    this.setState({ showConnects: !showConnects, selectedTab: constants.CONNECT });
                  }}
                >
                  Connects <span className="badge badge-primary">{selectedConnect}</span>
                </Link>
              </NavText>

              {listConnects}
            </NavItem>
          )}
          {this.renderMenuItem('fa fa-fw fa-gear', constants.SETTINGS, 'Settings')}
        </SideNav.Nav>
      </SideNav>
    );
  }
}

export default withRouter(Sidebar);
