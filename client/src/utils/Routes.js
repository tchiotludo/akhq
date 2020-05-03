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
    if (nextProps.location.pathname !== '/ui/error') {
      let routeObject = {
        pathname: nextProps.location.pathname,
        ...nextProps.history.location.state
      };
      localStorage.setItem('lastRoute', JSON.stringify(routeObject));
    }
  }

  render() {
    const { location } = this.props;
    let path = window.location.pathname.split('/');

    let clusterId = '';
    if(path.length < 4 || path[2] === '') {
      clusterId = this.props.clusterId;
    } else {
      clusterId = path[2];
    }
console.log(path);
    console.log(clusterId);
    if (path[2] === 'error') {
      return (
        <Switch>
          <Route exact path="/ui/error" component={ErrorPage} />
        </Switch>
      );
    }
    if (path.length > 0) {
      return (
        <Base>
          <Switch location={location}>
            <Route exact path="/ui/:clusterId/topic" component={TopicList} />
            <Route exact path="/ui/:clusterId/topic/create" component={TopicCreate} />
            <Route exact path="/ui/:clusterId/topic/:topicId" component={Topic} />
            <Route exact path="/ui/:clusterId/topic/:topicId/produce" component={TopicProduce} />
            <Route exact path="/ui/:login" component={Login} />
            <Route exact path="/ui/:clusterId/node" component={NodesList} />
            <Route exact path="/ui/:clusterId/node/:nodeId" component={NodeDetails} />
            <Route exact path="/ui/:clusterId/group" component={ConsumerGroupList} />
            <Route exact path="/ui/:clusterId/group/:consumerGroupId" component={ConsumerGroup} />
            <Route
              exact
              path="/ui/:clusterId/group/:consumerGroupId/offsets"
              component={ConsumerGroupUpdate}
            />
            <Route exact path="/ui/:clusterId/tail" component={Tail} />
            <Route exact path="/ui/:clusterId/acls" component={Acls} />
            <Route exact path="/ui/:clusterId/schema" component={SchemaList} />
            <Route exact path="/ui/:clusterId/schema/create" component={SchemaCreate} />
            <Route exact path="/ui/:clusterId/schema/details/:schemaId" component={Schema} />
            <Route exact path="/ui/:clusterId/connect/:connectId/create" component={ConnectCreate} />
            <Route exact path="/ui/:clusterId/connect/:connectId" component={ConnectList} />
            <Route exact path="/ui/:clusterId/acls/:principalEncoded" component={AclDetails} />
            <Route
              exact
              path="/ui/:clusterId/connect/:connectId/definition/:definitionId"
              component={Connect}
            />
            <Redirect from="/ui" to={`/ui/${clusterId}/topic`} />
          </Switch>
        </Base>
      );
    }
    return <Loading show />;
  }
}

export default withRouter(Routes);
