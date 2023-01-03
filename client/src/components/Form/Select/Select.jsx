import React from 'react';
import PropTypes from 'prop-types';

const Select = ({ name, label, items, error, wrapperClass, selectClass, blankItem, ...rest }) => {
  let wrapperClassRender = 'form-group';
  let selectClassRender = 'col-xs-10';
  if (wrapperClass) {
    wrapperClassRender = wrapperClass;
  }
  if (selectClass) {
    selectClassRender = selectClass;
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
      <div className={`${selectClassRender}`}>
        <select className={'form-control'} id={name} name={name} {...rest}>
          {blankItem && (
            <option key="" value="">
              Choose item...
            </option>
          )}
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

Select.propTypes = {
  name: PropTypes.string,
  label: PropTypes.string,
  items: PropTypes.array,
  error: PropTypes.string,
  blankItem: PropTypes.bool,
  wrapperClass: PropTypes.string,
  selectClass: PropTypes.string
};

export default Select;
