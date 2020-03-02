import React, { Component } from 'react';
import { withRouter, Switch, Route, Redirect } from 'react-router-dom';
import Dashboard from '../containers/Dashboard/Dashboard';
import Login from '../containers/Login/Login';
import TopicList from '../containers/TopicList';
import Topic from '../containers/TopicList/Topic';
import NodesList from '../containers/NodesList/NodesList';
import NodeDetails from '../containers/NodesList/Node';
import Base from '../components/Base/Base.jsx';
import Tail from '../containers/Tail';
import Group from '../containers/Group';
import Acls from '../containers/Acls';
import Schema from '../containers/Schema';
import Connect from '../containers/Connect';
import ErrorBoundary from '../containers/ErrorBoundary';
import api from '../utils/api';
import endpoints from '../utils/endpoints';
import TopicCreate from '../containers/TopicList/TopicCreate/TopicCreate';
import ErrorPage from '../containers/ErrorPage';
import history from '../utils/history';
import TopicProduce from '../containers/TopicList/Topic/TopicProduce';

class Routes extends Component {
  render() {
    const { location, match } = this.props;
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
            <Route exact path="/:clusterId/topic" component={TopicList} />
            <Route exact path="/:clusterId/node" component={NodesList} />
            <Route exact path="/:clusterId/node/:nodeId" component={NodeDetails} />

            <Route exact path="/:clusterId/topic/create" component={TopicCreate} />
            <Route exact path="/:clusterId/topic/:topicId" component={Topic} />
            <Route exact path="/:clusterId/topic/:topicId/produce" component={TopicProduce} />
            <Route exact path="/:clusterId/tail" component={Tail} />
            <Route exact path="/:clusterId/group" component={Group} />
            <Route exact path="/:clusterId/acls" component={Acls} />
            <Route exact path="/:clusterId/schema" component={Schema} />
            <Route exact path="/:clusterId/connect" component={Connect} />
            <Route exact path="/:clusterId/connect/:connectId" component={Connect} />
            {/* <Route exact path="/error" component={ErrorPage} /> */}
            <Redirect from="/" to={`/${clusterId}/topic`} />
          </Switch>
        </Base>
      );
    }
    return <span />;
  }
}
export default withRouter(Routes);
