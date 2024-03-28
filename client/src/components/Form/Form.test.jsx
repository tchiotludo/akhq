/*eslint-disable*/
import { describe, it } from 'vitest';
import Form from './Form';
import Joi from 'joi-browser';

describe('Form', () => {
  const TestForm = new Form();
  TestForm.schema = {
    test: Joi.number().min(0).label('Test').required()
  };
  TestForm.state.formData = {
    test: 12
  };

  it('should validate schema property successfully', ({ expect }) => {
    let validatePropertyResponse = TestForm.validateProperty({ name: 'test', value: 12 });
    expect(validatePropertyResponse).toBe(null);
  });

  it('should send error at validate schema property', ({ expect }) => {
    let validatePropertyResponse = TestForm.validateProperty({ name: 'test', value: 'testinho ' });
    expect(validatePropertyResponse).not.toBe(null);
  });

  it('should validate schema successfully', ({ expect }) => {
    let validatePropertyResponse = TestForm.validate();
    expect(validatePropertyResponse).toBe(null);
  });

  it('should send error at validate schema', ({ expect }) => {
    TestForm.state.formData = {
      test: 'test'
    };
    let validatePropertyResponse = TestForm.validate();
    expect(validatePropertyResponse).not.toBe(null);
  });
});
