import React from 'react';
import './App.scss';
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
  componentDidMount() {}
  render() {
    const { clusterId } = this.state;
    return <Login></Login>;
  }
}

export default App;
