import React from 'react';

const Input = props => {
  const { name, label, placeholder, error, ...rest } = props;

  return (
    <div className="form-group row">
      {label !== '' ? (
        <label htmlFor={name} className="col-sm-2 col-form-label">
          {label}
        </label>
      ) : (
        <div></div>
      )}
      <div className="col-sm-10">
        <input
          {...rest}
          key={name}
          name={name}
          id={name}
          className="form-control"
          placeholder={placeholder}
        />

        {error && <div className="alert alert-danger mt-1 p-1">{error}</div>}
      </div>
    </div>
  );
};

export default Input;
