import React, { Component } from 'react';
import PropTypes from 'prop-types';
import image from '../../images/logo.svg';
import { get } from '../../utils/api';
import { uriClusters } from '../../utils/endpoints';
import history from '../../utils/history';
import ErrorPage from '../../containers/ErrorPage';

class ErrorBoundary extends Component {
  static propTypes = {
    children: PropTypes.element
  };
  state = {
    hasError: false,
    error: null,
    info: null
  };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    this.setState({ error, info, hasError: true });
  }

  componentWillReceiveProps(nextProps) {
    if (window.location.pathname === '/ui/error') {
      this.setState({ hasError: true });
    } else {
      this.setState({ hasError: false });
    }
  }

  /**
   * If there will be a reload button, use this at onClick: window.location.reload()
   */

  render() {
    const { error, info, hasError } = this.state;
    const { children } = this.props;
    if (hasError) {
      return <ErrorPage history={this.props.history} />;
    }
    return children;
  }
}

export default ErrorBoundary;
