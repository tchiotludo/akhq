import React, {Component} from 'react';
import logo from '../../images/logo.svg';
import TabContainer from 'react-bootstrap/TabContainer';
import {Link} from 'react-router-dom';
//import { getBsProps } from "react-bootstrap/lib/utils/bootstrapUtils";
import api from '../../services/api';
import endpoints from '../../services/endpoints';

// Adaptation of template.ftl
class Sidebar extends Component {
  constructor(props) {
    super(props);
    api.get(endpoints.connects).then(result => console.log(result));
   
    this.state = {    };
  }
  async handleGetClusters  () {
    const allClusters = await api.get(endpoints.clusters);
    console.log(allClusters);
  }
  async handleGetConnects (){
    const allConnects = await api.get(endpoints.connects);
    console.log(allConnects);
  }

  render() {
    const{selectedTab, clusterId} = this.props; 
    const tag = 'Snapshot';
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
                  aria-expanded="false"
                  className="dropdown-toggle"
                >
                  <i className="fa fa-fw fa fa-database" aria-hidden="true" />
                  Clusters
                  <span className="badge badge-success">{/*${clusterId}*/}</span>
                </Link>
                <ul className="collapse list-unstyled" id="clusters">
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
                <Link to={`/${this.clusterId}/node`}>
                  <i className="fa fa-fw fa-laptop" aria-hidden="true" /> Nodes
                </Link>
              </li>
              {/*</#if>*/}
              {/*<#if roles?seq_contains("topic") == true>*/}
              {/*    <li className="${(tab == "topic")?then("active", "")}">*/}
              <li className={this.selectedTab === 'topic' ? 'active' : ''}>
                {/*<a href="${basePath}/${clusterId}/topic">
            <i className="fa fa-fw fa-list" aria-hidden="true"></i> Topic</a>*/}
                <Link to={`/${this.clusterId}/topic`}>
                  <i className="fa fa-fw fa-list" aria-hidden="true" /> Topics
                </Link>
              </li>
              {/*</#if>*/}
              {/*<#if roles?seq_contains("topic/data") == true>*/}
              {/*    <li className="${(tab == "tail")?then("active", "")}">*/}
              <li className={this.selectedTab === 'tail' ? 'active' : ''}>
                <Link to={`/${this.clusterId}/tail`}>
                  <i className="fa fa-fw fa-level-down" aria-hidden="true" /> Live Tail
                </Link>
              </li>
              {/*</#if>*/}
              {/*<#if roles?seq_contains("group") == true>*/}
              {/*    <li className="${(tab == "group")?then("active", "")}">*/}
              <li className={this.selectedTab === 'group' ? 'active' : ''}>
                <Link to={`/${this.clusterId}/group`}>
                  <i className="fa fa-fw fa-object-group" aria-hidden="true" /> Consumer Groups
                </Link>
              </li>
              {/*</#if>*/}
              {/*<#if roles?seq_contains("acls") == true>*/}
              {/*    <li className="${(tab == "acls")?then("active", "")}">*/}
              <li className={this.selectedTab === 'acls' ? 'active' : ''}>
                <Link to={`/${this.clusterId}/acls`}>
                  <i className="fa fa-fw fa-key" aria-hidden="true" /> ACLS
                </Link>
              </li>
              {/*</#if>*/}
              {/* <#if registryEnabled?? && registryEnabled == 
          true && roles?seq_contains("registry") == true>*/}
              {/*    <li className="${(tab == "schema")?then("active", "")}">*/}
              <li className={this.selectedTab === 'schema' ? 'active' : ''}>
                <Link to={`/${this.clusterId}/schema`}>
                  <i className="fa fa-fw fa-cogs" aria-hidden="true" /> Schema Registry
                </Link>
              </li>
              {/*</#if>*/}
              {/*<#if (connectList)??>*/}
              {/*<#if roles?seq_contains("connect") == true && (connectList?size > 0)>*/}
              {/*<li className="${(tab == "connect")?then("active", "")}">*/}
              <li className={this.selectedTab === 'connect' ? 'active' : ''}>
                <Link
                  to={`/${this.clusterId}/connect`}
                  data-toggle="collapse"
                  aria-expanded="false"
                  className="dropdown-toggle"
                >
                  <i className="fa fa-fw fa fa-exchange" aria-hidden="true" /> Connects
                  <span className="badge badge-success">
                    {/*${(connectId??)?then(connectId,"")}*/}
                    connect-1
                  </span>
                </Link>
                <ul className="collapse list-unstyled" id="connects">
                  {/*<#list connectList as connect>*/ this.allConnects}
                  <li>
                    {/*<a href="#">*/}
                    {/*    /!*${connect}*!/*/}
                    {/*    connect-1*/}
                    {/*</a>*/}
                  </li>
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
