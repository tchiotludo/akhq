/*eslint-disable*/
import React from 'react';
import Enzyme, { shallow } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import RadioGroup from './RadioGroup';
import { intersect } from 'joi-browser';
import { ExpansionPanelActions } from '@material-ui/core';

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
    onChange: value => {
      testOption = value;
    }
  };
  const wrapper = shallow(<RadioGroup {...props} />);

  it('should render radio group successfully', () => {
    const radio = wrapper.find(`#radio-${props.name}`);
    const options = radio.find(`#radio-option-${props.name}`);
    expect(options).toHaveLength(4);
  });

  it('should change options successfully', () => {
    const radio = wrapper.find(`#radio-${props.name}`);
    const options = radio.find(`#radio-option-${props.name}`);
    console.log('option', options[0]);
    //expect(options).toHaveLength(4);
  });
});
