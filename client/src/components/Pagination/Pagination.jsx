import React from 'react';
import PropTypes from 'prop-types';
import './styles.scss';

const Pagination = ({
  pageNumber,
  totalPageNumber,
  totalRecords,
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
        <a
          href={'#/'}
          className={'before-button'}
          onClick={e => {
            e.preventDefault();
            onSubmit(pageNumber - 1);
          }}
          disabled={+pageNumber === 1}
        >
          <div className="page-link">
            <span aria-hidden="true">&laquo;</span>
            <span className=" sr-only">Previous</span>
          </div>
        </a>
      </li>
      <li className="page-item info">
        <div className="page-link page-number">
          {totalRecords !== undefined ? (
            '≈ ' + totalRecords
          ) : (
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
          )}
          {totalPageNumber !== undefined && showTotalPageNumber && `of ${totalPageNumber}`}
        </div>
      </li>
      <li className={'page-item after'}>
        <a
          href={'#/'}
          className={'after-button'}
          onClick={e => {
            e.preventDefault();
            onSubmit(pageNumber + 1);
          }}
          disabled={+pageNumber === +totalPageNumber}
        >
          <div className=" page-link" aria-label=" Next">
            <span aria-hidden="true">&raquo;</span>
            <span className="sr-only">Next</span>
          </div>
        </a>
      </li>
    </ul>
  );
};

Pagination.propTypes = {
  pageNumber: PropTypes.number,
  totalPageNumber: PropTypes.number,
  totalRecords: PropTypes.number,
  onChange: PropTypes.func,
  onSubmit: PropTypes.func,
  editPageNumber: PropTypes.bool,
  showTotalPageNumber: PropTypes.bool
};

export default Pagination;
