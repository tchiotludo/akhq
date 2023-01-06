import React from 'react';
import PropTypes from 'prop-types';

import '../styles.scss';

function ConfirmModal({ handleConfirm, handleCancel, show, message, confirmLabel, cancelLabel }) {
  return (
    <div className={show ? 'modal display-block' : 'modal display-none'}>
      <div
        className="swal2-container swal2-center swal2-fade swal2-shown"
        style={{ overflowY: 'auto' }}
      >
        <div
          aria-labelledby="swal2-title"
          aria-describedby="swal2-content"
          className="swal2-popup swal2-modal swal2-show"
          tabIndex="-1"
          role="dialog"
          aria-live="assertive"
          aria-modal="true"
          style={{ display: 'flex' }}
        >
          <div className="swal2-header">
            <ul className="swal2-progresssteps" style={{ display: 'none' }} />
            <div className="swal2-icon swal2-error" style={{ display: 'none' }}>
              <span className="swal2-x-mark">
                <span className="swal2-x-mark-line-left" />
                <span className="swal2-x-mark-line-right" />
              </span>
            </div>
            <div
              className="swal2-icon swal2-question swal2-animate-question-icon"
              style={{ display: 'flex' }}
            >
              <span className="swal2-icon-text">?</span>
            </div>
            {/*<div className="swal2-icon swal2-warning" style={{display: "none"}}>*/}
            {/*    <span className="swal2-icon-text">!</span>*/}
            {/*</div>*/}
            {/*<div className="swal2-icon swal2-info" style={{display: "none"}}>*/}
            {/*    <span className="swal2-icon-text">i</span>*/}
            {/*</div>*/}
            {/*<div className="swal2-icon swal2-success" style={{display: "none"}}>*/}
            {/*    <div className="swal2-success-circular-line-left"*/}
            {/*         style={{backgroundColor: "rgb(255, 255, 255)"}}/>*/}
            {/*    <span className="swal2-success-line-tip"/>*/}
            {/*    <span className="swal2-success-line-long"/>*/}
            {/*    <div className="swal2-success-ring"/>*/}
            {/*    <div className="swal2-success-fix" style={{backgroundColor: "rgb(255, 255, 255)"}}/>*/}
            {/*    <div className="swal2-success-circular-line-right"*/}
            {/*         style={{backgroundColor: "rgb(255, 255, 255)"}}/>*/}
            {/*</div>*/}
            {/*<img alt="" className="swal2-image" style={{display: "none"}}/>*/}
            {/*<h2 className="swal2-title" id="swal2-title"/>*/}
            {/*<button type="button" className="swal2-close" style={{display: "none"}}>×</button>*/}
          </div>
          <div className="swal2-content">
            <div id="swal2-content" style={{ display: 'block' }}>
              {message}
            </div>
            {/*<input className="swal2-input" style={{display: "none"}}/>*/}
            {/*<input type="file" className="swal2-file" style={{display: "none"}}/>*/}
            {/*<div className="swal2-range" style={{display: "none"}}>*/}
            {/*    <input type="range"/>*/}
            {/*</div>*/}
            {/*<select className="swal2-select" style={{display: "none"}}/>*/}
            {/*<div className="swal2-radio" style={{display: "none"}}/>*/}
            {/*<label htmlFor="swal2-checkbox" className="swal2-checkbox" style={{display: "none"}}>*/}
            {/*    <input type="checkbox"/><span className="swal2-label"/>*/}
            {/*</label>*/}
            {/*<textarea className="swal2-textarea" style={{display: "none"}}/>*/}
            {/*<div className="swal2-validation-message" id="swal2-validation-message"*/}
            {/*     style={{display: "none"}}/>*/}
          </div>
          <div className="swal2-actions" style={{ display: 'flex' }}>
            <button
              type="button"
              className="swal2-confirm swal2-styled"
              aria-label=""
              style={{
                borderLeftColor: 'rgb(48, 133, 214); border-right-color: rgb(48, 133, 214)'
              }}
              onClick={handleConfirm}
            >
              {confirmLabel ? confirmLabel : 'OK'}
            </button>
            <button
              type="button"
              className="swal2-cancel swal2-styled"
              aria-label=""
              style={{ display: 'inline-block' }}
              onClick={handleCancel}
            >
              {cancelLabel ? cancelLabel : 'Cancel'}
            </button>
          </div>
          <div className="swal2-footer" style={{ display: 'none' }} />
        </div>
      </div>
    </div>
  );
}

ConfirmModal.propTypes = {
  handleConfirm: PropTypes.func,
  handleCancel: PropTypes.func,
  show: PropTypes.bool,
  message: PropTypes.string,
  confirmLabel: PropTypes.string,
  cancelLabel: PropTypes.string
};

export default ConfirmModal;
