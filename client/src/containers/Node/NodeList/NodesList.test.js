/*eslint-disable*/
/*
import React from 'react';
import Enzyme, { shallow } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import NodesList from './NodesList';

Enzyme.configure({ adapter: new Adapter() });

describe('NodesList', () => {
  it('should return processed nodes list', () => {
    const wrapper = shallow(<NodesList />);
    const instance = wrapper.instance();

    const data = [{ id: 1001, host: 'kafka', port: 9092 }];
    const expectedResult = {
      id: data[0].id || '',
      host: data[0].host || '',
      port: data[0].port || '',
      rack: data[0].rack || ''
    };
    const result = instance.handleData(data);
    expect(result[0]).toStrictEqual(expectedResult);
  });
});

 */
