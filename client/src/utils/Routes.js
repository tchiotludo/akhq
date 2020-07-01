import React, { Component } from 'react';
import PropTypes from 'prop-types';
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
import PageNotFound from './../containers/PageNotFound/PageNotFound';
import TopicData from '../containers/TopicList/Topic/TopicData';

class Routes extends Component {
  state = { clusterId: '' };
  static propTypes = {
    location: PropTypes.object,
    history: PropTypes.object,
    clusterId: PropTypes.string
  };

  //componentWillReceiveProps(nextProps) {}

  static getDerivedStateFromProps = nextProps => {
    if (nextProps.location.pathname !== '/ui/error') {
      let routeObject = {
        pathname: nextProps.location.pathname,
        ...nextProps.history.location.state
      };
      localStorage.setItem('lastRoute', JSON.stringify(routeObject));
    }
    let path = window.location.pathname.split('/');
    let clusterId = '';
    if (path.length < 4 || path[2] === '') {
      clusterId = nextProps.clusterId;
    } else {
      clusterId = path[2];
    }
    return { clusterId };
  };

  handleRedirect(clusterId) {
    const roles = JSON.parse(localStorage.getItem('roles'));
    if (roles && roles.topic && roles.topic['topic/read']) return `/ui/${clusterId}/topic`;
    else if (roles && roles.node && roles.node['node/read']) return `/ui/${clusterId}/node`;
    else if (roles && roles.group && roles.group['group/read']) return `/ui/${clusterId}/group`;
    else if (roles && roles.acls && roles.acls['acls/read']) return `/ui/${clusterId}/acls`;
    else if (roles && roles.registry && roles.registry['registry/read'])
      return `/ui/${clusterId}/schema`;
    else if (roles && Object.keys(roles).length > 0) return `/ui/${clusterId}/topic`;
    else return '/ui/login';
  }

  render() {
    const { location } = this.props;
    const roles = JSON.parse(localStorage.getItem('roles')) || {};
    let path = window.location.pathname.split('/');

    let clusterId = this.state.clusterId;

    if (path[2] === 'error' || clusterId.length <= 0) {
      return (
        <Switch>
          <Route exact path="/ui/error" component={ErrorPage} />
        </Switch>
      );
    }
    if (path[2] === 'page-not-found') {
      return (
        <Switch>
          <Route exact path="/ui/page-not-found" component={PageNotFound} />
        </Switch>
      );
    }

    if (path[2] === ':login') {
      return (
        <Switch>
          <Route exact path="/ui/:login" component={Login} />
        </Switch>
      );
    }

    if (path.length > 0) {
      return (
        <Base>
          <Switch location={location}>
            <Route exact path="/ui/:login" component={Login} />
            <Route exact path="/ui/page-not-found" component={PageNotFound} />
            {roles && roles.topic && roles.topic['topic/read'] && (
              /*<Route exact path="/page-not-found" component={PageNotFound} /> */
              <Route exact path="/ui/:clusterId/topic" component={TopicList} />
            )}
            {roles && roles.topic && roles.topic['topic/insert'] && (
              <Route exact path="/ui/:clusterId/topic/create" component={TopicCreate} />
            )}
            {roles && roles.topic && roles.topic['topic/data/insert'] && (
              <Route exact path="/ui/:clusterId/topic/:topicId/produce" component={TopicProduce} />
            )}

            {roles && roles.node && roles.node['node/read'] && (
              <Route exact path="/ui/:clusterId/node" component={NodesList} />
            )}
            {roles && roles.node && roles.node['node/read'] && (
              <Route exact path="/ui/:clusterId/node/:nodeId/:tab?" component={NodeDetails} />
            )}
            {roles && roles.group && roles.group['group/read'] && (
              <Route exact path="/ui/:clusterId/group" component={ConsumerGroupList} />
            )}
            {roles && roles.group && roles.group['group/read'] && (
              <Route exact path="/ui/:clusterId/group/:consumerGroupId" component={ConsumerGroup} />
            )}
            {roles && roles.group && roles.group['group/offsets/update'] && (
              <Route
                exact
                path="/ui/:clusterId/group/:consumerGroupId/offsets"
                component={ConsumerGroupUpdate}
              />
            )}
            {roles && roles.topic && roles.topic['topic/data/read'] && (
              <Route exact path="/ui/:clusterId/tail" component={Tail} />
            )}
            {roles && roles.acls && roles.acls['acls/read'] && (
              <Route exact path="/ui/:clusterId/acls" component={Acls} />
            )}
            {roles && roles.acls && roles.acls['acls/read'] && (
              <Route exact path="/ui/:clusterId/acls/:principalEncoded" component={AclDetails} />
            )}
            {roles && roles.registry && roles.registry['registry/read'] && (
              <Route exact path="/ui/:clusterId/schema" component={SchemaList} />
            )}
            {roles && roles.registry && roles.registry['registry/insert'] && (
              <Route exact path="/ui/:clusterId/schema/create" component={SchemaCreate} />
            )}
            {roles && roles.registry && roles.registry['registry/read'] && (
              <Route
                exact
                path="/ui/:clusterId/schema/details/:schemaId/:tab?"
                component={Schema}
              />
            )}
            {roles && roles.connect && roles.connect['connect/insert'] && (
              <Route
                exact
                path="/ui/:clusterId/connect/:connectId/create"
                component={ConnectCreate}
              />
            )}
            {roles && roles.connect && roles.connect['connect/read'] && (
              <Route exact path="/ui/:clusterId/connect/:connectId" component={ConnectList} />
            )}
            {roles && roles.connect && roles.connect['connect/update'] && (
              <Route
                exact
                path="/ui/:clusterId/connect/:connectId/definition/:definitionId/:tab?"
                component={Connect}
              />
            )}
            <Redirect from="/" to={this.handleRedirect(clusterId)} />
            <Redirect from="/ui" to={this.handleRedirect(clusterId)} />
          </Switch>
        </Base>
      );
    }
    return <Loading show />;
  }
}

export default withRouter(Routes);
