import React, { Component } from 'react';
import { withRouter } from 'react-router-dom';
import Sidebar from '../../containers/SideBar';
import constants from '../../utils/constants';
import SuccessToast from '../../components/Toast/SuccessToast';
import ErrorToast from '../../components/Toast/ErrorToast';
import Loading from '../../containers/Loading';
import { get } from '../../utils/api';
import { uriCurrentUser } from '../../utils/endpoints';
import { organizeRoles } from '../../utils/converters';
import './Base.scss';
import { Helmet } from 'react-helmet';
class Base extends Component {
  state = {
    clusterId: '',
    topicId: '',
    selectedTab: constants.CLUSTER, //cluster | node | topic | tail | group | acls | schema | connect
    action: '',
    showSuccessToast: false,
    successToastMessage: '',
    successToastTimeout: 6000, // in ms
    showErrorToast: false,
    errorToastTitle: '',
    errorToastMessage: '',
    errorToastTimeout: 6000, // in ms
    loading: false,
    expanded: false
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
      errorToastMessage,
      loading,
      tab
    } = nextProps.location;

    return {
      topicId: topicId,
      clusterId: clusterId,
      selectedTab: tab,
      action: action,
      showSuccessToast: showSuccessToast,
      successToastMessage: successToastMessage,
      showErrorToast: showErrorToast,
      errorToastTitle: errorToastTitle,
      errorToastMessage: errorToastMessage,
      loading
    };
  }

  handleTitle() {
    const page = window.location.pathname;
    let title = '';
    if (page.includes('node')) {
      title = 'Nodes |';
    }
    if (page.includes('topic')) {
      title = 'Topics |';
    }
    if (page.includes('tail')) {
      title = 'Live Tail |';
    }
    if (page.includes('group')) {
      title = 'Customer Groups |';
    }
    if (page.includes('acls')) {
      title = 'Acls |';
    }
    if (page.includes('schema')) {
      title = 'Schema Registry |';
    }
    if (page.includes('connect')) {
      title = 'Connect |';
    }

    return title + ' akhq.io';
  }

  componentDidMount() {
    this.checkToasts();
  }

  componentWillUnmount() {
    clearTimeout(this.interval);
  }

  async getCurrentUser() {
    try {
      let currentUserData = await get(uriCurrentUser());
      currentUserData = currentUserData.data;
      if (currentUserData.logged) {
        localStorage.setItem('login', true);
        localStorage.setItem('user', currentUserData.username);
        localStorage.setItem('roles', organizeRoles(currentUserData.roles));
      } else {
        localStorage.setItem('login', false);
        localStorage.setItem('user', 'default');
        localStorage.setItem('roles', organizeRoles(currentUserData.roles));
      }
    } catch (err) {
      console.error('Error:', err);
    }
  }

  checkToasts() {
    const { clusterId } = this.state;

    if (this.state.showSuccessToast) {
      this.interval = setTimeout(() => {
        this.props.history.replace({
          showSuccessToast: false,
          successToastMessage: ''
        });
      }, this.state.successToastTimeout);
    }

    if (this.state.showErrorToast) {
      this.interval = setTimeout(() => {
        this.props.history.replace({
          showErrorToast: false,
          errorToastTitle: '',
          errorToastMessage: ''
        });
      }, this.state.errorToastTimeout);
    }
  }

  render() {
    const { children } = this.props;
    const {
      showSuccessToast,
      showErrorToast,
      successToastMessage,
      errorToastTitle,
      errorToastMessage,
      loading,
      selectedTab,
      expanded
    } = this.state;
    this.checkToasts();
    //if (!localStorage.getItem('user')) {
    this.getCurrentUser();
    //}
    return (
      <>
        <Helmet title={this.handleTitle()} />
        <Loading show={loading} />
        <SuccessToast show={showSuccessToast} message={successToastMessage} />
        <ErrorToast show={showErrorToast} title={errorToastTitle} message={errorToastMessage} />
        {this.props.location.pathname !== '/ui/login' &&
          this.props.location.pathname !== '/ui/page-not-found' && (
            <Sidebar
              expanded={expanded}
              toggleSidebar={newExpanded => {
                this.setState({ expanded: newExpanded });
              }}
              selectedTab={selectedTab}
            />
          )}
        <div id="content" className={expanded ? 'expanded' : 'collapsed'}>
          {children}
        </div>
      </>
    );
  }
}

export default withRouter(Base);
