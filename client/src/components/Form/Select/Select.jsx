import React from 'react';

const Select = ({ name, label, items, error, ...rest }) => {
  return (
    <div className="form-group row">
      {label !== '' ? (
        <label htmlFor={name} className="col-sm-2 col-form-label">
          {label}
        </label>
      ) : (
        <div/>
      )}
      <div className="col-sm-10">
        <select className="form-control" id={name} name={name} {...rest}>
          {items.map(item => (
            <option key={item._id} value={item._id}>
              {item.name}
            </option>
          ))}
        </select>
        {error && <div className="alert alert-danger">{error}</div>}
      </div>
    </div>
  );
};

export default Select;
