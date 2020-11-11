import React from 'react';

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
    <div className={`${wrapperClassRender} row`}>
      {label !== '' ? (
        <label htmlFor={name} className="col-sm-2 col-form-label">
          {label}
        </label>
      ) : (
        <div/>
      )}
      <div className={`${selectClassRender}`}>
        <select className={'form-control'} id={name} name={name} {...rest}>
          {blankItem &&
              <option key="" value="">
                  Choose item...
              </option>
          }
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
