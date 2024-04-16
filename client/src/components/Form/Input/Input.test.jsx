/*eslint-disable*/
import React from 'react';
import { describe, it } from 'vitest';
import Input from './Input';
import { render } from '@testing-library/react';
import { Simulate } from 'react-dom/test-utils';

describe('Input', () => {
  let testText = '';
  const props = {
    name: 'test',
    label: 'Test',
    placeholder: 'This is test',
    onChange: e => {
      testText = e.target.value;
    }
  };

  const { container } = render(<Input {...props} />);

  it('should render text successfully', ({ expect }) => {
    const input = container.querySelectorAll(`#${props.name}`);
    expect(input).toHaveLength(1);
  });

  it('should change text successfully', ({ expect }) => {
    const input = container.querySelector(`#${props.name}`);
    input.value = 'checkTest';
    Simulate.change(input);
    expect(testText).toBe('checkTest');
  });

  it('should render error text successfully', ({ expect }) => {
    props.error = 'test error';
    const { container } = render(<Input {...props} />);
    const error = container.querySelector('#input-error');
    expect(error.textContent).toBe(props.error);
  });

  it('should not render error text', ({ expect }) => {
    const error = container.querySelectorAll('#input-error');
    expect(error).toHaveLength(0);
  });
});
