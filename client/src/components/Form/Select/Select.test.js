/*eslint-disable*/
/*
import React from 'react';
import Enzyme, { shallow } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import Select from './Select';

Enzyme.configure({ adapter: new Adapter() });

describe('Select', () => {
  let testOption = '';
  const props = {
    name: 'optTest',
    label: 'Opt Test',
    items: [
      { _id: 'test', name: 'Test' },
      { _id: 'test2', name: 'Test2' }
    ],
    onChange: value => {
      testOption = value;
    }
  };
  const wrapper = shallow(<Select {...props} />);

  it('should render radio group successfully', () => {
    const options = wrapper.find('option');
    expect(options.length).toBe(2);
  });

  it('should change options successfully', () => {
    const select = wrapper.find('select');
    const options = wrapper.find('option');
    const opt1 = options.at(0);
    const opt2 = options.at(1);
    select.simulate('change', { _id: 'test2', name: 'Test2' });
    expect(opt2.getElement().props.value).toBe(testOption._id);
    select.simulate('change', { _id: 'test', name: 'Test' });
    expect(opt1.getElement().props.value).toBe(testOption._id);
  });
});

 */
