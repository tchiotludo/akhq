/*eslint-disable*/
import React, { Children } from 'react';
import { createMemoryHistory } from 'history';
import TopicProduce from './TopicProduce';
import { render } from '@testing-library/react';

import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';

describe('TopicProduce', () => {
  it('should add a new header to headers list', () => {
    const { getByTestId } = render(
      <LocalizationProvider dateAdapter={AdapterDateFns}>
        <TopicProduce match={{ params: { clusterId: '123', topicId: '456' } }} />
      </LocalizationProvider>
    );
    const headers = getByTestId('headers');
    const button = getByTestId('button_0');
    button.click();
    expect(headers.children.length).toBe(2);
  });

  it('should remove a header from headers list', () => {
    const { getByTestId } = render(
      <LocalizationProvider dateAdapter={AdapterDateFns}>
        <TopicProduce match={{ params: { clusterId: '123', topicId: '456' } }} />
      </LocalizationProvider>
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
