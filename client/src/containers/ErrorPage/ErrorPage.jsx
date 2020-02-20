import React, { Component } from 'react';
import PropTypes from 'prop-types';
import image from '../../images/logo.svg';
import { get } from '../../utils/api';
import { uriClusters } from '../../utils/endpoints';

class ErrorPage extends Component {
  static propTypes = {
    title: PropTypes.string,
    message: PropTypes.string,
    status: PropTypes.string
  };

  state = {
    title: '',
    message: '',
    status: ''
  };

  componentDidMount() {
    //this.handleRetry();
    console.log('Error: ', this.props);
    const { data, status } = this.props.history.location.state.errorData.response;
    this.setState({ title: data.title, message: data.description, status });
  }

  async handleRetry() {
    const { history } = this.props;
    try {
      let response = await get(uriClusters());
      if (response.data.length > 0) {
        history.replace(`/${response.data[0].id}/topic`);
      }
    } catch (err) {
      console.error(err);
    }
  }

  render() {
    const { title, message } = this.state;
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
