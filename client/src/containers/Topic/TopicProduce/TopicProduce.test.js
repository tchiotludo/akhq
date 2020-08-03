/*eslint-disable*/
import React, { Children } from 'react';
import ReactDOM from 'react-dom';
import { createMemoryHistory } from 'history';
import TopicProduce from './TopicProduce';
import App from '../../../App';
import { render } from '@testing-library/react';

import { MemoryRouter } from 'react-router-dom';
import { MuiPickersUtilsProvider } from '@material-ui/pickers';
import MomentUtils from '@date-io/moment';
import { ExpansionPanelActions } from '@material-ui/core';

describe('TopicProduce', () => {
  it('should add a new header to headers list', () => {
    const route = '/abs/topic/123/produce';
    const history = createMemoryHistory({ initialEntries: [route] });
    const { getByTestId } = render(
      <MuiPickersUtilsProvider utils={MomentUtils}>
        <TopicProduce match={{ params: { clusterId: '123', topicId: '456' } }} history={history} />
      </MuiPickersUtilsProvider>
    );
    const headers = getByTestId('headers');
    const button = getByTestId('button_0');
    button.click();
    expect(headers.children.length).toBe(2);
  });

  it('should remove a header from headers list', () => {
    const route = '/abs/topic/123/produce';
    const history = createMemoryHistory({ initialEntries: [route] });
    const { getByTestId } = render(
      <MuiPickersUtilsProvider utils={MomentUtils}>
        <TopicProduce match={{ params: { clusterId: '123', topicId: '456' } }} history={history} />
      </MuiPickersUtilsProvider>
    );
    const headers = getByTestId('headers');
    const button = getByTestId('button_0');
    button.click();

    expect(headers.children.length).toBe(2);
    const button2 = getByTestId('button_1');
    button2.click();
    expect(headers.children.length).toBe(1);
  });
});
