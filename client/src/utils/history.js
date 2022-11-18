import { createBrowserHistory } from 'history';
import { basePath } from './endpoints';

const customHistory = createBrowserHistory({
  basename: basePath
});

export default customHistory;
