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

  render() {
    const {selectedTab, clusterId} = this.props;
    const {
      selectedConnect,
      selectedCluster,
      allClusters,
      allConnects,
      showClusters,
      showConnects
    } = this.state;
    const tag = 'Snapshot';
    const listClusters = allClusters.map(cluster => (
      <li key={cluster.id} className="list-group-item">
        {cluster.id}
      </li>
    ));
    const listConnects = allConnects.map(connect => (
      <li key={connect.name} className="list-group-item">
        {connect.name}
      </li>
    ));

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
              {/*if that checks if the selected tab is "cluster"*/}
              {/*<li className="${(tab == " cluster")?then(" active", "")}">*/}
              <li className={selectedTab === 'cluster' ? 'active' : ''}>
                <Link
                  to={`/${clusterId}/topic`}
                  data-toggle="collapse"
                  aria-expanded={showClusters}
                  className="dropdown-toggle"
                  onClick={() => {
                    this.setState({showClusters: !showClusters});
                  }}
                >
                  <i className="fa fa-fw fa fa-database" aria-hidden="true" />
                  Clusters
                  <span className="badge badge-success">
                    {/*${(connectId??)?then(connectId,"")}*/}
                    {selectedCluster}
                  </span>
                </Link>
                <ul className={`list-unstyled ${showClusters ? 'show' : 'collapse'}`} id="clusters">
                  {listClusters}
                  {/*array*/}
                  {/*for loop that dis*/}
                  {/*<#list clusters as cluster> allClusters.map( cluster =>
                 <li key={cluster._id} className="list-group-item"></li>)*/}
                  <li>
                    {/*if that checks if a cluster is selected */}
                    {/*className="${(cluster == clusterId)?then("active", "")}"*/}
                    <a href="#">{/*${cluster}*/}</a>
                  </li>
                  {/*</#list>*/}
                </ul>
              </li>
              {/*<#if roles?seq_contains("node") == true>*/}
              {/*    <li className="${(tab == "node")?then("active", "")}">*/}
              <li className={this.selectedTab === 'node' ? 'active' : ''}>
                <Link to={`/${selectedCluster}/node`}>
                  <i className="fa fa-fw fa-laptop" aria-hidden="true" /> Nodes
                </Link>
              </li>
              {/*</#if>*/}
              {/*<#if roles?seq_contains("topic") == true>*/}
              {/*    <li className="${(tab == "topic")?then("active", "")}">*/}
              <li className={this.selectedTab === 'topic' ? 'active' : ''}>
                {/*<a href="${basePath}/${clusterId}/topic">
            <i className="fa fa-fw fa-list" aria-hidden="true"></i> Topic</a>*/}
                <Link to={`/${selectedCluster}/topic`}>
                  <i className="fa fa-fw fa-list" aria-hidden="true" /> Topics
                </Link>
              </li>
              {/*</#if>*/}
              {/*<#if roles?seq_contains("topic/data") == true>*/}
              {/*    <li className="${(tab == "tail")?then("active", "")}">*/}
              <li className={this.selectedTab === 'tail' ? 'active' : ''}>
                <Link to={`/${selectedCluster}/tail`}>
                  <i className="fa fa-fw fa-level-down" aria-hidden="true" /> Live Tail
                </Link>
              </li>
              {/*</#if>*/}
              {/*<#if roles?seq_contains("group") == true>*/}
              {/*    <li className="${(tab == "group")?then("active", "")}">*/}
              <li className={this.selectedTab === 'group' ? 'active' : ''}>
                <Link to={`/${selectedCluster}/group`}>
                  <i className="fa fa-fw fa-object-group" aria-hidden="true" /> Consumer Groups
                </Link>
              </li>
              {/*</#if>*/}
              {/*<#if roles?seq_contains("acls") == true>*/}
              {/*    <li className="${(tab == "acls")?then("active", "")}">*/}
              <li className={this.selectedTab === 'acls' ? 'active' : ''}>
                <Link to={`/${selectedCluster}/acls`}>
                  <i className="fa fa-fw fa-key" aria-hidden="true" /> ACLS
                </Link>
              </li>
              {/*</#if>*/}
              {/* <#if registryEnabled?? && registryEnabled == 
          true && roles?seq_contains("registry") == true>*/}
              {/*    <li className="${(tab == "schema")?then("active", "")}">*/}
              <li className={this.selectedTab === 'schema' ? 'active' : ''}>
                <Link to={`/${selectedCluster}/schema`}>
                  <i className="fa fa-fw fa-cogs" aria-hidden="true" /> Schema Registry
                </Link>
              </li>
              {/*</#if>*/}
              {/*<#if (connectList)??>*/}
              {/*<#if roles?seq_contains("connect") == true && (connectList?size > 0)>*/}
              {/*<li className="${(tab == "connect")?then("active", "")}">*/}
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
                  <span className="badge badge-success">
                    {/*${(connectId??)?then(connectId,"")}*/}
                    {selectedConnect.name}
                  </span>
                </Link>
                <ul className={`list-unstyled ${showConnects ? 'show' : 'collapse'}`} id="connects">
                  {/*<#list connectList as connect>*/}

                  {listConnects}
                  {/*<a href="#">*/}
                  {/*    /!*${connect}*!/*/}
                  {/*    connect-1*/}
                  {/*</a>*/}

                  {/*</#list>*/}
                </ul>
              </li>
              {/*</#if>*/}
              {/*</#if>*/}
            </ul>
            {/*<#if loginEnabled>*/}
            <div className="sidebar-log">
              {/*<#if username??>*/}
              {/*<a href="${basePath}/logout" data-turbolinks="false">*/}
              {/*    <i className="fa fa-fw fa-sign-out" aria-hidden="true"></i>*/}
              {/*    ${username} */}
              {/*    (Logout)*/}
              {/*</a>*/}
              {/*<#else>*/}
              <Link to="/login" data-turbolinks="false">
                <i className="fa fa-fw fa-sign-in" aria-hidden="true" />
                Login
              </Link>
              {/*</#if>*/}
              {/*</a>*/}
            </div>
            {/*</#if>*/}
          </nav>
        </TabContainer>
      </div>
    );
  }
}

export default Sidebar;
