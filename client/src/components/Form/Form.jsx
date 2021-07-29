import React from 'react';

import Joi from 'joi-browser';
import _ from 'lodash';

import Input from './Input';
import Select from './Select';
import RadioGroup from './RadioGroup';
import DatePicker from '../DatePicker';
import AceEditor from 'react-ace';
import Dropdown from 'react-bootstrap/Dropdown';

import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-merbivore_soft';
import Root from '../Root';

class Form extends Root {
  state = {
    formData: {},
    errors: {}
  };

  validate = () => {
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
  };

  renderButton = (label, click, className, type, children) => {
    return (
      <div className="khq-submit button-footer" style={{ marginRight: 0 }}>
        <aside>
          {children}
          <button
            type={type ? type : 'button'}
            className={className ? className : 'btn btn-primary'}
            disabled={this.validate()}
            onClick={click}
          >
            {label}
          </button>
        </aside>
      </div>
    );
  };

  renderInput = (
    name,
    label,
    placeholder,
    type = 'text',
    onChange = this.handleChange,
    noStyle,
    wrapperClass,
    inputClass,
    rest
  ) => {
    const { formData, errors } = this.state;
    return (
      <Input
        type={type}
        name={name}
        id={name}
        value={formData[name] || ''}
        label={label}
        placeholder={placeholder}
        onChange={this.handleChange}
        error={errors[name]}
        noStyle={noStyle}
        wrapperClass={wrapperClass}
        inputClass={inputClass}
        {...rest}
      />
    );
  };

  renderJSONInput = (name, label, onChange) => {
    const { formData, errors } = this.state;
    return (
      <div className="form-group row">
        {label !== '' ? (
          <label htmlFor={name} className="col-sm-2 col-form-label">
            {label}
          </label>
        ) : (
          <div></div>
        )}
        <div className="col-sm-10" style={{ height: '100%' }}>
          <AceEditor
            mode={ formData.schemaType === "PROTOBUF"? "protobuf"  : "json" }
            id={name}
            theme="merbivore_soft"
            value={formData[name]}
            onChange={value => {
              onChange(value);
            }}
            name="UNIQUE_ID_OF_DIV"
            editorProps={{ $blockScrolling: true }}
            style={{ width: '100%', minHeight: '25vh' }}
          />
          {errors[name] && <div className="alert alert-danger mt-1 p-1">{errors[name]}</div>}
        </div>
      </div>
    );
  };

  renderSelect = (name, label, items, onChange, selectClass, wrapperClass, blankItem, rest) => {
    const { formData, errors } = this.state;

    return (
      <Select
        name={name}
        value={formData[name]}
        label={label}
        items={items}
        error={errors[name]}
        onChange={value => {
          onChange(value);
        }}
        selectClass={selectClass}
        wrapperClass={wrapperClass}
        blankItem={blankItem}
        {...rest}
      />
    );
  };

  renderDatePicker = (name, label, onChange) => {
    const { formData, errors } = this.state;

    return (
      <DatePicker
        name={name}
        label={label}
        error={errors[name]}
        value={formData[name]}
        onChange={value => {
          onChange(value);
        }}
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

  renderDropdown = (name, options, searchValue, selectedKeySchema, onChange, renderResults) => {
    return (
      <React.Fragment>
        <div className="form-group row">
          <label className="col-sm-2">{name}</label>
          <div className="col-sm-10">
            <Dropdown className="form-group dropdown bootstrap-select show-tick khq-select show">
              <Dropdown.Toggle className="btn dropdown-toggle btn-white">
                <input
                  type="text"
                  name="searchValue"
                  className="form-control placeholder"
                  placeholder={name}
                  value={selectedKeySchema}
                />
              </Dropdown.Toggle>
              <Dropdown.Menu>
                <div className="bs-searchbox input-group">
                  <input
                    type="text"
                    name="searchValue"
                    className="form-control col-sm-9 mr-2"
                    autoComplete="off"
                    role="combobox"
                    aria-expanded="false"
                    aria-label="Search"
                    aria-controls="bs-select-1"
                    aria-autocomplete="list"
                    placeholder={'search'}
                    onChange={onChange}
                    value={searchValue}
                  />
                </div>
                {renderResults}
              </Dropdown.Menu>
            </Dropdown>
          </div>
        </div>
      </React.Fragment>
    );
  };
}

export default Form;
