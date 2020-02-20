import React from 'react';
import './App.scss';
import { BrowserRouter as Router } from 'react-router-dom';
import { baseUrl } from './utils/endpoints';
import Routes from './utils/Routes';
import history from './utils/history';

function App() {
  localStorage.setItem('fetchClusters', true);
  return (
    <Router history={history}>
      <Routes location={baseUrl} />
    </Router>
  );
}

export default App;
