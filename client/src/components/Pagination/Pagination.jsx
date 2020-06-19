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
          <div className="page-link">
            <span aria-hidden="true">&laquo;</span>
            <span className=" sr-only">Previous</span>
          </div>
        </button>
      </li>
      <li className=" page-item info">
        <div className=" page-link page-number">
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
        </div>
      </li>
      <li className={'page-item after'}>
        <button
          className={'after-button'}
          onClick={() => onSubmit(pageNumber + 1)}
          disabled={+pageNumber === +totalPageNumber}
        >
          <div className=" page-link" aria-label=" Next">
            <span aria-hidden="true">&raquo;</span>
            <span className="sr-only">Next</span>
          </div>
        </button>
      </li>
    </ul>
  );
};

export default Pagination;
