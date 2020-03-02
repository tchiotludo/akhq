import React, { Component } from 'react';
import './styles.scss'
import image from '../../images/logo.svg';
class Loading extends Component {
  render() {
    return (
      <div className="loading">
        <h3 className="logo">
          <img src={image} />
          <sup>
            <strong>HQ</strong>
          </sup>
        </h3>
      </div>
    );
  }
}

export default Loading;
