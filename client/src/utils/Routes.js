import React, { Component } from 'react';
import { withRouter, Switch, Route, Redirect } from 'react-router-dom';
import Dashboard from '../containers/Dashboard/Dashboard';
import Login from '../containers/Login/Login';
import TopicList from '../containers/TopicList';
import Topic from '../containers/TopicList/Topic';
import NodesList from '../containers/NodesList/NodesList';
import NodeDetails from '../containers/NodeDetails';
import Base from '../components/Base/Base.jsx';
import Tail from '../containers/Tab/Tabs/Tail';
import Group from '../containers/Tab/Tabs/Group';
import Acls from '../containers/Tab/Tabs/Acls';
import Schema from '../containers/Tab/Tabs/Schema';
import Connect from '../containers/Tab/Tabs/Connect';

const Routes = ({ location }) => {
  return (
    <Base>
      <Switch location={location}>
        <Route exact path="/:clusterId/node" component={NodesList} />
        <Route exact path="/:clusterId/node/:nodeId" component={NodeDetails} />
        <Route exact path="/:clusterId/topic" component={TopicList} />
        <Route exact path="/:clusterId/topic/:topicId" component={Topic} />
        <Route exact path="/:clusterId/tail" component={Tail} />
        <Route exact path="/:clusterId/group" component={Group} />
        <Route exact path="/:clusterId/acls" component={Acls} />
        <Route exact path="/:clusterId/schema" component={Schema} />
        <Route exact path="/:clusterId/connect" component={Connect} />
        <Route exact path="/:clusterId/connect/:connectId" component={Connect} />
        <Redirect to="/" component={NodesList} />
      </Switch>
    </Base>
  );
};
export default withRouter(Routes);
