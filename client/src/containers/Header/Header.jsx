import React, { Component } from 'react';

import { Link, withRouter } from 'react-router-dom';
import { responsiveFontSizes } from '@material-ui/core';
import { organizeRoles } from '../../utils/converters';
import { get } from '../../utils/api';
import { uriLogout } from '../../utils/endpoints';

class Header extends Component {
  state = {
    login: localStorage.getItem('login')
  };

  async logout() {
    try {
      await get(uriLogout()).then(res => {
        let currentUserData = res.data;

        localStorage.setItem('login', currentUserData.logged);
        localStorage.setItem('user', 'default');
        localStorage.setItem('roles', organizeRoles(currentUserData.roles));

        this.setState({ login: currentUserData.logged });

        this.setState({ login: currentUserData.logged }, () => {
          this.props.history.replace({
            ...this.props.history,
            showSuccessToast: true,
            successToastMessage: 'Logged out successfully'
          });
          window.location.reload(false);
        });
      });
    } catch (err) {
      this.props.history.replace('/error', { errorData: err });
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
            <Link to="/login">
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
