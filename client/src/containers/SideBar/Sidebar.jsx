import React, { Component } from 'react';
import logo from '../../images/logo.svg';
import TabContainer from 'react-bootstrap/TabContainer';
import { Link, withRouter } from 'react-router-dom';
import api from '../../utils/api';
import endpoints from '../../utils/endpoints';
import constants from '../../utils/constants';

// Adaptation of template.ftl
class Sidebar extends Component {
  state = {
    selectedTab: constants.TOPIC,
    selectedCluster: '',
    selectedConnect: {},
    allClusters: [],
    allConnects: [],
    showClusters: false,
    showConnects: false
  };

  componentDidMount() {
    this.handleGetClusters(selectedCluster => {
      this.handleGetConnects(selectedCluster);
    });
  }

  async handleGetClusters(callback = () => {}) {
    let allClusters = {};
    try {
      allClusters = await api.get(endpoints.uriClusters());
      this.setState(
        { allClusters: allClusters.data, selectedCluster: allClusters.data[0].id },
        () => {
          const { selectedCluster } = this.state;
          this.props.history.push({
            pathname: `/${selectedCluster}/topic`,
            selectedCluster
          });
          callback(selectedCluster);
        }
      );
    } catch (err) {
      console.log('Erro allClusters:' + err);
    }
  }

  async handleGetConnects(selectedCluster) {
    let allConnects = {};
    try {
      allConnects = await api.get(endpoints.uriConnects(selectedCluster));
      this.setState({ allConnects: allConnects.data, selectedConnect: allConnects.data[0] });
    } catch (err) {
      console.log('Erro allConnects:' + err);
    }
  }

  setClustersAndConnects = () => {
    const { allClusters, allConnects, selectedCluster, selectedConnect } = this.state;
    const listClusters = allClusters.map(cluster => (
      <li key={cluster.id} onClick={() => this.changeSelectedCluster(cluster)}>
        <a className={selectedCluster === cluster.id ? ' active' : ''}>{cluster.id}</a>
      </li>
    ));
    const listConnects = allConnects.map(connect => (
      <li key={connect.name} onClick={() => this.changeSelectedConnect(connect)}>
        <a className={selectedConnect.name === connect.name ? ' active' : ''}>{connect.name}</a>
      </li>
    ));
    return { listClusters, listConnects };
  };

  changeSelectedCluster(newSelectedCluster) {
    this.setState({ selectedCluster: newSelectedCluster.id }, () => {
      const { selectedCluster } = this.state;
      this.props.history.push({
        pathname: `/${selectedCluster}/topic`,
        selectedCluster
      });
      this.handleGetConnects(selectedCluster);
    });
  }

  changeSelectedConnect(connect) {
    this.setState({ selectedConnect: connect }, () => {
      const { selectedConnect, selectedCluster } = this.state;
      this.props.history.push({
        pathname: `/${selectedCluster}/connect/${selectedConnect.name}`,
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
          <i className={iconClassName} aria-hidden="true" /> {label}
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

    const { listConnects, listClusters } = this.setClustersAndConnects();
    return (
      <div className="wrapper">
        <TabContainer id="khq-sidebar-tabs" defaultActiveKey="first">
          <nav id="khq-sidebar">
            <div className="sidebar-header">
              <a href="#">
                <h3 className="logo">
                  <img src={logo} alt="" />
                  <sup>
                    <strong>HQ</strong>
                  </sup>
                </h3>
              </a>
              <div className="version">{tag}</div>
            </div>
            <ul className="list-unstyled components">
              <li className={selectedTab === constants.CLUSTER ? 'active' : ''}>
                <Link
                  to={`/${selectedCluster}/topic`}
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
                </Link>
                <ul className={`list-unstyled ${showClusters ? 'show' : 'collapse'}`} id="clusters">
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
                  to={`/${selectedCluster}/connect`}
                  data-toggle="collapse"
                  aria-expanded={showConnects}
                  className="dropdown-toggle"
                  onClick={() => {
                    this.setState({ showConnects: !showConnects, selectedTab: constants.CONNECT });
                  }}
                >
                  <i className="fa fa-fw fa fa-exchange" aria-hidden="true" /> Connects
                  <span className="badge badge-success">{selectedConnect.name}</span>
                </Link>
                <ul className={`list-unstyled ${showConnects ? 'show' : 'collapse'}`} id="connects">
                  {listConnects}
                </ul>
              </li>{' '}
            </ul>
            <div className="sidebar-log">
              <Link to="/login" data-turbolinks="false">
                <i className="fa fa-fw fa-sign-in" aria-hidden="true" />
                Login
              </Link>
            </div>
          </nav>
        </TabContainer>
      </div>
    );
  }
}

export default withRouter(Sidebar);
