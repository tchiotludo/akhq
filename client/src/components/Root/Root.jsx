import React, {Component} from 'react';
import axios from 'axios';
import { get, put, post, remove } from '../../utils/api';

class Root extends Component {

  cancel = axios.CancelToken.source();

  componentWillUnmount() {
    console.log('componentWillUnmount');

    if (this.cancel !== undefined) {
      this.cancel.cancel('cancel all');
    }
  }

  getApi(url) {
    return get(url, {cancelToken: this.cancel.token})
  }

}

export default Root;