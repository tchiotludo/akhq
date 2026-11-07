import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';
import { matchPath } from 'react-router';
import constants from '../../utils/constants';
import { getHomePath } from '../../utils/functions';
import logoUrl from '../../images/logo.svg';
import iconUrl from '../../images/icon.svg';
import SideNav, { NavIcon, NavItem, NavText } from '@trendmicro/react-sidenav';
import '@trendmicro/react-sidenav/dist/react-sidenav.css';
import { withRouter } from '../../utils/withRouter';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faArrowRightFromBracket,
  faArrowRightToBracket,
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

export class Sidebar extends Component {
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
    flyoutPosition: null
  };

  static getDerivedStateFromProps(nextProps, prevState) {
    let selectedTab = nextProps.selectedTab || prevState.selectedTab;
    return { selectedTab };
  }

  componentDidMount() {
    window.addEventListener('resize', this.updateFlyoutPosition);
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
    const allClusters = clusters || [];
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
    if (this.props.expanded !== prevProps.expanded) {
      this.flyoutAnchor = null;
      this.setState({ flyoutPosition: null });
    }
    if (this.props.clusters !== prevProps.clusters) {
      this.handleGetClusters(this.props.clusters || [], selectedCluster => {
        this.handleRegistryAndConnectsAndKsqlDBs(selectedCluster);
      });
    }
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this.updateFlyoutPosition);
  }

  updateFlyoutPosition = () => {
    if (this.props.expanded || !this.flyoutAnchor) {
      return;
    }
    const { key, element } = this.flyoutAnchor;
    const rect = element.getBoundingClientRect();
    const menu = element.parentElement.querySelector('[role="menu"]');
    const height = Math.min(menu ? menu.scrollHeight : 0, window.innerHeight * 0.3);
    this.setState({
      flyoutPosition: {
        key,
        left: element.parentElement.getBoundingClientRect().right,
        top: Math.max(0, Math.min(rect.top, window.innerHeight - height - 60))
      }
    });
  };

  flyoutProps(key) {
    const position = this.state.flyoutPosition;
    return {
      onMouseEnter: event => {
        this.flyoutAnchor = { key, element: event.currentTarget };
        this.updateFlyoutPosition();
      },
      // Fixed positioning lets the hover menu escape the sidebar's scroll clip.
      subnavStyle: this.props.expanded
        ? undefined
        : {
            position: 'fixed',
            visibility: position && position.key === key ? 'visible' : 'hidden',
            left: position && position.key === key ? position.left : 0,
            top: position && position.key === key ? position.top : 0
          }
    };
  }

  isPlainClick(event) {
    return (
      event.button === 0 && !event.ctrlKey && !event.metaKey && !event.shiftKey && !event.altKey
    );
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

    const listKsqlDBs = allKsqlDBs.map(ksqlDB => (
      <NavItem
        key={`cluster/${ksqlDB}`}
        eventKey={`cluster/${ksqlDB}`}
        onClick={() => this.changeSelectedKsqlDB(ksqlDB)}
      >
        <NavText>
          <Link to={`/ui/${selectedCluster}/ksqldb/${ksqlDB}`}>
            <div
              className={selectedKsqlDB === ksqlDB ? ' active' : ''}
              style={{ color: '#759dac' }}
            >
              {ksqlDB}
            </div>
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
    // Navigation is handled by the <Link> inside listConnects.
    // Only update sidebar UI state here to avoid a double navigate.
    this.setState({ selectedConnect: connect, showConnects: false });
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
      <NavItem eventKey={label} className={pathname.includes(tab) ? 'active' : ''}>
        <NavIcon>
          {' '}
          <span>
            <FontAwesomeIcon icon={icon} aria-hidden={true} />
          </span>
        </NavIcon>
        <NavText>
          {' '}
          <span>{label}</span>
        </NavText>
        <Link
          className="sidebar-menu-link"
          aria-label={label}
          to={`/ui/${selectedCluster}/${tab}`}
          onClick={event => {
            if (this.isPlainClick(event)) this.setState({ selectedTab: tab });
          }}
        />
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
      enableRegistry,
      registryType,
      enableConnect,
      enableKsqlDB
    } = this.state;
    const { expanded } = this.props;
    const roles = this.state.roles || {};
    const tag = sessionStorage.getItem('version');
    const homePath = getHomePath(selectedCluster, roles);
    const { listConnects, listKsqlDBs, listClusters } = this.setClustersAndConnectsAndKsqlDBs();
    const sidebarToggle = (
      <button
        type="button"
        className="sidebar-toggle"
        aria-label={expanded ? 'Collapse sidebar' : 'Expand sidebar'}
        aria-expanded={expanded}
        title={expanded ? 'Collapse sidebar' : 'Expand sidebar'}
        onClick={() => this.props.toggleSidebar(!expanded)}
      >
        <FontAwesomeIcon
          icon={expanded ? faArrowRightFromBracket : faArrowRightToBracket}
          flip={expanded ? 'horizontal' : undefined}
          aria-hidden={true}
        />
      </button>
    );

    return (
      <SideNav
        onScrollCapture={this.updateFlyoutPosition}
        expanded={expanded}
        style={{
          height: '100vh',
          position: 'fixed',
          paddingBottom: expanded && tag ? '100px' : '60px',
          overflowX: 'hidden',
          overflowY: 'auto'
        }}
      >
        <div className="logo-wrapper">
          <Link
            to={homePath}
            aria-label="AKHQ home"
            onClick={event => {
              if (this.isPlainClick(event)) {
                this.setState({ selectedTab: homePath.split('/')[3] });
              }
            }}
          >
            <img className="logo" src={expanded ? logoUrl : iconUrl} alt="AKHQ" />
          </Link>
          {expanded && sidebarToggle}
        </div>
        {!expanded && sidebarToggle}
        <SideNav.Nav defaultSelected={`${constants.TOPIC}`}>
          <NavItem eventKey="cluster" {...this.flyoutProps('cluster')}>
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
                <span className="badge bg-primary clusters">{selectedCluster}</span>
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
              {...this.flyoutProps('connects')}
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
                  <span className="badge bg-primary clusters">{selectedConnect}</span>
                </div>
              </NavText>

              {listConnects}
            </NavItem>
          )}
          {enableKsqlDB && roles && roles.KSQLDB && roles.KSQLDB.includes('READ') && (
            <NavItem
              eventKey="ksqlDBs"
              {...this.flyoutProps('ksqlDBs')}
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
                  <span className="badge bg-primary clusters">{selectedKsqlDB}</span>
                </div>
              </NavText>

              {listKsqlDBs}
            </NavItem>
          )}
          {this.renderMenuItem(faGear, constants.SETTINGS, 'Settings')}
        </SideNav.Nav>
        {expanded && tag && (
          <p className="sidebar-version" title={tag}>
            {tag}
          </p>
        )}
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
