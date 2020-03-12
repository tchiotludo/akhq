import React from 'react';
import './styles.scss';
import AceEditor from 'react-ace';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-dracula';

function CodeViewModal({ show, body, handleClose }) {
  const showHideClassname = show ? 'modal display-block' : 'modal display-none';

  return (
    <div className={showHideClassname}>
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
            <button
              type="button"
              className="close pull-right"
              aria-label="Close"
              onClick={handleClose}
            >
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div className="swal2-content">
            <div id="swal2-content" style={{ display: 'block' }}>
              <AceEditor
                mode="json"
                name="aceEditor"
                theme="dracula"
                editorProps={{ $blockScrolling: true }}
                value={body}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default CodeViewModal;
