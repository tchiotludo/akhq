import axios from 'axios';
import history from './history';

export const get = url =>
  new Promise((resolve, reject) => {
    axios
      .get(url)
      .then(res => {
        resolve(res);
      })
      .catch(err => {
        console.log('err', err);
        if (err.response) {
          console.log('1');
          return history.replace('/error', { errorData: err });
        } else {
          console.log('2');
          return history.replace('/error');
        }
        // reject(err);
      });
  });

export const put = (url, body) =>
  new Promise((resolve, reject) => {
    axios
      .put(url, body)
      .then(res => {
        resolve(res);
      })
      .catch(err => {
        reject(err);
      });
  });

export const post = (url, body) =>
  new Promise((resolve, reject) => {
    axios
      .post(url, body)
      .then(res => {
        resolve(res);
      })
      .catch(err => {
        reject();
      });
  });

export const remove = url =>
  new Promise((resolve, reject) => {
    axios
      .delete(url)
      .then(res => {
        resolve(res);
      })
      .catch(err => {
        reject();
      });
  });

export default { get, put, post, remove };
