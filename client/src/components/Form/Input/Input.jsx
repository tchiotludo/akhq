import React from 'react';
import PropTypes from 'prop-types';

const Input = props => {
  const { name, label, placeholder, error, noStyle, wrapperClass, inputClass, ...rest } = props;
  let wrapperClassRender = 'form-group';
  let inputClassRender = 'col-sm-10';
  if (noStyle) {
    wrapperClassRender = '';
    inputClassRender = '';
  }
  if (wrapperClass) {
    wrapperClassRender = wrapperClass;
  }
  if (inputClass) {
    inputClassRender = inputClass;
  }

  return (
    <div className={`${wrapperClassRender}`}>
      {label !== '' ? (
        <label htmlFor={name} className="col-sm-2 col-form-label">
          {label}
        </label>
      ) : (
        <div />
      )}
      <div className={`${inputClassRender}`}>
        <input
          {...rest}
          key={name}
          name={name}
          id={name}
          className="form-control"
          placeholder={placeholder}
        />

        {error && (
          <div id="input-error" className="alert alert-danger mt-1 p-1">
            {error}
          </div>
        )}
      </div>
    </div>
  );
};

Input.propTypes = {
  name: PropTypes.string,
  label: PropTypes.string,
  placeholder: PropTypes.string,
  error: PropTypes.string,
  noStyle: PropTypes.bool,
  wrapperClass: PropTypes.string,
  inputClass: PropTypes.string
};

export default Input;
