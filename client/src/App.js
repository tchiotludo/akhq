import React from 'react';
import './App.scss';
import { BrowserRouter as Router } from 'react-router-dom';
import { baseUrl } from './services/endpoints';
import Routes from './utils/Routes';

function App() {
  localStorage.setItem('fetchClusters', true);
  return (
    <Router>
      <Routes location={baseUrl} />
    </Router>
  );
}

export default App;
