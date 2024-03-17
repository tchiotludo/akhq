import React from 'react';
import PropTypes from 'prop-types';

function Modal({ show, children }) {
  return <div className={show ? 'modal display-block' : 'modal display-none'}>{children}</div>;
}

Modal.propTypes = {
  children: PropTypes.any,
  show: PropTypes.bool
};

export default Modal;
