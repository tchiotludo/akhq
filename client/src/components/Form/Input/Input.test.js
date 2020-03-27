/*eslint-disable*/
import React from 'react';
import Enzyme, { shallow } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import Input from './Input';
import { intersect } from 'joi-browser';
import { ExpansionPanelActions } from '@material-ui/core';

Enzyme.configure({ adapter: new Adapter() });

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
  const wrapper = shallow(<Input {...props} />);

  it('should render text successfully', () => {
    const input = wrapper.find(`#${props.name}`);
    expect(input).toHaveLength(1);
  });

  it('should change text successfully', () => {
    const input = wrapper.find(`#${props.name}`);
    input.simulate('change', { target: { value: 'checkTest' } });
    expect(testText).toBe('checkTest');
  });

  it('should render error text successfully', () => {
    props.error = 'test error';
    const wrapper = shallow(<Input {...props} />);
    const error = wrapper.find('#input-error');
    expect(error.text()).toBe(props.error);
  });

  it('should not render error text', () => {
    const error = wrapper.find('#input-error');
    expect(error).toHaveLength(0);
  });
});
