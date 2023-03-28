import './Spinner.scss';
import React, { Component } from 'react';

class Spinner extends Component {
  render() {
    return (
      <div className="line-spinner">
        <div className="rect1" />
        <div className="rect2" />
        <div className="rect3" />
        <div className="rect4" />
        <div className="rect5" />
      </div>
    );
  }
}

export default Spinner;
