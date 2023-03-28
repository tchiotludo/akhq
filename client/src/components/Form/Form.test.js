/*eslint-disable*/
import React from 'react';
import Enzyme, { shallow } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import Form from './Form';
import { intersect } from 'joi-browser';
import { ExpansionPanelActions } from '@material-ui/core';
import Joi from 'joi-browser';

Enzyme.configure({ adapter: new Adapter() });

describe('Form', () => {
  const TestForm = new Form();
  TestForm.schema = {
    test: Joi.number().min(0).label('Test').required()
  };
  TestForm.state.formData = {
    test: 12
  };

  it('should validate schema property successfully', () => {
    let validatePropertyResponse = TestForm.validateProperty({ name: 'test', value: 12 });
    expect(validatePropertyResponse).toBe(null);
  });

  it('should send error at validate schema property', () => {
    let validatePropertyResponse = TestForm.validateProperty({ name: 'test', value: 'testinho ' });
    expect(validatePropertyResponse).not.toBe(null);
  });

  it('should validate schema successfully', () => {
    let validatePropertyResponse = TestForm.validate();
    expect(validatePropertyResponse).toBe(null);
  });

  it('should send error at validate schema', () => {
    TestForm.state.formData = {
      test: 'test'
    };
    let validatePropertyResponse = TestForm.validate();
    expect(validatePropertyResponse).not.toBe(null);
  });
});
