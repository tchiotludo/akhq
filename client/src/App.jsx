import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import Routes from './utils/AkhqRoutes';
import { ToastContainer } from 'react-toastify';

class App extends React.Component {
  render() {
    return (
      <Router>
        <Routes />
        <ToastContainer draggable={false} closeOnClick={false} theme="dark" />
      </Router>
    );
  }
}

export default App;
