import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import { baseUrl } from './utils/endpoints';
import Routes from './utils/Routes';
import { MuiPickersUtilsProvider } from '@material-ui/pickers';
import MomentUtils from '@date-io/moment';
import { ToastContainer } from 'react-toastify';
import { loadProgressBar } from 'axios-progress-bar';

class App extends React.Component {
  componentDidMount() {
    loadProgressBar();
  }

  render() {
    return (
      <MuiPickersUtilsProvider utils={MomentUtils}>
        <Router>
          <Routes location={baseUrl} />
          <ToastContainer draggable={false} closeOnClick={false} />
        </Router>
      </MuiPickersUtilsProvider>
    );
  }
}

export default App;
