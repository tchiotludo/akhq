import React from 'react';

import logo from '../../images/logo.svg';
import {uriCurrentUser, uriLogin, uriOidc} from '../../utils/endpoints';
import { organizeRoles } from '../../utils/converters';
import { login } from '../../utils/api';
import Form from '../../components/Form/Form';
import Joi from 'joi-browser';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
// Adaptation of login.ftl

class Login extends Form {
  state = {
    formData: {
      username: '',
      password: ''
    },
    errors: {},
    config: {
      formEnabled: true,
      oidcAuths: []
    }
  };

  schema = {
    username: Joi.string()
      .required()
      .label('Username'),
    password: Joi.string()
      .required()
      .label('Password')
  };

  login() {
    const { formData } = this.state;

    try {
      const body = {
        username: formData.username,
        password: formData.password
      };

      login(uriLogin(), body)
        .then(() => {
          this.getData();
        });
    } catch (err) {
      toast.error('Wrong Username or Password!');
    }
  }

  async getData() {
    const res = await this.getApi(uriCurrentUser());
    const currentUserData = res.data;

    if (currentUserData.logged) {
      sessionStorage.setItem('login', true);
      sessionStorage.setItem('user', currentUserData.username);
      sessionStorage.setItem('roles', organizeRoles(currentUserData.roles));

      this.props.history.push({
        pathname: '/ui',
      });
      window.location.reload(true);
    } else {
      toast.error('Wrong Username or Password!');
    }
  }

  componentDidMount() {
    const auths = JSON.parse(sessionStorage.getItem('auths'));
    if(auths && auths.loginEnabled) {
      const {loginEnabled, ...config} = auths;
      this.setState({config});
    } else {
      this.props.history.push({
        pathname: '/ui',
      });
    }
  }

  _renderForm() {
    const { errors } = this.state;
    return (
      <>
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
      </>
    );
  }

  _renderSeparator() {
    return (
      <div className="khq-login-separator">
        <span>or</span>
      </div>
    );
  }

  _renderOidc(oidcsAuths) {
    return oidcsAuths.map(auth => (
      <a href={uriOidc(auth.key)} className="btn btn-primary btn-block">{auth.label}</a>
    ));
  }

  render() {
    const { formEnabled, oidcAuths } = this.state.config;

    return (
      <div>
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
              </h3>
            </div>
            {formEnabled && this._renderForm()}
            {formEnabled && oidcAuths && this._renderSeparator()}
            {oidcAuths && this._renderOidc(oidcAuths)}
          </form>
        </main>
      </div>
    );
  }
}

export default Login;
