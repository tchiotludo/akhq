import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import { baseUrl, uriClusters } from './utils/endpoints';
import Routes from './utils/Routes';
import history from './utils/history';
import api from './utils/api';
import ErrorBoundary from './containers/ErrorBoundary';
import Loading from '../src/containers/Loading';
import { MuiPickersUtilsProvider } from '@material-ui/pickers';
import MomentUtils from '@date-io/moment';
import Login from '../src/containers/Login';

class App extends React.Component {
  state = {
    clusterId: ''
  };
  componentDidMount() {
    api
      .get(uriClusters())
      .then(res => {
        console.log('here', res.data);
        this.setState({ clusterId: res.data ? res.data[0].id : '' });
      })
      .catch(err => {
        console.log('???', err);
        if (err.response && err.response.status === 404) {
          history.replace('/page-not-found', { errorData: err });
        } else {
          history.replace('/error', { errorData: err });
        }
        this.setState({ clusterId: '' });
      });
  }
  render() {
    const { clusterId } = this.state;
    return (
      <MuiPickersUtilsProvider utils={MomentUtils}>
        <Router history={history}>
          <ErrorBoundary history={history}>
            <Routes clusterId={clusterId} location={baseUrl} />
          </ErrorBoundary>
        </Router>
      </MuiPickersUtilsProvider>
    );
  }
}

export default App;
