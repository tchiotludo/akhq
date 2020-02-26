import React, { Component } from 'react';

import Joi from 'joi-browser';
import _ from 'lodash';

import Input from './Input';
import Select from './Select';
import RadioGroup from './RadioGroup';

class Form extends Component {
  state = {
    formData: {},
    errors: {}
  };

  validate = () => {
    const options = { abortEarly: false };
    const { error } = Joi.validate(this.state.formData, this.schema);
    if (!error) return null;

    const errors = {};
    for (let item of error.details) {
      errors[item.path[0]] = item.message;
    }

    return errors;
  };

  validateProperty = ({ name, value }) => {
    const obj = { [name]: value };
    const schema = { [name]: this.schema[name] };
    const { error } = Joi.validate(obj, schema);

    return error ? error.details[0].message : null;
  };

  handleSubmit = e => {
    e.preventDefault();

    const errors = this.validate();
    this.setState({ errors: errors || {} });

    if (errors) return;

    this.doSubmit();
  };

  handleChange = ({ currentTarget: input }) => {
    console.log(input.value);
    const errors = { ...this.state.errors };
    const errorMessage = this.validateProperty(input);
    if (errorMessage) {
      errors[input.name] = errorMessage;
    } else {
      delete errors[input.name];
    }

    const { formData } = this.state;
    formData[input.name] = input.value;
    this.setState({ formData, errors });
    console.log(this.state.formData);
  };

  renderButton = (label, click, className, type) => {
    return (
      <div className="khq-submit">
        <button
          type={type ? type : 'button'}
          className={className ? className : 'btn btn-primary'}
          disabled={this.validate()}
          onClick={click}
        >
          {label}
        </button>
      </div>
    );
  };

  renderInput = (name, label, placeholder, type = 'text', rest) => {
    const { formData, errors } = this.state;
    console.log(formData[name]);

    return (
      <Input
        type={type}
        name={name}
        value={formData[name]}
        label={label}
        placeholder={placeholder}
        onChange={this.handleChange}
        error={errors[name]}
        {...rest}
      />
    );
  };

  renderSelect = (name, label, items) => {
    const { formData, errors } = this.state;

    return (
      <Select
        name={name}
        value={formData[name]}
        label={label}
        items={items}
        error={errors[name]}
        onChange={this.handleChange}
      />
    );
  };

  renderRadioGroup = (name, label, options, onChange) => {
    const { formData } = this.state;
    const items = [];

    for (let option of options) {
      const value = _.camelCase(option.toString());

      items[items.length] = {
        name: name,
        label: option,
        value: value,
        checked: value === formData[name]
      };
    }

    return <RadioGroup name={name} label={label} items={items} handleChange={onChange} />;
  };
}

export default Form;
