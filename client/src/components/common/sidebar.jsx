import React, {Component} from 'react';
import logo from '../../images/logo.svg';
import TabContainer from 'react-bootstrap/TabContainer';
import {Link} from 'react-router-dom';
//import { getBsProps } from "react-bootstrap/lib/utils/bootstrapUtils";
import api from '../../services/api';
import endpoints from '../../services/endpoints';

// Adaptation of template.ftl
class Sidebar extends Component {
  state = {
    selectedCluster: this.props.clusterId,
    selectedConnect: {},
    allClusters: [],
    allConnects: [],
    showClusters: false,
    showConnects: false
  };

  componentDidMount() {
    this.handleGetClusters();
    this.handleGetConnects(this.state.selectedCluster);
  }

  async handleGetClusters() {
    let allClusters = {};
    try {
      allClusters = await api.get(endpoints.uriClusters());
      this.setState({allClusters: allClusters.data});
    } catch (err) {
      console.log('Erro allClusters:' + err);
    }
  }

  async handleGetConnects(selectedCluster) {
    let allConnects = {};
    try {
      allConnects = await api.get(endpoints.uriConnects(selectedCluster));
      this.setState({allConnects: allConnects.data, selectedConnect: allConnects.data[0]});
    } catch (err) {
      console.log('Erro allConnects:' + err);
    }
  }

  setClustersAndConnects = () => {
    const {allClusters, allConnects} = this.state;
    const listClusters = allClusters.map(cluster => (
      <li
        key={cluster.id}
        className="list-group-item"
        onClick={() => this.setState({selectedCluster: cluster.id})}
      >
        {cluster.id}
      </li>
    ));
    const listConnects = allConnects.map(connect => (
      <li
        key={connect.name}
        className="list-group-item"
        onClick={() => this.setState({selectedConnect: connect})}
      >
        {connect.name}
      </li>
    ));
    return {listClusters, listConnects};
  };

  render() {
    const {selectedTab} = this.props;
    const {selectedConnect, selectedCluster, showClusters, showConnects} = this.state;
    const tag = 'Snapshot';

    const {listConnects, listClusters} = this.setClustersAndConnects();

    console.log('connects', listConnects);
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
              <li className={selectedTab === 'cluster' ? 'active' : ''}>
                <Link
                  to={`/${selectedCluster}/topic`}
                  data-toggle="collapse"
                  aria-expanded={showClusters}
                  className="dropdown-toggle"
                  onClick={() => {
                    this.setState({showClusters: !showClusters});
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
              <li className={this.selectedTab === 'node' ? 'active' : ''}>
                <Link to={`/${selectedCluster}/node`}>
                  <i className="fa fa-fw fa-laptop" aria-hidden="true" /> Nodes
                </Link>
              </li>
              <li className={this.selectedTab === 'topic' ? 'active' : ''}>
                <Link to={`/${selectedCluster}/topic`}>
                  <i className="fa fa-fw fa-list" aria-hidden="true" /> Topics
                </Link>
              </li>
              <li className={this.selectedTab === 'tail' ? 'active' : ''}>
                <Link to={`/${selectedCluster}/tail`}>
                  <i className="fa fa-fw fa-level-down" aria-hidden="true" /> Live Tail
                </Link>
              </li>
              <li className={this.selectedTab === 'group' ? 'active' : ''}>
                <Link to={`/${selectedCluster}/group`}>
                  <i className="fa fa-fw fa-object-group" aria-hidden="true" /> Consumer Groups
                </Link>
              </li>
              <li className={this.selectedTab === 'acls' ? 'active' : ''}>
                <Link to={`/${selectedCluster}/acls`}>
                  <i className="fa fa-fw fa-key" aria-hidden="true" /> ACLS
                </Link>
              </li>
              <li className={this.selectedTab === 'schema' ? 'active' : ''}>
                <Link to={`/${selectedCluster}/schema`}>
                  <i className="fa fa-fw fa-cogs" aria-hidden="true" /> Schema Registry
                </Link>
              </li>
              <li className={this.selectedTab === 'connect' ? 'active' : ''}>
                <Link
                  to={`/${selectedCluster}/connect`}
                  data-toggle="collapse"
                  aria-expanded={showConnects}
                  className="dropdown-toggle"
                  onClick={() => {
                    this.setState({showConnects: !showConnects});
                  }}
                >
                  <i className="fa fa-fw fa fa-exchange" aria-hidden="true" /> Connects
                  <span className="badge badge-success">{selectedConnect.name}</span>
                </Link>
                <ul className={`list-unstyled ${showConnects ? 'show' : 'collapse'}`} id="connects">
                  {listConnects}
                </ul>
              </li>
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

export default Sidebar;
