import React from 'react';
import './styles.scss';

const Pagination = ({
  pageNumber,
  totalPageNumber,
  onChange,
  onSubmit,
  editPageNumber,
  showTotalPageNumber = true
}) => {
  if (editPageNumber === undefined) {
    editPageNumber = true;
  }

  return (
    <ul className="pagination mb-0">
      <li className={'page-item before'}>
        <button
          className={'before-button'}
          onClick={() => onSubmit(pageNumber - 1)}
          disabled={+pageNumber === 1}
        >
          <a className=" page-link">
            <span aria-hidden=" true">&laquo;</span>
            <span className=" sr-only">Previous</span>
          </a>
        </button>
      </li>
      <li className=" page-item info">
        <a className=" page-link page-number">
          <input
            className="pagination-input page-input"
            disabled={!editPageNumber}
            type="number"
            value={pageNumber}
            onChange={onChange}
            onKeyDown={e => {
              if (e.key === 'Enter') onSubmit(pageNumber);
            }}
          />
          {totalPageNumber !== undefined && showTotalPageNumber && `of ${totalPageNumber}`}
        </a>
      </li>
      <li className={'page-item after'}>
        <button
          className={'after-button'}
          onClick={() => onSubmit(pageNumber + 1)}
          disabled={+pageNumber === +totalPageNumber}
        >
          <a className=" page-link" aria-label=" Next">
            <span aria-hidden="true">&raquo;</span>
            <span className="sr-only">Next</span>
          </a>
        </button>
      </li>
    </ul>
  );
};

export default Pagination;
