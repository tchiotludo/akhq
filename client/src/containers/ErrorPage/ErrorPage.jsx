import React, { Component } from 'react';
import PropTypes from 'prop-types';
import image from '../../images/logo.svg';
import Sidebar from '../../containers/SideBar';

class ErrorPage extends Component {
  static propTypes = {
    children: PropTypes.element
  };

  state = {
    status: null,
    message: null
  };

  onUnload() {
    sessionStorage.setItem('reload', true);
  }

  handleHide = () => {
    this.setState({
      display: 'none'
    });
  };

  componentDidMount() {
    window.addEventListener('beforeunload', this.onUnload);
    let errorData = null;
    if (sessionStorage.getItem('reload') === 'true') {
      this.props.history.push(JSON.parse(sessionStorage.getItem('lastRoute')));
      sessionStorage.setItem('reload', false);
    } else {
      if (this.props.location && this.props.location.state) {
        errorData = this.props.location.state.errorData;
      }
      if (errorData) {
        let { status, message } = errorData;
        this.setState({ status, message });
      }
    }
    return <Sidebar onLoaded={this.handleHide} />;
  }

  /**
   * If there will be a reload button, use this at onClick: window.location.reload()
   */

  render() {
    const { status, message } = this.state;
    return (
      <div className="no-side-bar" style={{ height: window.innerHeight - 100 }}>
        <div className="mb-5">
          <h3 className="logo">
            <img src={image} width={'195.53px'} height={'63px'} alt="" />
          </h3>
        </div>
        <div style={{ width: '100%' }}>
          <code>{'Error: ' + status || ''}</code>
          <br />
          <br />
          <pre>
            <code>{message || 'Something went wrong.'}</code>
          </pre>
        </div>
      </div>
    );
  }
}

export default ErrorPage;
