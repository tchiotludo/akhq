/*eslint-disable*/
/*
import React from 'react';
import Enzyme, { shallow } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import RadioGroup from './RadioGroup';

Enzyme.configure({ adapter: new Adapter() });

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
  const wrapper = shallow(<RadioGroup {...props} />);

  it('should render radio group successfully', () => {
    const options = wrapper.find('input');
    expect(options.length).toBe(4);
  });

  it('should change options successfully', () => {
    const radio = wrapper.find('input');
    const opt1 = radio.at(1);
    const opt2 = radio.at(2);
    opt1.simulate('change', opt1.getElement().props.value);
    expect(opt1.getElement().props.value).toBe(testOption);
    opt2.simulate('change', opt2.getElement().props.value);
    expect(opt2.getElement().props.value).toBe(testOption);
  });
});

 */
