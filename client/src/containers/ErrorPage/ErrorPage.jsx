import React, { Component } from 'react';
import PropTypes from 'prop-types';
import image from '../../images/logo.svg';
import { get } from '../../services/api';
import { uriClusters } from '../../services/endpoints';

class ErrorPage extends Component {
  static propTypes = {
    title: PropTypes.string,
    message: PropTypes.string,
    errorData: PropTypes.object
  };

  componentDidMount() {
    this.handleRetry();
  }

  async handleRetry() {
    const { history } = this.props;
    try {
      let response = await get(uriClusters());
      history.push(`/${response.data[0].id}/topic`);
    } catch (err) {}
  }

  render() {
    const { title, message, errorData } = this.props;
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
        <code>{title || ''}</code>
        <br />
        <br />
        <pre>
          <code>{message || ''}</code>
        </pre>
      </div>
    );
  }
}

export default ErrorPage;
