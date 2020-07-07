import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import { baseUrl, uriClusters, uriCurrentUser } from './utils/endpoints';
import Routes from './utils/Routes';
import history from './utils/history';
import api, { get } from './utils/api';
import Loading from '../src/containers/Loading';
import { MuiPickersUtilsProvider } from '@material-ui/pickers';
import MomentUtils from '@date-io/moment';
import { ToastContainer } from 'react-toastify';
import { organizeRoles } from './utils/converters';

class App extends React.Component {
  state = {
    clusterId: '',
    clusters: []
  };

  componentDidMount() {
    if (
      history.location.pathname !== '/ui/login' &&
      history.location.pathname !== '/ui/page-not-found'
    ) {
      api.get(uriClusters()).then(res => {
        console.log('here', res);
        this.setState({ clusterId: res.data ? res.data[0].id : '' }, () => {
          history.replace({
            loading: false
          });
        });
      });
    }
  }

  getCurrentUser(callback = () => {}) {
    get(uriCurrentUser())
      .then(res => {
        let currentUserData = res.data;
        if (currentUserData.logged) {
          sessionStorage.setItem('login', true);
          sessionStorage.setItem('user', currentUserData.username);
          sessionStorage.setItem('roles', organizeRoles(currentUserData.roles));
        } else {
          sessionStorage.setItem('login', false);
          sessionStorage.setItem('user', 'default');
          if (currentUserData.roles) {
            sessionStorage.setItem('roles', organizeRoles(currentUserData.roles));
          } else {
            sessionStorage.setItem('roles', JSON.stringify({}));
          }
        }
        callback();
      })
      .catch(err => {
        console.error('Error:', err);
      });
  }

  render() {
    const { clusterId, clusters } = this.state;
    if (clusterId) {
      this.getCurrentUser();
      return (
        <MuiPickersUtilsProvider utils={MomentUtils}>
          <Router>
            <Routes clusters={clusters} clusterId={clusterId} location={baseUrl} />
            <ToastContainer draggable={false} closeOnClick={false} />
          </Router>
        </MuiPickersUtilsProvider>
      );
    } else {
      return (
        <div>
          <Loading show="true" />
          <ToastContainer draggable={false} />
        </div>
      );
    }
  }
}

export default App;
