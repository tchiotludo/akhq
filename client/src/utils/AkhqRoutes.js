import React from 'react';
import PropTypes from 'prop-types';
import { Routes, Route, Navigate } from 'react-router-dom';
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
import TopicIncreaseParition from '../containers/Topic/Topic/TopicPartitions/TopicIncreaseParition';
import TopicCopy from '../containers/Topic/TopicCopy';
import Loading from '../containers/Loading';
import ConsumerGroupList from '../containers/ConsumerGroup/ConsumerGroupList';
import ConsumerGroup from '../containers/ConsumerGroup/ConsumerGroupDetail';
import Schema from '../containers/Schema/SchemaDetail/Schema';
import SchemaList from '../containers/Schema/SchemaList/SchemaList';
import SchemaCreate from '../containers/Schema/SchemaCreate/SchemaCreate';
import ConsumerGroupUpdate from '../containers/ConsumerGroup/ConsumerGroupDetail/ConsumerGroupUpdate';
import ConsumerGroupOffsetDelete from '../containers/ConsumerGroup/ConsumerGroupDetail/ConsumerGroupOffsetDelete';
import AclDetails from '../containers/Acl/AclDetail';
import Login from '../containers/Login';
import Settings from '../containers/Settings/Settings';
import { organizeRoles } from './converters';
import { uriAuths, uriClusters, uriCurrentUser } from './endpoints';
import Root from '../components/Root';
import KsqlDBList from '../containers/KsqlDB/KsqlDBList/KsqlDBList';
import KsqlDBStatement from '../containers/KsqlDB/KsqlDBStatement';
import KsqlDBQuery from '../containers/KsqlDB/KsqlDBQuery';
import sortBy from 'lodash/sortBy';
import { withRouter } from './withRouter';

class AkhqRoutes extends Root {
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

  async getClusters() {
    const { clusterId } = this.state;
    try {
      const resClusters = await this.getApi(uriClusters());
      let sortedClusters = sortBy(resClusters.data || [], cluster => cluster.id);
      this.setState({
        clusters: sortedClusters,
        clusterId: sortedClusters ? sortedClusters[0].id : '',
        loading: false
      });
    } catch (err) {
      if (err.status === 401) {
        if (!clusterId || clusterId.length <= 0) {
          this.setState({ clusterId: '401' });
        }
      }
      console.error('Error:', err);
    }
  }

  _initUserAndAuth() {
    const requests = [this.getApi(uriCurrentUser()), this.getApi(uriAuths())];
    Promise.all(requests)
      .then(data => {
        this._setAuths(data[1]);
        this._setCurrentUser(data[0].data);
      })
      .catch(err => {
        console.error('Error:', err);
      });
  }

  _setAuths(response) {
    if (response.status === 200) {
      sessionStorage.setItem('auths', JSON.stringify(response.data));
      sessionStorage.setItem('version', response.data.version);
    }
  }

  _setCurrentUser(currentUserData) {
    sessionStorage.setItem('user', '');
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
  }

  handleRedirect() {
    let clusterId = this.state.clusterId;
    const roles = JSON.parse(sessionStorage.getItem('roles'));
    if (roles && roles.TOPIC && roles.TOPIC.includes('READ')) return `/ui/${clusterId}/topic`;
    else if (roles && roles.NODE && roles.NODE.includes('READ')) return `/ui/${clusterId}/node`;
    else if (roles && roles.CONSUMER_GROUP && roles.CONSUMER_GROUP.includes('READ'))
      return `/ui/${clusterId}/group`;
    else if (roles && roles.ACL && roles.ACL.includes('READ')) return `/ui/${clusterId}/acls`;
    else if (roles && roles.SCHEMA && roles.SCHEMA.includes('READ'))
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
      this._initUserAndAuth();
      return <></>;
    }

    if (
      (clusterId.length <= 0 || clusterId === '401') &&
      (sessionStorage.getItem('login') === 'true' || this.state.user === 'default')
    ) {
      this.getClusters();
      return <></>;
    }

