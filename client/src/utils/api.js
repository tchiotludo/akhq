import axios from 'axios';
import history from './history';

const configs = {
  withCredentials: true
};

const handleError = err => {
  let error = {
    status: err.response ? err.response.status : '',
    message:
      err.response && err.response.data && err.response.data.message
        ? err.response.data.message
        : ''
  };
  if (error.status === 401) {
    history.replace('/ui/login');
    window.location.reload(false);
    return error;
  } else if (error.status === 404) {
    history.replace('/ui/page-not-found', { errorData: error });
    return error;
  } else {
    history.replace('/ui/error', { errorData: error });
    return error;
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

export const login = (url, body) => {
  const requestOptions = {
    method: 'POST',
    redirect: 'manual',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  };
  return fetch(url, requestOptions);
};

export const logout = url => {
  const requestOptions = {
    method: 'GET',
    redirect: 'manual'
  };

  new Promise((resolve, reject) => {
    fetch(url, requestOptions)
      .then(response => {
        resolve(response);
      })
      .catch(function(err) {
        reject(handleError(err));
      });
  });
};

export default { get, put, post, remove, login, logout };
