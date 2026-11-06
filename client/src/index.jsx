import React from 'react';
import './index.scss';
import App from './App';
import * as serviceWorker from './serviceWorker';
import { basePath } from './utils/endpoints';
import { createRoot } from 'react-dom/client';

import { ThemeProvider } from './context/ThemeContext';
import { getStoredTheme, resolveTheme, applyThemeToDOM } from './utils/theme';

applyThemeToDOM(resolveTheme(getStoredTheme()));

let pathPrefix = basePath + '/ui';

const container = document.getElementById('root');
const root = createRoot(container); // createRoot(container!) if you use TypeScript
root.render(
  <ThemeProvider>
    <App pathPrefix={pathPrefix} />
  </ThemeProvider>
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
