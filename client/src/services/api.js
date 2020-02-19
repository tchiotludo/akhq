import axios from 'axios';
import history from '../utils/history';
import { useHistory } from 'react-router-dom';
export const get = url =>
  new Promise((resolve, reject) => {
    let error = useHistory();
    axios
      .get(url)
      .then(res => {
        resolve(res);
      })
      .catch(err => {
        console.error('GET', err);
        console.log('history', history);
        history.replace('/error');
        reject();
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
        reject();
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
