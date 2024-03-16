import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';
import { matchPath } from 'react-router';
import constants from '../../utils/constants';
import sortBy from 'lodash/sortBy';
import './styles.scss';
import SideNav, { NavIcon, NavItem, NavText } from '@trendmicro/react-sidenav';
import '@trendmicro/react-sidenav/dist/react-sidenav.css';
import { withRouter } from '../../utils/withRouter';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faCogs,
  faDatabase,
  faExchange,
  faGear,
  faKey,
  faLaptop,
  faLevelDown,
  faList,
  faObjectGroup,
  faRocket
} from '@fortawesome/free-solid-svg-icons';

class Sidebar extends Component {
  state = {
    selectedTab: constants.TOPIC,
    selectedCluster: '',
    selectedConnect: '',
    selectedKsqlDB: '',
    allClusters: [],
    allConnects: [],
    allKsqlDBs: [],
    showClusters: false,
    showConnects: false,
    showKsqlDBs: false,
    enableRegistry: false,
    registryType: '',
    enableConnect: false,
    enableKsqlDB: false,
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
      constants.KSQLDB,
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
        this.handleRegistryAndConnectsAndKsqlDBs(selectedCluster);
      });
    }
  }

  handleGetClusters(clusters, callback = () => {}) {
    const match = matchPath(
      {
        path: '/ui/:clusterId/',
        end: false,
        caseSensitive: false
      },
      this.props.location.pathname
    );

    const clusterId = match ? match.params.clusterId || '' : '';
    const allClusters = sortBy(clusters || [], cluster => cluster.id);
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
        this.handleRegistryAndConnectsAndKsqlDBs(selectedCluster);
      });
    }
  }

  handleRegistryAndConnectsAndKsqlDBs(selectedCluster) {
    const { allClusters } = this.state;
    const cluster = allClusters.find(cluster => cluster.id === selectedCluster);
    const enableConnects = cluster.connects !== undefined;
    const enableKsqlDB = cluster.ksqldbs !== undefined;
    let newState = {
      enableRegistry: cluster.registry,
      registryType: cluster.registryType,
      enableConnect: enableConnects,
      allConnects: [],
      selectedConnect: '',
      enableKsqlDB: enableKsqlDB,
      allKsqlDBs: [],
      selectedKsqlDB: ''
    };
    if (enableConnects) {
      newState = {
        ...newState,
        ...{
          allConnects: cluster.connects,
          selectedConnect: cluster.connects[0]
        }
      };
    }
    if (enableKsqlDB) {
      newState = {
        ...newState,
        ...{
          allKsqlDBs: cluster.ksqldbs,
          selectedKsqlDB: cluster.ksqldbs[0]
        }
      };
    }
    this.setState(newState);
  }

  setClustersAndConnectsAndKsqlDBs = () => {
    const {
      allClusters,
      allConnects,
      allKsqlDBs,
      selectedCluster,
      selectedConnect,
      selectedKsqlDB
    } = this.state;
    const listClusters = allClusters.map(cluster => (
      <NavItem
        key={`cluster/${cluster.id}`}
        eventKey={`cluster/${cluster.id}`}
        onClick={() => this.changeSelectedCluster(cluster)}
      >
        <NavText style={{ color: '#32a9d4' }}>
          {' '}
          <Link to={`/ui/${cluster.id}/topic`}>
            <div className={selectedCluster === cluster.id ? ' active' : ''}>{cluster.id}</div>
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
            <div>{connect}</div>
          </Link>
        </NavText>
      </NavItem>
    ));

    const listKsqlDBs = allKsqlDBs.map(ksqlDB => (
      <NavItem
        key={`cluster/${ksqlDB}`}
        eventKey={`cluster/${ksqlDB}`}
        onClick={() => this.changeSelectedKsqlDB(ksqlDB)}
      >
        <NavText>
          <Link to={`/ui/${selectedCluster}/ksqldb/${ksqlDB}`}>
            <div>{ksqlDB}</div>
          </Link>
        </NavText>
      </NavItem>
    ));

    return { listClusters, listConnects, listKsqlDBs };
  };

  changeSelectedCluster(newSelectedCluster) {
    this.setState(
      {
        selectedCluster: newSelectedCluster.id,
        showClusters: false
      },
      () => {
        const { selectedCluster } = this.state;
        this.props.router.navigate(
          {
            pathname: `/ui/${selectedCluster}/topic`,
            selectedCluster
          },
          { replace: true }
        );

        this.handleRegistryAndConnectsAndKsqlDBs(selectedCluster);
      }
    );
  }

  changeSelectedConnect(connect) {
    this.setState({ selectedConnect: connect, showConnects: false }, () => {
      const { selectedConnect, selectedCluster } = this.state;
      this.props.router.navigate(
        {
          pathname: `/ui/${selectedCluster}/connect/${selectedConnect}`,
          selectedCluster
        },
        { replace: true }
      );
    });
  }

  changeSelectedKsqlDB(ksqlDB) {
    this.setState({ selectedKsqlDB: ksqlDB, showKsqlDBs: false }, () => {
      const { selectedKsqlDB, selectedCluster } = this.state;
      this.props.router.navigate(
        {
          pathname: `/ui/${selectedCluster}/ksqldb/${selectedKsqlDB}`,
          selectedCluster
        },
        { replace: true }
      );
    });
  }

  renderMenuItem(icon, tab, label) {
    const { selectedCluster } = this.state;
    const pathname = window.location.pathname;
    return (
      <NavItem
        eventKey={label}
        className={pathname.includes(tab) ? 'active' : ''}
        onClick={() => {
          this.setState({ selectedTab: tab });
          /* eslint-disable react/prop-types */
          this.props.router.navigate(`/ui/${selectedCluster}/${tab}`, { replace: true });
          return false;
        }}
      >
        <NavIcon>
          {' '}
          <Link
            to={`/ui/${selectedCluster}/${tab}`}
            onClick={e => {
              this.setState({ selectedTab: tab });
              e.preventDefault();
            }}
          >
            <FontAwesomeIcon icon={icon} aria-hidden={true} />
          </Link>
        </NavIcon>
        <NavText>
          {' '}
          <Link
            to={`/ui/${selectedCluster}/${tab}`}
            onClick={e => {
              this.setState({ selectedTab: tab });
              e.preventDefault();
            }}
          >
            {label}
          </Link>
        </NavText>
      </NavItem>
    );
  }

  render() {
    const {
      selectedConnect,
      selectedKsqlDB,
      selectedCluster,
      showClusters,
      showConnects,
      showKsqlDBs,
      selectedTab,
      height,
      enableRegistry,
      registryType,
      enableConnect,
      enableKsqlDB
    } = this.state;
    const { expanded } = this.props;
    const roles = this.state.roles || {};
    const tag = sessionStorage.getItem('version');
    const { listConnects, listKsqlDBs, listClusters } = this.setClustersAndConnectsAndKsqlDBs();
    return (
      <SideNav
        expanded={expanded}
        onToggle={expanded => {
          this.props.toggleSidebar(expanded);
        }}
        style={{
          background: 'black',
          height: height,
          position: 'fixed',
          paddingBottom: '60px',
          ...(expanded ? { overflowY: 'auto' } : {})
        }}
      >
        <SideNav.Toggle />
        <div className="logo-wrapper">
          <span className="logo" />
          {this.props.expanded && (
            <p
              style={{
                color: 'white',
                fontStyle: 'Italic',
                textAlign: 'center',
                margin: '20px 0 0 0'
              }}
            >
              {''}
              {tag}
            </p>
          )}
        </div>
        <SideNav.Nav defaultSelected={`${constants.TOPIC}`} style={{ background: 'black' }}>
          <NavItem className="nav-clusters" eventKey="cluster">
            <NavIcon>
              <FontAwesomeIcon icon={faDatabase} aria-hidden={true} />
            </NavIcon>
            <NavText>
              <div
                data-toggle="collapse"
                aria-expanded={showClusters}
                className="dropdown-toggle text-center"
                onClick={() => {
                  this.setState({ showClusters: !showClusters, selectedTab: constants.CLUSTER });
                }}
              >
                <span className="clusters">{selectedCluster}</span>
              </div>
            </NavText>
            {listClusters}
          </NavItem>

          {roles &&
            roles.NODE &&
            roles.NODE.includes('READ') &&
            this.renderMenuItem(faLaptop, constants.NODE, 'Nodes')}
          {roles &&
            roles.TOPIC &&
            roles.TOPIC.includes('READ') &&
            this.renderMenuItem(faList, constants.TOPIC, 'Topics')}
          {roles &&
            roles.TOPIC_DATA &&
            roles.TOPIC_DATA.includes('READ') &&
            this.renderMenuItem(faLevelDown, constants.TAIL, 'Live Tail')}
          {roles &&
            roles.CONSUMER_GROUP &&
            roles.CONSUMER_GROUP.includes('READ') &&
            this.renderMenuItem(faObjectGroup, constants.GROUP, 'Consumer Groups')}
          {roles &&
            roles.ACL &&
            roles.ACL.includes('READ') &&
            this.renderMenuItem(faKey, constants.ACLS, 'ACLS')}
          {enableRegistry &&
            registryType !== 'GLUE' &&
            roles &&
            roles.SCHEMA &&
            roles.SCHEMA.includes('READ') &&
            this.renderMenuItem(faCogs, constants.SCHEMA, 'Schema Registry')}
          {enableConnect && roles && roles.CONNECTOR && roles.CONNECTOR.includes('READ') && (
            <NavItem
              eventKey="connects"
              className={selectedTab === constants.CONNECT ? 'active' : ''}
            >
              <NavIcon>
                <FontAwesomeIcon icon={faExchange} aria-hidden={true} />
              </NavIcon>
              <NavText>
                <div
                  to={`/ui/${selectedCluster}/connect/${selectedConnect}`}
                  data-toggle="collapse"
                  aria-expanded={showConnects}
                  className="dropdown-toggle text-center"
                  onClick={() => {
                    this.setState({ showConnects: !showConnects, selectedTab: constants.CONNECT });
                  }}
                >
                  <span className="clusters">{selectedConnect}</span>
                </div>
              </NavText>

              {listConnects}
            </NavItem>
          )}
          {enableKsqlDB && roles && roles.KSQLDB && roles.KSQLDB.includes('READ') && (
            <NavItem
              eventKey="ksqlDBs"
              className={selectedTab === constants.KSQLDB ? 'active' : ''}
            >
              <NavIcon>
                <FontAwesomeIcon icon={faRocket} aria-hidden={true} />
              </NavIcon>
              <NavText>
                <div
                  to={`/ui/${selectedCluster}/ksqldb/${selectedKsqlDB}`}
                  data-toggle="collapse"
                  aria-expanded={showKsqlDBs}
                  className="dropdown-toggle text-center"
                  onClick={() => {
                    this.setState({ showKsqlDBs: !showKsqlDBs, selectedTab: constants.KSQLDB });
                  }}
                >
                  <span className="clusters">{selectedKsqlDB}</span>
                </div>
              </NavText>

              {listKsqlDBs}
            </NavItem>
          )}
          {this.renderMenuItem(faGear, constants.SETTINGS, 'Settings')}
        </SideNav.Nav>
      </SideNav>
    );
  }
}

Sidebar.propTypes = {
  router: PropTypes.object,
  location: PropTypes.object,
  clusters: PropTypes.array,
  children: PropTypes.any,
  expanded: PropTypes.bool,
  toggleSidebar: PropTypes.func,
  selectedTab: PropTypes.string
};

export default withRouter(Sidebar);
