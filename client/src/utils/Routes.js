import React, { Component } from 'react';
import { withRouter, Switch, Route, Redirect } from 'react-router-dom';
import TopicList from '../containers/TopicList';
import Topic from '../containers/TopicList/Topic';
import NodesList from '../containers/NodesList/NodesList';
import NodeDetails from '../containers/NodesList/Node';
import Base from '../components/Base/Base.jsx';
import Tail from '../containers/Tail';
import Acls from '../containers/Acls';
import ConnectList from '../containers/ConnectList/ConnectList';
import ConnectCreate from '../containers/ConnectList/ConnectCreate/ConnectCreate';
import Connect from '../containers/ConnectList/Connect/Connect';
import TopicCreate from '../containers/TopicList/TopicCreate/TopicCreate';
import ErrorPage from '../containers/ErrorPage';
import TopicProduce from '../containers/TopicList/Topic/TopicProduce';
import Loading from '../containers/Loading';
import ConsumerGroupList from '../containers/ConsumerGroupList';
import ConsumerGroup from '../containers/ConsumerGroupList/ConsumerGroup';
import Schema from '../containers/SchemaList/Schema/Schema';
import SchemaList from '../containers/SchemaList/SchemaList';
import SchemaCreate from '../containers/SchemaList/SchemaCreate/SchemaCreate';
import ConsumerGroupUpdate from '../containers/ConsumerGroupList/ConsumerGroup/ConsumerGroupUpdate';
import AclDetails from '../containers/Acls/AclDetails';
import Login from '../containers/Login';
class Routes extends Component {
  componentWillReceiveProps(nextProps) {
    if (nextProps.location.pathname !== '/error') {
      let routeObject = {
        pathname: nextProps.location.pathname,
        ...nextProps.history.location.state
      };
      localStorage.setItem('lastRoute', JSON.stringify(routeObject));
    }
  }

  handleRedirect(clusterId) {
    const roles = JSON.parse(localStorage.getItem('roles')) || {};
    if (roles.topic && roles.topic['topic/read']) return `/${clusterId}/topic`;
    else if (roles.node && roles.node['node/read']) return `/${clusterId}/node`;
    else if (roles.group && roles.group['group/read']) return `/${clusterId}/group`;
    else if (roles.acls && roles.acls['acls/read']) return `/${clusterId}/acls`;
    else if (roles.registry && roles.registry['registry/read']) return `/${clusterId}/schema`;
    return `/${clusterId}/topic`;
  }

  render() {
    const { location } = this.props;
    const roles = JSON.parse(localStorage.getItem('roles')) || {};
    let path = window.location.pathname.split('/');

    let clusterId = '';
    clusterId = path[1];
    if (clusterId.length <= 0) {
      clusterId = this.props.clusterId;
    }

    if (path[1] === 'error') {
      return (
        <Switch>
          <Route exact path="/error" component={ErrorPage} />
        </Switch>
      );
    }
    if (clusterId.length > 0) {
      return (
        <Base>
          <Switch location={location}>
            <Route exact path="/:login" component={Login} />
            {roles.topic && roles.topic['topic/read'] && (
              <Route exact path="/:clusterId/topic" component={TopicList} />
            )}
            {roles.topic && roles.topic['topic/insert'] && (
              <Route exact path="/:clusterId/topic/create" component={TopicCreate} />
            )}
            {roles.topic && roles.topic['topic/read'] && (
              <Route exact path="/:clusterId/topic/:topicId" component={Topic} />
            )}
            {roles.topic && roles.topic['topic/data/insert'] && (
              <Route exact path="/:clusterId/topic/:topicId/produce" component={TopicProduce} />
            )}
            {roles.node && roles.node['node/read'] && (
              <Route exact path="/:clusterId/node" component={NodesList} />
            )}
            {roles.node && roles.node['node/read'] && (
              <Route exact path="/:clusterId/node/:nodeId" component={NodeDetails} />
            )}
            {roles.group && roles.group['group/read'] && (
              <Route exact path="/:clusterId/group" component={ConsumerGroupList} />
            )}
            {roles.group && roles.group['group/read'] && (
              <Route exact path="/:clusterId/group/:consumerGroupId" component={ConsumerGroup} />
            )}
            {roles.group && roles.group['group/offsets/update'] && (
              <Route
                exact
                path="/:clusterId/group/:consumerGroupId/offsets"
                component={ConsumerGroupUpdate}
              />
            )}
            {roles.topic && roles.topic['topic/data/read'] && (
              <Route exact path="/:clusterId/tail" component={Tail} />
            )}
            {roles.acls && roles.acls['acls/read'] && (
              <Route exact path="/:clusterId/acls" component={Acls} />
            )}
            {roles.acls && roles.acls['acls/read'] && (
              <Route exact path="/:clusterId/acls/:principalEncoded" component={AclDetails} />
            )}
            {roles.registry && roles.registry['registry/read'] && (
              <Route exact path="/:clusterId/schema" component={SchemaList} />
            )}
            {roles.registry && roles.registry['registry/insert'] && (
              <Route exact path="/:clusterId/schema/create" component={SchemaCreate} />
            )}
            {roles.registry && roles.registry['registry/read'] && (
              <Route exact path="/:clusterId/schema/details/:schemaId" component={Schema} />
            )}
            {roles.connect && roles.connect['connect/insert'] && (
              <Route exact path="/:clusterId/connect/:connectId/create" component={ConnectCreate} />
            )}
            {roles.connect && roles.connect['connect/read'] && (
              <Route exact path="/:clusterId/connect/:connectId" component={ConnectList} />
            )}
            {roles.connect && roles.connect['connect/update'] && (
              <Route
                exact
                path="/:clusterId/connect/:connectId/definition/:definitionId"
                component={Connect}
              />
            )}
            <Redirect from="/" to={this.handleRedirect(clusterId)} />
          </Switch>
        </Base>
      );
    }
    return <Loading show />;
  }
}

export default withRouter(Routes);