    if (!clusters.find(el => el.id === this.state.clusterId) && clusterId !== '401') {
      clusterId = clusters[0] || '';
    }

    if (!this.state.loading) {
      if (clusterId) {
        return (
          <Base clusters={clusters}>
            <Routes location={location}>
              <Route exact path="/ui/login" element={<Login />} />
              {roles && roles.TOPIC && roles.TOPIC.includes('READ') && (
                <>
                  <Route exact path="/ui/:clusterId/topic" element={<TopicList />} />

                  <Route
                    exact
                    path="/ui/:clusterId/topic/:topicId/partitions"
                    element={<Topic clusters={clusters} />}
                  />

                  <Route
                    exact
                    path="/ui/:clusterId/topic/:topicId/groups"
                    element={<Topic clusters={clusters} />}
                  />

                  <Route
                    exact
                    path="/ui/:clusterId/topic/:topicId/configs"
                    element={<Topic clusters={clusters} />}
                  />

                  <Route
                    exact
                    path="/ui/:clusterId/topic/:topicId/acls"
                    element={<Topic clusters={clusters} />}
                  />

                  <Route
                    exact
                    path="/ui/:clusterId/topic/:topicId/logs"
                    element={<Topic clusters={clusters} />}
                  />
                </>
              )}
              {roles && roles.TOPIC && roles.TOPIC_DATA.includes('READ') && (
                <Route
                  exact
                  path="/ui/:clusterId/topic/:topicId/data"
                  element={<Topic clusters={clusters} />}
                />
              )}

              {roles && roles.TOPIC && roles.TOPIC.includes('CREATE') && (
                <Route exact path="/ui/:clusterId/topic/create" element={<TopicCreate />} />
              )}
              {roles && roles.TOPIC && roles.TOPIC_DATA.includes('CREATE') && (
                <Route
                  exact
                  path="/ui/:clusterId/topic/:topicId/produce"
                  element={<TopicProduce />}
                />
              )}

              {roles && roles.TOPIC && roles.TOPIC_DATA.includes('CREATE') && (
                <Route
                  exact
                  path="/ui/:clusterId/topic/:topicId/increasepartition"
                  element={<TopicIncreaseParition />}
                />
              )}

              {roles && roles.TOPIC && roles.TOPIC_DATA.includes('CREATE') && (
                <Route exact path="/ui/:clusterId/topic/:topicId/copy" element={<TopicCopy />} />
              )}

              {roles && roles.TOPIC && roles.TOPIC.includes('READ') && (
                <Route
                  exact
                  path="/ui/:clusterId/topic/:topicId/:tab?"
                  render={props => <Topic clusters={clusters} {...props} />}
                />
              )}

              {roles && roles.TOPIC && roles.TOPIC_DATA.includes('READ') && (
                <Route exact path="/ui/:clusterId/tail" element={<Tail />} />
              )}

              {roles && roles.NODE && roles.NODE.includes('READ') && (
                <Route exact path="/ui/:clusterId/node" element={<NodesList />} />
              )}
              {roles && roles.NODE && roles.NODE.includes('READ') && (
                <Route exact path="/ui/:clusterId/node/:nodeId/:tab?" element={<NodeDetails />} />
              )}

              {roles && roles.CONSUMER_GROUP && roles.CONSUMER_GROUP.includes('READ') && (
                <Route exact path="/ui/:clusterId/group" element={<ConsumerGroupList />} />
              )}

              {roles && roles.CONSUMER_GROUP && roles.CONSUMER_GROUP.includes('DELETE_OFFSET') && (
                <Route
                  exact
                  path="/ui/:clusterId/group/:consumerGroupId/offsetsdelete"
                  element={<ConsumerGroupOffsetDelete />}
                />
              )}

              {roles && roles.CONSUMER_GROUP && roles.CONSUMER_GROUP.includes('UPDATE_OFFSET') && (
                <Route
                  exact
                  path="/ui/:clusterId/group/:consumerGroupId/offsets"
                  element={<ConsumerGroupUpdate />}
                />
              )}

              {roles && roles.CONSUMER_GROUP && roles.CONSUMER_GROUP.includes('READ') && (
                <Route
                  exact
                  path="/ui/:clusterId/group/:consumerGroupId/:tab?"
                  element={<ConsumerGroup />}
                />
              )}

              {roles && roles.ACL && roles.ACL.includes('READ') && (
                <Route exact path="/ui/:clusterId/acls" element={<Acls />} />
              )}
              {roles && roles.ACL && roles.ACL.includes('READ') && (
                <Route
                  exact
                  path="/ui/:clusterId/acls/:principalEncoded/:tab?"
                  element={<AclDetails />}
                />
              )}

              {roles && roles.SCHEMA && roles.SCHEMA.includes('READ') && (
                <Route exact path="/ui/:clusterId/schema" element={<SchemaList />} />
              )}
              {roles && roles.SCHEMA && roles.SCHEMA.includes('CREATE') && (
                <Route exact path="/ui/:clusterId/schema/create" element={<SchemaCreate />} />
              )}

              {roles && roles.SCHEMA && roles.SCHEMA.includes('UPDATE') && (
                <Route
                  exact
                  path="/ui/:clusterId/schema/details/:schemaId/update"
                  element={<Schema />}
                />
              )}

              {roles && roles.SCHEMA && roles.SCHEMA.includes('READ') && (
                <Route
                  exact
                  path="/ui/:clusterId/schema/details/:schemaId/:tab?"
                  element={<Schema />}
                />
              )}

              {roles && roles.CONNECTOR && roles.CONNECTOR.includes('CREATE') && (
                <Route
                  exact
                  path="/ui/:clusterId/connect/:connectId/create"
                  element={<ConnectCreate />}
                />
              )}
              {roles && roles.CONNECTOR && roles.CONNECTOR.includes('READ') && (
                <Route exact path="/ui/:clusterId/connect/:connectId" element={<ConnectList />} />
              )}
              {roles && roles.CONNECTOR && roles.CONNECTOR.includes('READ') && (
                <Route
                  exact
                  path="/ui/:clusterId/connect/:connectId/definition/:definitionId/:tab?"
                  element={<Connect />}
                />
              )}
              {roles && roles.KSQLDB && roles.KSQLDB.includes('EXECUTE') && (
                <Route
                  exact
                  path="/ui/:clusterId/ksqldb/:ksqlDBId/query"
                  element={<KsqlDBQuery />}
                />
              )}
              {roles && roles.KSQLDB && roles.KSQLDB.includes('EXECUTE') && (
                <Route
                  exact
                  path="/ui/:clusterId/ksqldb/:ksqlDBId/statement"
                  element={<KsqlDBStatement />}
                />
              )}
              {roles && roles.KSQLDB && roles.KSQLDB.includes('READ') && (
                <Route
                  exact
                  path="/ui/:clusterId/ksqldb/:ksqlDBId/:tab?"
                  element={<KsqlDBList />}
                />
              )}
              <Route exact path="/ui/:clusterId/settings" element={<Settings />} />
              <Route path="/" element={<Navigate to={this.handleRedirect()} />} />
              <Route path="/ui" element={<Navigate to={this.handleRedirect()} />} />
              <Route path="/ui/401" element={<Navigate to={this.handleRedirect()} />} />
            </Routes>
          </Base>
        );
      } else if (sessionStorage.getItem('login') === 'false' && this.state.user !== 'default') {
        return (
          <Routes>
            <Route exact path="/ui/login" element={<Login />} />
            <Route path="/ui" element={<Navigate to="/ui/login" replace />} />
            <Route path="/*" element={<Login />} />
          </Routes>
        );
      }
      return <Loading show />;
    }

    return <Loading show />;
  }
}

export default withRouter(AkhqRoutes);
