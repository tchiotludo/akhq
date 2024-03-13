import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import Routes from './utils/AkhqRoutes';
import { ToastContainer } from 'react-toastify';
import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';

class App extends React.Component {
  render() {
    return (
      <LocalizationProvider dateAdapter={AdapterDateFns}>
        <Router>
          <Routes />
          <ToastContainer draggable={false} closeOnClick={false} theme="dark" />
        </Router>
      </LocalizationProvider>
    );
  }
}

export default App;
