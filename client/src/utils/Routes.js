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

class Routes extends Component {
  componentWillReceiveProps(nextProps) {
    if (nextProps.location.pathname !== '/error') {
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
            <Route exact path="/:clusterId/topic/create" component={TopicCreate} />
            <Route exact path="/:clusterId/topic/:topicId" component={Topic} />
            <Route exact path="/:clusterId/topic/:topicId/produce" component={TopicProduce} />

            <Route exact path="/:clusterId/node" component={NodesList} />
            <Route exact path="/:clusterId/node/:nodeId" component={NodeDetails} />

            <Route exact path="/:clusterId/group" component={ConsumerGroupList} />
            <Route exact path="/:clusterId/group/:consumerGroupId" component={ConsumerGroup} />
            <Route
              exact
              path="/:clusterId/group/:consumerGroupId/offsets"
              component={ConsumerGroupUpdate}
            />

            <Route exact path="/:clusterId/tail" component={Tail} />
            <Route exact path="/:clusterId/acls" component={Acls} />

            <Route exact path="/:clusterId/schema" component={SchemaList} />
            <Route exact path="/:clusterId/schema/create" component={SchemaCreate} />
            <Route exact path="/:clusterId/schema/details/:schemaId" component={Schema} />

            <Route exact path="/:clusterId/connect/create" component={ConnectCreate} />
            <Route exact path="/:clusterId/connect/:connectId" component={ConnectList} />
            <Route exact path="/:clusterId/acls/:aclUser" component={AclDetails} />
            <Route
              exact
              path="/:clusterId/connect/:connectId/definition/:definitionId"
              component={Connect}
            />
            {/* <Route exact path="/error" component={ErrorPage} /> */}
            <Redirect from="/" to={`/${clusterId}/topic`} />
          </Switch>
        </Base>
      );
    }
    return <Loading show />;
  }
}

export default withRouter(Routes);
