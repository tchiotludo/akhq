import React, { Component } from 'react';

import logo from '../../images/logo.svg';
import { BrowserRouter as Router } from 'react-router-dom';
import { baseUrl, uriClusters, uriLogin, uriCurrentUser } from '../../utils/endpoints';
import { organizeRoles } from '../../utils/converters';
import Routes from '../../utils/Routes';
import history from '../../utils/history';
import { get, post } from '../../utils/api';
import ErrorBoundary from '../../containers/ErrorBoundary';
import Loading from '../../containers/Loading';
import { MuiPickersUtilsProvider } from '@material-ui/pickers';
import MomentUtils from '@date-io/moment';
import lkl from '../../';
import Form from '../../components/Form/Form';
import Joi from 'joi-browser';
// Adaptation of login.ftl

class Login extends Form {
  state = {
    clusterId: '',
    formData: {
      username: '',
      password: ''
    },
    errors: {}
  };

  schema = {
    username: Joi.string()
      .required()
      .label('Username'),
    password: Joi.string()
      .required()
      .label('Password')
  };

  async login() {
    const { formData } = this.state;

    history.push({
      loading: true
    });
    try {
      const body = {
        username: formData.username,
        password: formData.password
      };

      const response = await post(uriLogin(), body);
      const res = await get(uriCurrentUser());
      const currentUserData = res.data;

      if (currentUserData.logged) {
        localStorage.setItem('login', true);
        localStorage.setItem('user', currentUserData.username);
        localStorage.setItem('roles', organizeRoles(currentUserData.roles));
        this.props.history.push({
          ...this.props.history,
          pathname: '/',
          showSuccessToast: true,
          successToastMessage: `User '${currentUserData.username}' logged in successfully`,
          loading: false
        });
      } else {
        this.props.history.replace({
          ...this.props.history,
          showErrorToast: true,
          errorToastTitle: 'Login failed',
          errorToastMessage: 'Invalid credentials',
          loading: false
        });
      }
      window.location.reload(false);
    } catch (err) {
      this.props.history.replace({
        ...this.props.history,
        showErrorToast: true,
        errorToastTitle: 'Login failed',
        errorToastMessage: err.response.message,
        loading: false
      });
    }
  }

  render() {
    const { errors } = this.state;

    return (
      <div style={{ height: window.innerHeight - 100, marginLeft: 0 }}>
        <main>
          <form
            className="khq-login"
            onSubmit={e => {
              e.preventDefault();
              this.login();
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
                onChange={this.handleChange}
              />
              {errors.username && (
                <div id="input-error" className="alert alert-danger mt-1 p-1">
                  {errors.username}
                </div>
              )}
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
                onChange={this.handleChange}
              />
              {errors.password && (
                <div id="input-error" className="alert alert-danger mt-1 p-1">
                  {errors.password}
                </div>
              )}
            </div>

            <div className="form-group text-right">
              <input
                type="submit"
                value="Login"
                className="btn btn-primary btn-lg"
                disabled={this.validate()}
              />
            </div>
          </form>
        </main>
      </div>
    );
  }
}

export default Login;
