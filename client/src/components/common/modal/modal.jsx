import React from 'react';
import './modal.css';

function Modal({ handleClose, show, children }) {
    const showHideClassname = show ? "modal display-block" : "modal display-none";

    return (
        <div className={showHideClassname}>
                {children}
        </div>
    );
}

export default Modal;