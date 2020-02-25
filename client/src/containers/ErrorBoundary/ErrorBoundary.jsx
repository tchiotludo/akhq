import React, { Component } from 'react';
import PropTypes from 'prop-types';
import image from '../../images/logo.svg';
import { get } from '../../utils/api';
import { uriClusters } from '../../utils/endpoints';
import history from '../../utils/history';

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
    console.log('derviedStateFromError');
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    console.log('???', error);
    this.setState({ error, info });
  }

  /**
   * If there will be a reload button, use this at onClick: window.location.reload()
   */

  render() {
    const { error, info, hasError } = this.state;
    const { children } = this.props;
    if (hasError) {
      return (
        <div id="content" className="no-side-bar">
          <div className="mb-5">
            <h3 className="logo">
              <img src={image} />
              <sup>
                <strong>HQ</strong>
              </sup>
            </h3>
          </div>
          <code>{''}</code>
          <br />
          <br />
          <pre>
            <code>{''}</code>
          </pre>
        </div>
      );
    }
    return children;
  }
}

export default ErrorBoundary;
