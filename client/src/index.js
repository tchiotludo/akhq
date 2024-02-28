import React from 'react';
import './index.scss';
import App from './App';
import * as serviceWorker from './serviceWorker';
import prefix from './prefix';
import { createRoot } from 'react-dom/client';

let pathPrefix = prefix() + '/ui';

const container = document.getElementById('root');
const root = createRoot(container); // createRoot(container!) if you use TypeScript
root.render(<App pathPrefix={pathPrefix} />);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
