import React from 'react';
import './App.scss';
import Dashboard from './container/Dashboard/Dashboard';
import { BrowserRouter as Router, Redirect, Route, Switch } from 'react-router-dom';
import Login from './container/Login/Login';
import Topic from './container/Tab/Tabs/TopicList/Topic/Topic';
import NodesList from '.container/NodesList';

function App() {
  return (
    <Router>
      <Switch>
        <Route path="/login" component={Login} />
        <Route path="/nodes" component={NodesList} />
        <Route path="/:clusterId/:tab" exact component={Dashboard} />
        <Route path="/:clusterId/:tab/:action" exact component={Dashboard} />
        <Redirect from="/:clusterId" exact to="/:clusterId/topic" />
        <Redirect from="/" exact to="/fake-cluster/topic" />
      </Switch>
    </Router>
  );
}

export default App;
