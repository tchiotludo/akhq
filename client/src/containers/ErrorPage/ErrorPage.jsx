import React, { Component } from 'react';
import PropTypes from 'prop-types';
import image from '../../images/logo.svg';
import history from '../../utils/history';

class ErrorPage extends Component {
  static propTypes = {
    children: PropTypes.element
  };
  state = {
    title: null,
    description: null
  };

  componentDidMount() {
    let errorData = {};
    if (this.props.location && this.props.history.location.state) {
      errorData = this.props.history.location.state.errorData;
    } else if (history.location && history.location.state) {
      errorData = history.location.state.errorData;
    }

    if (errorData.response) {
      let { title, description } = errorData.response.data;
      this.setState({ title, description });
    }
  }

  /**
   * If there will be a reload button, use this at onClick: window.location.reload()
   */

  render() {
    const { title, description } = this.state;
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
        <code>{title || 'Error'}</code>
        <br />
        <br />
        <pre>
          <code>{description || 'Something went wrong.'}</code>
        </pre>
      </div>
    );
  }
}

export default ErrorPage;
