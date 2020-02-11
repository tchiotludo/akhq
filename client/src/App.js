import React from 'react';
import './App.scss';
import Dashboard from "./components/dashboard";
import {BrowserRouter as Router, Redirect, Route, Switch} from "react-router-dom";
import Login from "./components/login";
import Topic from "./components/tabs/topic/topic";

function App() {
    return (
        <Router>
            <Switch>
                <Route path="/login" component={Login}/>
                <Route path="/:clusterId/:tab" exact component={Dashboard}/>
                <Route path="/:clusterId/:tab/:action" exact component={Dashboard}/>
                <Redirect from="/:clusterId" exact to="/:clusterId/topic"/>
                <Redirect from="/" exact to="/fake-cluster/topic"/>
            </Switch>
        </Router>
    );
}

export default App;
