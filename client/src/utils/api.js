import axios from 'axios';
import history from './history';

const configs = {
  withCredentials: true
};

const handleError = err => {
  if (err.response && err.response.status === 404) {
    history.replace('/page-not-found', { errorData: err });
    return err;
  } else {
    history.replace('/error', { errorData: err });
    return err;
  }
};

export const get = url =>
  new Promise((resolve, reject) => {
    axios
      .get(url, configs)
      .then(res => {
        resolve(res);
      })
      .catch(err => {
        reject(handleError(err));
      });
  });

export const put = (url, body) =>
  new Promise((resolve, reject) => {
    axios
      .put(url, body, configs)
      .then(res => {
        resolve(res);
      })
      .catch(err => {
        reject(handleError(err));
      });
  });

export const post = (url, body) =>
  new Promise((resolve, reject) => {
    axios
      .post(url, body, configs)
      .then(res => {
        resolve(res);
      })
      .catch(err => {
        reject(handleError(err));
      });
  });

export const remove = (url, body) =>
  new Promise((resolve, reject) => {
    axios
      .delete(url, { ...configs, data: body })
      .then(res => {
        resolve(res);
      })
      .catch(err => {
        reject(handleError(err));
      });
  });

export default { get, put, post, remove };
