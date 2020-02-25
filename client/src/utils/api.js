import axios from 'axios';
import history from './history';

const handleError = err => {
  console.log('error', err);
  return err;
};

export const get = url =>
  new Promise((resolve, reject) => {
    axios
      .get(url)
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
      .put(url, body)
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
      .post(url, body)
      .then(res => {
        resolve(res);
      })
      .catch(err => {
        reject(handleError(err));
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
        reject(handleError(err));
      });
  });

export default { get, put, post, remove };
