import React, { Component } from 'react';
import PropTypes from 'prop-types';
import './styles.scss';
import image from '../../images/icon.svg';

class Loading extends Component {
  render() {
    const { show } = this.props;
    let loadingContainer = show ? 'loading-container' : 'loading-none';
    return (
      <div>
        <div className={loadingContainer}>
          <div className="loading">
            <h3 className="logo">
              <img src={image} alt="" />
              <sup>
                <strong>HQ</strong>
              </sup>
            </h3>
          </div>
        </div>

        {this.props.children}
      </div>
    );
  }
}

Loading.propTypes = {
  show: PropTypes.bool,
  children: PropTypes.any
};

export default Loading;
