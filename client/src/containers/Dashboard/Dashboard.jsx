import React, { Component } from 'react';

import Sidebar from '../SideBar';
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
