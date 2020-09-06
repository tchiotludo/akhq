import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { withRouter, Switch, Route, Redirect } from 'react-router-dom';
import TopicList from '../containers/Topic/TopicList';
import Topic from '../containers/Topic/Topic';
import NodesList from '../containers/Node/NodeList/NodesList';
import NodeDetails from '../containers/Node/NodeDetail';
import Base from '../components/Base/Base.jsx';
import Tail from '../containers/Tail';
import Acls from '../containers/Acl/AclList';
import ConnectList from '../containers/Connect/ConnectList/ConnectList';
import ConnectCreate from '../containers/Connect/ConnectCreate/ConnectCreate';
import Connect from '../containers/Connect/ConnectDetail/Connect';
import TopicCreate from '../containers/Topic/TopicCreate/TopicCreate';
import TopicProduce from '../containers/Topic/TopicProduce';
import Loading from '../containers/Loading';
import ConsumerGroupList from '../containers/ConsumerGroup/ConsumerGroupList';
import ConsumerGroup from '../containers/ConsumerGroup/ConsumerGroupDetail';
import Schema from '../containers/Schema/SchemaDetail/Schema';
import SchemaList from '../containers/Schema/SchemaList/SchemaList';
import SchemaCreate from '../containers/Schema/SchemaCreate/SchemaCreate';
import ConsumerGroupUpdate from '../containers/ConsumerGroup/ConsumerGroupDetail/ConsumerGroupUpdate';
import AclDetails from '../containers/Acl/AclDetail';
import Login from '../containers/Login';

import history from './history';
import api, { get } from './api';
import { organizeRoles } from './converters';
import { uriClusters, uriCurrentUser } from './endpoints';
class Routes extends Component {
  state = {
    clusters: [],
    clusterId: '',
    user: '',
    loading: true
  };
  static propTypes = {
    location: PropTypes.object,
    history: PropTypes.object,
    clusterId: PropTypes.string,
    clusters: PropTypes.array
  };

  getClusters = () => {
    const { clusterId } = this.state;

    api
      .get(uriClusters())
      .then(res => {
        this.setState(
          { clusters: res.data, clusterId: res.data ? res.data[0].id : '', loading: false },
          () => {
            history.replace({
              loading: false
            });
          }
        );
      })
      .catch(err => {
        if (err.status === 401) {
          if (!clusterId || clusterId.length <= 0) {
            this.setState({ clusterId: '401' });
          }
        }
        console.error('Error:', err);
      });
  };

  getCurrentUser(callback = () => {}) {
    sessionStorage.setItem('user', '');
    get(uriCurrentUser())
      .then(res => {
        let currentUserData = res.data;
        if (currentUserData.logged) {
          sessionStorage.setItem('login', true);
          sessionStorage.setItem('user', currentUserData.username);
          sessionStorage.setItem('roles', organizeRoles(currentUserData.roles));
          this.setState({ user: currentUserData.username });
        } else {
          sessionStorage.setItem('login', false);
          if (currentUserData.roles) {
            sessionStorage.setItem('user', 'default');
            sessionStorage.setItem('roles', organizeRoles(currentUserData.roles));
            this.setState({ user: 'default' });
          } else {
            sessionStorage.setItem('user', '');
            sessionStorage.setItem('roles', JSON.stringify({}));
            this.setState({ user: 'not_logged' });
          }
        }
        this.setState({ loading: false });
        callback();
      })
      .catch(err => {
        console.error('Error:', err);
      });
  }

  handleRedirect() {
    let clusterId = this.state.clusterId;
    const roles = JSON.parse(sessionStorage.getItem('roles'));
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
    const clusters = this.state.clusters || [];
    const roles = JSON.parse(sessionStorage.getItem('roles')) || {};
    let clusterId = this.state.clusterId;

    if (this.state.user.length <= 0) {
      this.getCurrentUser();
    }

    if (
      (clusterId.length <= 0 || clusterId === '401') &&
      (sessionStorage.getItem('login') === 'true' || this.state.user === 'default')
    ) {
      this.getClusters();
    }

    if (!clusters.find(el => el.id === this.state.clusterId) && clusterId !== '401') {
      clusterId = clusters[0] || '';
    }

    if (!this.state.loading) {
      if (clusterId) {
        return (
          <Base clusters={clusters}>
            <Switch location={location}>
              <Route exact path="/ui/login" component={Login} />
              {roles && roles.topic && roles.topic['topic/read'] && (
                <Route exact path="/ui/:clusterId/topic" component={TopicList} />
              )}

              {roles && roles.topic && roles.topic['topic/insert'] && (
                <Route exact path="/ui/:clusterId/topic/create" component={TopicCreate} />
              )}
              {roles && roles.topic && roles.topic['topic/data/insert'] && (
                <Route
                  exact
                  path="/ui/:clusterId/topic/:topicId/produce"
                  component={TopicProduce}
                />
              )}
              {roles && roles.topic && roles.topic['topic/read'] && (
                <Route exact path="/ui/:clusterId/topic/:topicId/:tab?" component={Topic} />
              )}

              {roles && roles.topic && roles.topic['topic/data/read'] && (
                  <Route exact path="/ui/:clusterId/tail" component={Tail} />
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
                <Route
                  exact
                  path="/ui/:clusterId/group/:consumerGroupId/:tab?"
                  component={ConsumerGroup}
                />
              )}
              {roles && roles.group && roles.group['group/offsets/update'] && (
                <Route
                  exact
                  path="/ui/:clusterId/group/:consumerGroupId/offsets"
                  component={ConsumerGroupUpdate}
                />
              )}

              {roles && roles.acls && roles.acls['acls/read'] && (
                <Route exact path="/ui/:clusterId/acls" component={Acls} />
              )}
              {roles && roles.acls && roles.acls['acls/read'] && (
                <Route exact path="/ui/:clusterId/acls/:principalEncoded/:tab?" component={AclDetails} />
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
              <Redirect from="/" to={this.handleRedirect()} />
              <Redirect from="/ui" to={this.handleRedirect()} />
              <Redirect from="/ui/401" to={this.handleRedirect()} />
            </Switch>
          </Base>
        );
      } else if (sessionStorage.getItem('login') === 'false' && this.state.user !== 'default') {
        return (
          <Switch>
            <Route exact path="/ui/login" component={Login} />
            <Redirect from="/ui" to={'/ui/login'} />
            <Redirect from="/" to={'/ui/login'} />
          </Switch>
        );
      }
      return <Loading show />;
    }

    return <Loading show />;
  }
}

export default withRouter(Routes);
