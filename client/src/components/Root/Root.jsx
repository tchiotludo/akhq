import { Component } from 'react';
import axios from 'axios';
import { get, put, post, remove } from '../../utils/api';

class Root extends Component {
  cancel = axios.CancelToken.source();

  componentWillUnmount() {
    /* eslint-disable react/prop-types */
    const pathname = this.props.location?.pathname;

    if (pathname && pathname !== '/ui/login') {
      sessionStorage.setItem('returnTo', pathname + (window.location.search || ''));
    }

    this.cancelAxiosRequests();
  }

  cancelAxiosRequests() {
    if (this.cancel !== undefined) {
      this.cancel.cancel('cancel all');
    }
  }

  renewCancelToken() {
    this.cancel = axios.CancelToken.source();
  }

  getApi(url) {
    return get(url, this.buildConfig());
  }
  postApi(url, body) {
    return post(url, body, this.buildConfig());
  }
  putApi(url, body) {
    return put(url, body, this.buildConfig());
  }
  removeApi(url, body) {
    return remove(url, body, this.buildConfig());
  }

  buildConfig() {
    let config = new Map();
    config.cancelToken = this.cancel.token;

    if (localStorage.getItem('jwtToken')) {
      config.headers = {};
      config.headers['Authorization'] = 'Bearer ' + localStorage.getItem('jwtToken');
    }

    return config;
  }
}

export default Root;
