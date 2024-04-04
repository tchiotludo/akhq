/*eslint-disable*/
import React from 'react';
import { describe, it } from 'vitest';
import Select from './Select';
import { render } from '@testing-library/react';
import { Simulate } from 'react-dom/test-utils';

describe('Select', () => {
  let testOption = {};
  const props = {
    name: 'optTest',
    label: 'Opt Test',
    items: [
      { _id: 'test', name: 'Test' },
      { _id: 'test2', name: 'Test2' }
    ],
    onChange: ({ target }) => {
      testOption = target;
    }
  };

  const { container } = render(<Select {...props} />);

  it('should render radio group successfully', ({ expect }) => {
    const options = container.querySelectorAll('option');
    expect(options.length).toBe(2);
  });

  it('should change options successfully', ({ expect }) => {
    const options = container.querySelectorAll('option');
    const opt1 = options.item(0);
    const opt2 = options.item(1);
    const select = container.querySelector('select');
    select._id = 'test2';
    select.name = 'Test2';
    Simulate.change(select);
    expect(opt2.value).toBe(testOption._id);
    select._id = 'test';
    select.name = 'Test';
    Simulate.change(select);
    expect(opt1.value).toBe(testOption._id);
  });
});
