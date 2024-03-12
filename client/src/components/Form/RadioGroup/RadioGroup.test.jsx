/*eslint-disable*/
import React from 'react';
import { describe, it } from 'vitest';
import RadioGroup from './RadioGroup';
import { render } from '@testing-library/react';
import { Simulate } from 'react-dom/test-utils';

describe('RadioGroup', () => {
  let testOption = '';
  const props = {
    name: 'optTest',
    label: 'Opt Test',
    items: [
      {
        name: 'test1',
        label: 'Test1',
        value: 'test1',
        checked: testOption === 'test1'
      },
      {
        name: 'test2',
        label: 'Test2',
        value: 'test2',
        checked: testOption === 'test2'
      },
      {
        name: 'test3',
        label: 'Test3',
        value: 'test3',
        checked: testOption === 'test3'
      },
      {
        name: 'test4',
        label: 'Test4',
        value: 'test4',
        checked: testOption === 'test4'
      }
    ],
    handleChange: value => {
      testOption = value;
    }
  };

  const { container } = render(<RadioGroup {...props} />);

  it('should render radio group successfully', ({ expect }) => {
    const options = container.querySelectorAll('input');
    expect(options.length).toBe(4);
  });

  it('should change options successfully', ({ expect }) => {
    const radio = container.querySelectorAll('input');

    const opt1 = radio.item(1);
    opt1.value = '';
    Simulate.change(opt1);
    expect(radio.item(1).value).toBe(testOption);

    const opt2 = radio.item(2);
    opt2.value = '';
    Simulate.change(opt2);
    expect(radio.item(2).value).toBe(testOption);
  });
});
