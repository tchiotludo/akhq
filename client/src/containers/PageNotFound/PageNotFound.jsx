import React, { Component } from 'react';
import image from '../../images/logo.svg';
import Sidebar from '../../containers/SideBar';

class PageNotFound extends Component {
  handleHide = () => {
    this.setState({
      display: 'none'
    });
  };

  componentDidMount() {
    return <Sidebar onLoaded={this.handleHide} />;
  }

  render() {
    const { history } = this.props;

    let path = window.location.pathname.split('/');

    let clusterId = this.props.clusterId;

    return (
      <div className="no-side-bar" style={{ height: window.innerHeight - 100 }}>
        <div className="max-width" style={{ backgroundColor: '#333333', display: 'inline-block' }}>
          <h3 className="logo mt-5">
            <img src={image} width={'195.53px'} height={'63px'} />
          </h3>
          <div className="container mt-5">
            <p>The page you were looking for doesn't exist.</p>
            <p>You may have mistyped the address or the page doesn't exist.</p>
          </div>
          <div class="p-15 mb-4" style={{ display: 'flex' }}>
            <button
              class="btn btn-primary"
              style={{ marginLeft: 'auto' }}
              onClick={() => history.replace('/')}
            >
              Back to home
            </button>
          </div>
        </div>
      </div>
    );
  }
}

export default PageNotFound;
