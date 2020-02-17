import React, { Component } from 'react';
import { withRouter, Switch, Route, Redirect } from 'react-router-dom';
import Dashboard from '../containers/Dashboard/Dashboard';
import Login from '../containers/Login/Login';
import NodesList from '../containers/NodesList/NodesList';
import Base from '../components/Base/Base.jsx';

const Routes = ({ location }) => {
  return (
    <Base>
      <Switch location={location}>
        <Route exact path="/" component={NodesList} />
        <Route component={NodesList} />
      </Switch>
    </Base>
  );
};

export default withRouter(Routes);
