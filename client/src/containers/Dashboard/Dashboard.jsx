import React, { Component } from 'react';

import Sidebar from '../SideBar';
import Cluster from '../Tab/Tabs/Cluster';
import Tail from '../Tab/Tabs/Tail';
import NodesList from '../NodesList';
import Group from '../Tab/Tabs/Group';
import Acls from '../Tab/Tabs/Acls';
import Connect from '../Tab/Tabs/Connect';
import Schema from '../Tab/Tabs/Schema';
import TopicList from '../Tab/Tabs/TopicList';
import TopicCreate from '../Tab/Tabs/TopicList/TopicCreate/TopicCreate';
import SuccessToast from '../../components/Toast/SuccessToast';
import ErrorToast from '../../components/Toast/ErrorToast';
import Topic from '../Tab/Tabs/TopicList/Topic';
import NodeDetails from '../NodeDetails';
class Dashboard extends Component {
  state = {
    clusterId: '',
    topicId: '',
    selectedTab: 'cluster', //cluster | node | topic | tail | group | acls | schema | connect
    action: '',
    showSuccessToast: false,
    successToastMessage: '',
    successToastTimeout: 10000, // in ms
    showErrorToast: false,
    errorToastTitle: '',
    errorToastMessage: '',
    errorToastTimeout: 6000 // in ms
  };

  static getDerivedStateFromProps(nextProps, prevState) {
    const clusterId = nextProps.match.params.clusterId;
    const topicId = nextProps.match.params.topicId;
    const selectedTab = nextProps.match.params.tab;
    const action = nextProps.match.params.action;
    const {
      showSuccessToast,
      successToastMessage,
      showErrorToast,
      errorToastTitle,
      errorToastMessage
    } = nextProps.location;

    return {
      topicId: topicId,
      clusterId: clusterId,
      selectedTab: selectedTab,
      action: action,
      showSuccessToast: showSuccessToast,
      successToastMessage: successToastMessage,
      showErrorToast: showErrorToast,
      errorToastTitle: errorToastTitle,
      errorToastMessage: errorToastMessage
    };
  }

  componentDidMount() {
    this.checkToasts();
  }

  componentWillUnmount() {
    clearTimeout(this.interval);
  }

  checkToasts = () => {
    const { clusterId } = this.state;

    if (this.state.showSuccessToast) {
      this.interval = setTimeout(() => {
        this.props.history.push({
          pathname: `/${clusterId}/topic`,
          showSuccessToast: false,
          successToastMessage: ''
        });
      }, this.state.successToastTimeout);
    }

    if (this.state.showErrorToast) {
      this.interval = setTimeout(() => {
        this.props.history.push({
          pathname: `/${clusterId}/topic`,
          showErrorToast: false,
          errorToastTitle: '',
          errorToastMessage: ''
        });
      }, this.state.errorToastTimeout);
    }
  };

  renderSelectedTab = data => {
    const { selectedTab } = this.state;

    this.checkToasts();

    switch (selectedTab) {
      case 'cluster':
        return <Cluster data={data} history={this.props.history} />;
      case 'node':
        return <NodesList data={data} history={this.props.history} />;
      case 'topic':
        return <TopicList data={data} history={this.props.history} />;
      case 'tail':
        return <Tail data={data} history={this.props.history} />;
      case 'group':
        return <Group data={data} history={this.props.history} />;
      case 'acls':
        return <Acls data={data} history={this.props.history} />;
      case 'schema':
        return <Schema data={data} history={this.props.history} />;
      case 'connect':
        return <Connect data={data} history={this.props.history} />;
      default:
        return <Cluster data={data} history={this.props.history} />;
    }
  };

  renderActionTab = () => {
    const { clusterId, selectedTab, action } = this.state;

    console.log('HERE', action);
    // eslint-disable-next-line default-case
    switch (selectedTab) {
      case 'topic':
        switch (action) {
          case 'create':
            return <TopicCreate clusterId={clusterId} />;
          default:
            return <Topic clusterId={clusterId} topicId={action} />;
        }
      case 'node':
        console.log('nodenode', action);
        return <NodeDetails clusterId={clusterId} nodeId={action} />;
      case 'tail':
        break;
      case 'group':
        break;
      case 'acls':
        break;
      case 'schema':
        break;
      case 'connect':
        break;
    }
  };

  render() {
    const {
      selectedTab,
      topicId,
      clusterId,
      action,
      showSuccessToast,
      successToastMessage,
      showErrorToast,
      errorToastTitle,
      errorToastMessage
    } = this.state;

    return (
      <React.Fragment>
        <SuccessToast show={showSuccessToast} message={successToastMessage} />
        <ErrorToast show={showErrorToast} title={errorToastTitle} message={errorToastMessage} />
        <Sidebar selectedTab={selectedTab} clusterId={clusterId} />
        {action !== undefined
          ? this.renderActionTab()
          : this.renderSelectedTab({ clusterId, topicId })}
      </React.Fragment>
    );
  }
}

export default Dashboard;
