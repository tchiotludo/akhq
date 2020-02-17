import React, { Component } from 'react';
import { withRouter, Switch, Route, Redirect } from 'react-router-dom';
import Dashboard from '../containers/Dashboard';
import Login from '../containers/Login/Login';
import Topic from '../containers/Tab/Tabs/TopicList/Topic';
import NodesList from '../containers/NodesList';
import NodeDetails from '../containers/NodeDetails';

class Routes extends Component {
  render() {
    return (
      <Base>
        <Switch location={location}>
          <Route path="/login" component={Login} />
          <Route path="/:clusterId/:tab" exact component={Dashboard} />
          <Route path="/:clusterId/:tab/:action" exact component={Dashboard} />
          <Redirect from="/:clusterId" exact to="/:clusterId/topic" />
          <Redirect from="/" exact to="/my-cluster/topic" />
        </Switch>
      </Base>
    );
  }
}
