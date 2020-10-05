/*eslint-disable*/
import React from 'react';
import Enzyme, { shallow } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import SchemaCreate from './SchemaCreate';

Enzyme.configure({ adapter: new Adapter() });

describe('SchemaCreate', () => {
  it('should save input in schemas formData', () => {
    const wrapper = shallow(<SchemaCreate />);

    const mockState = {
      subject: 'test',
      schemaData: 'schemaDataTest',
      compatibilityLevel: 'BACKWARD'
    };

    const subject = wrapper.find('#subject');
    subject.simulate('change', { currentTarget: { name: 'subject', value: 'test' } });
    const schemaData = wrapper.find('#schemaData');
    schemaData.simulate('change', 'schemaDataTest');
    const compatibilityLevel = wrapper.find('Select');
    compatibilityLevel.simulate('change', {
      target: { name: 'compatibilityLevel', value: 'BACKWARD' }
    });

    expect(wrapper.instance().state.formData).toStrictEqual(mockState);
  });
});
