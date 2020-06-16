import React, { Component } from 'react';
import PropTypes from 'prop-types';
import image from '../../images/logo.svg';
import history from '../../utils/history';
import Sidebar from '../../containers/SideBar';

class ErrorPage extends Component {
  static propTypes = {
    children: PropTypes.element
  };

  state = {
    title: null,
    description: null
  };

  onUnload() {
    localStorage.setItem('reload', true);
  }

  handleHide = () => {
    this.setState({
      display: 'none'
    });
  };

  componentDidMount() {
    window.addEventListener('beforeunload', this.onUnload);
    let errorData = {};
    if (localStorage.getItem('reload') === 'true') {
      this.props.history.push(JSON.parse(localStorage.getItem('lastRoute')));
      localStorage.setItem('reload', false);
    } else {
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
    return (<Sidebar onLoaded={this.handleHide} />);
  }

  /**
   * If there will be a reload button, use this at onClick: window.location.reload()
   */

  render() {
    const { title, description } = this.state;
    return (
      <div className="no-side-bar" style={{ height: window.innerHeight - 100 }}>
        <div className="mb-5">
          <h3 className="logo">
            <img src={image} width={'195.53px'} height={'63px'}  alt="" />
          </h3>
        </div>
        <div style={{ width: '100%' }}>
          <code>{title || 'Error'}</code>
          <br />
          <br />
          <pre>
            <code>{description || 'Something went wrong.'}</code>
          </pre>
        </div>
      </div>
    );
  }
}

export default ErrorPage;
