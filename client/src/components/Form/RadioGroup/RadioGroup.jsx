import React, { Component } from 'react';

class RadioGroup extends Component {
  renderOptions = (name, items, handleChange) => {
    let renderedItems = [];
    for (let item of items) {
      renderedItems.push(
        <div id={`radio-option-${name}`} key={item.value} className="form-check">
          <input
            className="form-check-input"
            type="radio"
            name={item.name}
            id={item.value}
            value={item.value}
            defaultChecked={item.checked}
            onChange={() => handleChange(item.value)}
          />
          <label className="form-check-label" htmlFor={item.name}>
            {item.label}
          </label>
        </div>
      );
    }

    return renderedItems;
  };

  render() {
    const { name, label, items, handleChange } = this.props;

    return (
      <fieldset name={name} className="form-group">
        <div id={`radio-${name}`} className="row">
          <legend className="col-form-label col-sm-2 pt-0">{label}</legend>
          <div id={`radio-options-${name}`} className="col-sm-10">
            {this.renderOptions(name, items, handleChange)}
          </div>
        </div>
      </fieldset>
    );
  }
}

export default RadioGroup;
