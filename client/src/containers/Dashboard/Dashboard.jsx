import React, { Component } from 'react';

import Sidebar from '../SideBar';
import Cluster from '../Cluster';
import Tail from '../Tail';
import NodesList from '../NodesList';
import Group from '../Group';
import Acls from '../Acls';
import Connect from '../Connect';
import SchemaList from '../SchemaList/SchemaList';
import TopicList from '../TopicList';
import TopicCreate from '../TopicList/TopicCreate/TopicCreate';
import SuccessToast from '../../components/Toast/SuccessToast';
import ErrorToast from '../../components/Toast/ErrorToast';
class Dashboard extends Component {
  state = {
    clusterId: '',
    showSuccessToast: false,
    successToastMessage: '',
    successToastTimeout: 10000, // in ms
    showErrorToast: false,
    errorToastTitle: '',
    errorToastMessage: '',
    errorToastTimeout: 6000 // in ms
  };

  static getDerivedStateFromProps(nextProps) {
    const clusterId = nextProps.match.params.clusterId;
    const {
      showSuccessToast,
      successToastMessage,
      showErrorToast,
      errorToastTitle,
      errorToastMessage
    } = nextProps.location;

    return {
      clusterId: clusterId,
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
        return <SchemaList data={data} history={this.props.history} />;
      case 'connect':
        return <Connect data={data} history={this.props.history} />;
      default:
        return <Cluster data={data} history={this.props.history} />;
    }
  };

  renderActionTab = () => {
    const { clusterId, selectedTab, action } = this.state;

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
        return <tails clusterId={clusterId} nodeId={action} />;
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
        <Sidebar />
      </React.Fragment>
    );
  }
}

export default Dashboard;
