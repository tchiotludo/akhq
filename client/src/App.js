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

class App extends React.Component {
  state = {
    clusterId: ''
  };

  componentDidMount() {
    api
      .get(uriClusters())
      .then(res => {
        this.setState({ clusterId: res.data ? res.data[0].id : '' });
      })
      .catch(err => {
        if (err.status === 401) {
          history.replace('/ui/:login');
          this.setState({ clusterId: ':login' });
          return;
        }
        if (err.status === 404) {
          history.replace('/ui/page-not-found', { errorData: err });
          this.setState({ clusterId: 'page-not-found' });
        } else {
          history.replace('/ui/error', { errorData: err });
          this.setState({ clusterId: 'error' });
        }
      });
  }

  render() {
    const { clusterId } = this.state;
    return (
      <MuiPickersUtilsProvider utils={MomentUtils}>
        <Router>
          <ErrorBoundary history={history}>
            <Routes clusterId={clusterId} location={baseUrl} />
          </ErrorBoundary>
        </Router>
      </MuiPickersUtilsProvider>
    );
  }
}

export default App;
