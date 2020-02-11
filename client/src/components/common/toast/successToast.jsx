import React from 'react';
import './toast.css';

function SuccessToast({message, show}) {
    const style = show ? {display: "flex"} : {display: "none"};

    return (
        <div aria-labelledby="swal2-title" aria-describedby="swal2-content"
             className="swal2-popup swal2-toast swal2-show" tabIndex="-1" role="alert" aria-live="polite"
             style={style}>
            <div className="swal2-header">
                <ul className="swal2-progresssteps" style={{display: "none"}}/>
                <div className="swal2-icon swal2-error" style={{display: "none"}}>
                    <span className="swal2-x-mark">
                        <span className="swal2-x-mark-line-left"/>
                        <span className="swal2-x-mark-line-right"/>
                    </span>
                </div>
                <div className="swal2-icon swal2-question" style={{display: "none"}}>
                    <span className="swal2-icon-text">?</span>
                </div>
                <div className="swal2-icon swal2-warning" style={{display: "none"}}>
                    <span className="swal2-icon-text">!</span>
                </div>
                <div className="swal2-icon swal2-info" style={{display: "none"}}>
                    <span className="swal2-icon-text">i</span>
                </div>
                <div className="swal2-icon swal2-success swal2-animate-success-icon" style={{display: "flex"}}>
                    <div className="swal2-success-circular-line-left" style={{backgroundColor: "rgb(255, 255, 255)"}}/>
                    <span className="swal2-success-line-tip"/>
                    <span className="swal2-success-line-long"/>
                    <div className="swal2-success-ring"/>
                    <div className="swal2-success-fix" style={{backgroundColor: "rgb(255, 255, 255)"}}/>
                    <div className="swal2-success-circular-line-right" style={{backgroundColor: "rgb(255, 255, 255)"}}/>
                </div>
                <img className="swal2-image" style={{display: "none"}} alt=""/>
                <h2 className="swal2-title" id="swal2-title"/>
                <button type="button" className="swal2-close" style={{display: "none"}}>×</button>
            </div>
            <div className="swal2-content">
                <div id="swal2-content" style={{display: "block"}}>{message}</div>
                <input className="swal2-input" style={{display: "none"}}/>
                <input type="file" className="swal2-file" style={{display: "none"}}/>
                <div className="swal2-range" style={{display: "none"}}>
                    <input type="range"/>
                    <output/>
                </div>
                <select className="swal2-select" style={{display: "none"}}/>
                <div className="swal2-radio" style={{display: "none"}}/>
                <label htmlFor="swal2-checkbox" className="swal2-checkbox" style={{display: "none"}}>
                    <input type="checkbox"/>
                    <span className="swal2-label"/>
                </label>
                <textarea className="swal2-textarea" style={{display: "none"}}/>
                <div className="swal2-validation-message" id="swal2-validation-message" style={{display: "none"}}/>
            </div>
            <div className="swal2-actions" style={{display: "none"}}>
                <button type="button" className="swal2-confirm swal2-styled" aria-label=""
                        style={{display: "none; border-left-color: rgb(48, 133, 214); border-right-color: rgb(48, 133, 214)"}}>
                    OK
                </button>
                <button type="button" className="swal2-cancel swal2-styled" aria-label="" style={{display: "none"}}>
                    Cancel
                </button>
            </div>
            <div className="swal2-footer" style={{display: "none"}}/>
        </div>
    );
}

export default SuccessToast;