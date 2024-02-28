import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import { basePath } from './utils/endpoints';
import Routes from './utils/AkhqRoutes';
import { ToastContainer } from 'react-toastify';
//import { loadProgressBar } from 'axios-progress-bar';
import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';

class App extends React.Component {
  componentDidMount() {
    //loadProgressBar();
  }

  render() {
    return (
      <LocalizationProvider dateAdapter={AdapterDateFns}>
        <Router basename={basePath}>
          <Routes />
          <ToastContainer draggable={false} closeOnClick={false} />
        </Router>
      </LocalizationProvider>
    );
  }
}

export default App;
