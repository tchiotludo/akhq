import React, { Component } from 'react';
import logo from '../../images/logo.svg';
import TabContainer from 'react-bootstrap/TabContainer';
import { Link, withRouter } from 'react-router-dom';
import { matchPath } from 'react-router';
import { get } from '../../utils/api';
import { uriClusters } from '../../utils/endpoints';
import constants from '../../utils/constants';
import { organizeRoles } from '../../utils/converters';
import _ from 'lodash';
import './styles.scss';
import SideNav, { Toggle, Nav, NavItem, NavIcon, NavText } from '@trendmicro/react-sidenav';
import '@trendmicro/react-sidenav/dist/react-sidenav.css';
// Adaptation of template.ftl
class Sidebar extends Component {
  state = {
    selectedTab: constants.TOPIC,
    selectedCluster: '',
    selectedConnect: '',
    allClusters: [],
    allConnects: [],
    showClusters: false,
    showConnects: false,
    roles: JSON.parse(localStorage.getItem('roles'))
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
      path: '/ui/:clusterId/',
      exact: false,
      strict: false
    });

    const clusterId = match ? match.params.clusterId || '' : '';
    let allClusters = {};
    try {
      allClusters = await get(uriClusters());
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
      if (err.response && err.response.status === 404) {
        this.props.history.replace('/ui/page-not-found', { errorData: err });
      } else {
        this.props.history.replace('/ui/error', { errorData: err });
      }
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
      <NavItem
        eventKey={`cluster/${cluster.id}`}
        onClick={() => this.changeSelectedCluster(cluster)}
      >
        <NavText style={{ color: '#32a9d4' }}>
          {' '}
          <a
            className={selectedCluster === cluster.id ? ' active' : ''}
            style={{ color: '#759dac' }}
          >
            {cluster.id}
          </a>
        </NavText>
      </NavItem>
    ));
    const listConnects = allConnects.map(connect => (
      <NavItem eventKey={`cluster/${connect}`} onClick={() => this.changeSelectedConnect(connect)}>
        <NavText>
          <a className={selectedConnect === connect ? ' active' : ''} style={{ color: '#759dac' }}>
            {connect}
          </a>
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

        this.handleGetConnects(selectedCluster);
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
    const { selectedCluster, selectedTab } = this.state;
    const pathname = window.location.pathname;
    return (
      <NavItem
        eventKey={label}
        className={pathname.includes(tab) ? 'active' : ''}
        onClick={() => {
          this.setState({ selectedTab: tab });
        }}
      >
        <NavIcon>
          {' '}
          <Link to={`/ui/${selectedCluster}/${tab}`}>
            <i className={iconClassName} aria-hidden="true" />
          </Link>
        </NavIcon>
        <NavText>
          {' '}
          <Link to={`/ui/${selectedCluster}/${tab}`}>{label}</Link>
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
      selectedTab
    } = this.state;
    const roles = this.state.roles || {};
    const tag = 'Snapshot';
    const { listConnects, listClusters } = this.setClustersAndConnects();
    const height = document.getElementById('root').offsetHeight;
    return (
      <SideNav
        expanded={this.props.expanded}
        onToggle={expanded => {
          this.props.toggleSidebar(expanded);
        }}
        style={{ background: 'black', height: height }}
      >
        <SideNav.Toggle /> <img src={logo} alt="" />
        <SideNav.Nav
          defaultSelected={`${constants.TOPIC}`}
          id="khq-sidebar-tabs"
          style={{ background: 'black' }}
          defaultActiveKey={selectedTab}
        >
          <NavItem style={{ backgroundColor: 'Black', cursor: 'default' }}>
            <NavIcon></NavIcon>
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
              <Link
                data-toggle="collapse"
                aria-expanded={showClusters}
                className="dropdown-toggle"
                onClick={() => {
                  this.setState({ showClusters: !showClusters, selectedTab: constants.CLUSTER });
                }}
              >
                Clusters <span className="badge badge-primary clusters">{selectedCluster}</span>
              </Link>
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
          {roles &&
            roles.registry &&
            roles.registry['registry/read'] &&
            this.renderMenuItem('fa fa-fw fa-cogs', constants.SCHEMA, 'Schema Registry')}
          {roles && roles.connect && roles.connect['connect/read'] && (
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
        </SideNav.Nav>
      </SideNav>
    );
  }
}

export default withRouter(Sidebar);
