import React, { Component } from 'react';

import { Link, withRouter } from 'react-router-dom';
import { organizeRoles } from '../../utils/converters';
import { get, logout } from '../../utils/api';
import { uriCurrentUser, uriLogout } from '../../utils/endpoints';
import history from '../../utils/history';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
class Header extends Component {
  state = {
    login: sessionStorage.getItem('login')
  };

  async logout() {
    try {
      await logout(uriLogout());
      await get(uriCurrentUser()).then(res => {
        let currentUserData = res.data;
        sessionStorage.setItem('login', currentUserData.logged);
        sessionStorage.setItem('user', 'default');
        sessionStorage.setItem('roles', organizeRoles(currentUserData.roles));
        this.setState({ login: currentUserData.logged }, () => {
          this.props.history.replace({
            pathname: '/ui/login',
            ...this.props.history
          });
          window.location.reload(false);
          toast.success('Logged out successfully');
        });
      });
    } finally {
      history.replace({
        loading: false
      });
    }
  }

  render() {
    const { title, children } = this.props;
    const { login } = this.state;

    return (
      <React.Fragment>
        <div
          className="title"
          style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
        >
          {' '}
          <h1>{title}</h1>{' '}
          {login === 'false' || !login ? (
            <Link to="/ui/login">
              <button
                data-turbolinks="false"
                className="btn btn-primary"
                style={{ float: 'right' }}
              >
                {' '}
                <i className="fa fa-fw fa-sign-in" aria-hidden="true" />
                Login
              </button>
            </Link>
          ) : (
            <Link to="#">
              <button
                data-turbolinks="false"
                className="btn btn-primary"
                style={{ float: 'right' }}
                onClick={() => {
                  this.logout();
                }}
              >
                {' '}
                <i className="fa fa-fw fa-sign-in" aria-hidden="true" />
                Logout
              </button>
            </Link>
          )}
          {children}
        </div>
      </React.Fragment>
    );
  }
}

export default withRouter(Header);
