import React, { Component } from 'react';
import PropTypes from 'prop-types';
import image from '../../images/logo.svg';

class ErrorPage extends Component {
  static propTypes = {
    title: PropTypes.string,
    message: PropTypes.string,
    errorData: PropTypes.object
  };

  render() {
    const { title, message, errorData } = this.props;
    console.log('error?', this.props);
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
