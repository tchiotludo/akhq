import React, { Component } from 'react';

import logo from '../../images/logo.svg';
import { BrowserRouter as Router } from 'react-router-dom';
import { baseUrl, uriClusters } from '../../utils/endpoints';
import Routes from '../../utils/Routes';
import history from '../../utils/history';
import api from '../../utils/api';
import ErrorBoundary from '../../containers/ErrorBoundary';
import Loading from '../../containers/Loading';
import { MuiPickersUtilsProvider } from '@material-ui/pickers';
import MomentUtils from '@date-io/moment';
import lkl from '../../';
// Adaptation of login.ftl

class Login extends Component {
  state = {
    clusterId: ''
  };
  onLogin() {
    let { clusterId } = this.state;
    api
      .get(uriClusters())
      .then(res => {
        //this.setState({ clusterId: res.data ? res.data[0].id : '' });
        localStorage.setItem('login', 'true');
        this.props.history.push({ pathname: `/${res.data && res.data[0].id}/topic` });
      })
      .catch(err => {
        history.replace('/error', { errorData: err });
        this.setState({ clusterId: '' });
      });
  }
  render() {
    return (
      <div className="wrapper" style={{ height: window.innerHeight - 100 }}>
        <div className="no-side-bar">
          <main>
            <form
              className="khq-login"
              onSubmit={e => {
                e.preventDefault();
                this.onLogin();
              }}
            >
              <div>
                <h3 className="logo">
                  <img src={logo} alt="" />
                  <sup>
                    <strong>HQ</strong>
                  </sup>
                </h3>
              </div>

              <div className="input-group mb-3">
                <div className="input-group-prepend">
                  <span className="input-group-text">
                    <i className="fa fa-user" />
                  </span>
                </div>
                <input
                  type="text"
                  name="username"
                  className="form-control"
                  placeholder="Username"
                  aria-label="Username"
                  required=""
                  autoFocus=""
                />
              </div>

              <div className="input-group mb-3">
                <div className="input-group-prepend">
                  <span className="input-group-text">
                    <i className="fa fa-lock" />
                  </span>
                </div>
                <input
                  type="password"
                  name="password"
                  className="form-control"
                  placeholder="Password"
                  aria-label="Password"
                  required=""
                />
              </div>

              <div className="form-group text-right">
                <input type="submit" value="Login" className="btn btn-primary btn-lg" />
              </div>
            </form>
          </main>
        </div>
      </div>
    );
  }
}

export default Login;
