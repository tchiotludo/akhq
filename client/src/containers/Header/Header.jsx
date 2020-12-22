import React from 'react';

import { Link, withRouter } from 'react-router-dom';
import { organizeRoles } from '../../utils/converters';
import { logout } from '../../utils/api';
import { uriCurrentUser, uriLogout } from '../../utils/endpoints';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Root from "../../components/Root";

class Header extends Root {
  state = {
    login: sessionStorage.getItem('login'),
    username: sessionStorage.getItem('user'),
    auths: JSON.parse(sessionStorage.getItem('auths')),
    goBack: true
  };

  // unauthorizedGoBack = ['topic', 'node', 'tail', 'group', 'acls', 'schema'];
  //
  // componentDidMount() {
  //   const url = window.location.pathname.split('/');
  //   this.unauthorizedGoBack.forEach(el => {
  //     if ('' === url[url.length - 1] ||  el === url[url.length - 1] || 'connect' === url[url.length - 2]) {
  //       this.setState({ goBack: false });
  //     }
  //   });
  //   this.goBack = this.goBack.bind(this);
  // }

  // goBack() {
  //   this.props.history.goBack();
  // }

  async logout() {
    await logout(uriLogout());
    await this.getApi(uriCurrentUser()).then(res => {
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
  }

  _renderLogin() {
    const { login, username, auths } = this.state;
    if(auths && auths.loginEnabled) {
        return (login === 'false' || !login ? (
            <Link to="/ui/login">
              <button className="btn btn-primary">
                {' '}
                <i className="fa fa-fw fa-sign-in" aria-hidden="true"/>
                Login
              </button>
            </Link>
        ) : (
            <Link to="#">
              <button
                  className="btn btn-primary"
                  onClick={() => {
                    this.logout();
                  }}
              >
                {' '}
                <i className="fa fa-fw fa-sign-in" aria-hidden="true"/>
                {username} (Logout)
              </button>
            </Link>
        ));
    } else {
      return (<></>);
    }
  }

  render() {
    const { title, children } = this.props;
    return (
      <React.Fragment>
        <div
          className="title"
          style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
        >
          {' '}
          <h1>{title}</h1>{' '}
          <div>
            {this._renderLogin()}
            {children}
          </div>
        </div>
      </React.Fragment>
    );
  }
}

export default withRouter(Header);
