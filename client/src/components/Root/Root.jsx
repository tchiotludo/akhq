import {Component} from 'react';
import axios from 'axios';
import { get, put, post, remove } from '../../utils/api';

class Root extends Component {

  cancel = axios.CancelToken.source();

  componentWillUnmount() {
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
    return get(url, {cancelToken: this.cancel.token})
  }
  postApi(url, body) {
    return post(url, body,{cancelToken: this.cancel.token})
  }
  putApi(url, body) {
    return put(url, body,{cancelToken: this.cancel.token})
  }
  removeApi(url, body) {
    return remove(url, body, {cancelToken: this.cancel.token})
  }

}

export default Root;
