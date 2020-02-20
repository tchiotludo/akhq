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
import ErrorPage from '../containers/ErrorPage';
import api from '../services/api';
import endpoints from '../services/endpoints';

class Routes extends Component {
  state = {
    clusterId: ''
  };

  componentDidMount() {
    api.get(endpoints.uriClusters()).then(res => {
      this.setState({ clusterId: res.data[0].id });
    });
  }

  render() {
    const { location, match, history } = this.props;
    const { clusterId } = this.state;
    let path = location.pathname.split('/');
    console.log('path', path);
    if (path[1] === 'error') {
      //history.push('/error');
      return (
        <Switch>
          <Route exact path="/error" component={ErrorPage} />
        </Switch>
      );
    }

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
          <Redirect
            path="/"
            to={
              match.params.clusterId
                ? '/:clusterId/topic'
                : !match.params.clusterId && clusterId
                ? `/${clusterId}/topic`
                : '/error'
            }
          />
        </Switch>
      </Base>
    );
  }
}
export default withRouter(Routes);
