import React, { Component } from 'react';
import './styles.scss';

const Pagination = props => {
  const { pageNumber, totalPageNumber, onChange, onSubmit } = props;

  return (
    <ul className="pagination mb-0">
      <li
        className={pageNumber === 1 ? 'page-item before disabled' : 'page-item before'}
        onClick={() => onSubmit(pageNumber - 1)}
        disabled={pageNumber === 1}
      >
        <a className=" page-link">
          <span aria-hidden=" true">&laquo;</span>
          <span className=" sr-only">Previous</span>
        </a>
      </li>
      <li className=" page-item info">
        <a className=" page-link page-number">
          <input
            className="page-input"
            type="number"
            value={pageNumber}
            onChange={onChange}
            onKeyDown={e => {
              if (e.key === 'Enter') onSubmit(pageNumber);
            }}
          />
          of {totalPageNumber}
        </a>
      </li>
      <li
        className={
          pageNumber === totalPageNumber ? 'page-item before disabled' : 'page-item before'
        }
        onClick={() => onSubmit(pageNumber + 1)}
        disabled={pageNumber === totalPageNumber}
      >
        <a className=" page-link" aria-label=" Next">
          <span aria-hidden=" true">&raquo;</span>
          <span className=" sr-only">Next</span>
        </a>
      </li>
    </ul>
  );
};

export default Pagination;
