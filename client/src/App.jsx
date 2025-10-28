import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import { basePath } from './utils/endpoints';
import Routes from './utils/AkhqRoutes';
import { ToastContainer } from 'react-toastify';
import { HelmetProvider } from 'react-helmet-async';

class App extends React.Component {
  render() {
    return (
      <HelmetProvider>
        <Router basename={basePath}>
          <Routes />
          <ToastContainer draggable={false} closeOnClick={false} theme="dark" />
        </Router>
      </HelmetProvider>
    );
  }
}

export default App;
