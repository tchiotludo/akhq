import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import { basePath } from './utils/endpoints';
import Routes from './utils/AkhqRoutes';
import { ToastContainer } from 'react-toastify';

import { ThemeContext } from './context/ThemeContext';

class App extends React.Component {
  render() {
    return (
      <Router basename={basePath}>
        <Routes />
        <ThemeContext.Consumer>
          {({ resolvedTheme }) => (
            <ToastContainer draggable={false} closeOnClick={false} theme={resolvedTheme} />
          )}
        </ThemeContext.Consumer>
      </Router>
    );
  }
}

export default App;
