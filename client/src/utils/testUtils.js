/*eslint-disable*/
import React from 'react';
import { createMemoryHistory } from 'history';
import { render } from '@testing-library/react';
import Router from 'react-dom';
import { MuiPickersUtilsProvider } from '@material-ui/pickers';
import MomentUtils from '@date-io/moment';

export function renderWithRouter(
  ui,
  { route = '/', history = createMemoryHistory({ initialEntries: [route] }) } = {}
) {
  return {
    ...render(
      //<MuiPickersUtilsProvider utils={MomentUtils}>
      <Router history={history}>{ui}</Router>
      //  </MuiPickersUtilsProvider>
    ),
    // adding `history` to the returned utilities to allow us
    // to reference it in our tests (just try to avoid using
    // this to test implementation details).
    history
  };
}

export default { renderWithRouter };
