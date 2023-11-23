import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import { basePath, uriLogin } from './utils/endpoints';
import Routes from './utils/Routes';
import { MuiPickersUtilsProvider } from '@material-ui/pickers';
import MomentUtils from '@date-io/moment';
import { ToastContainer } from 'react-toastify';
import { loadProgressBar } from 'axios-progress-bar';
import axios from 'axios';

class App extends React.Component {
  componentDidMount() {
    loadProgressBar();

    axios.interceptors.response.use(null, error => {
      try {
        // Used for token expiration by redirecting on login page
        if (error.response.status === 401) {
          window.location = uriLogin();
        }

        return Promise.reject(error);
      } catch (e) {
        // Propagate cancel
        if (axios.isCancel(error)) {
          throw new axios.Cancel('Operation canceled by the user.');
        }
      }
    });
  }

  render() {
    return (
      <MuiPickersUtilsProvider utils={MomentUtils}>
        <Router basename={basePath}>
          <Routes />
          <ToastContainer draggable={false} closeOnClick={false} />
        </Router>
      </MuiPickersUtilsProvider>
    );
  }
}

export default App;
