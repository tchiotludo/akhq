import React, { Component } from 'react';
import image from '../../images/logo.svg';
class Loading extends Component {
  render() {
    return (
      <h3 className="logo">
        <img src={image} />
        <sup>
          <strong>HQ</strong>
        </sup>
      </h3>
    );
  }
}

export default Loading;
