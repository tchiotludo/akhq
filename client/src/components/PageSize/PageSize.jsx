import React from 'react';
import './styles.scss';

const PageSize = ({
  pageNumber,
  totalPageNumber,
  currentPageSize,
  totalRecords,
  ranges = [10, 25, 50, 100, 150, 200],
  onChange,
  editPageNumber,
  showTotalPageNumber = true
}) => {
    var pageSizeOptions = [];
    var pageSizeOptionsList = [];
    ranges.forEach(element => pageSizeOptions[element] = element );
    //if server's pageSize does not belong to ranges, it should be added - when the page is loaded for the first
    if(!ranges.includes(currentPageSize)){
      pageSizeOptions[currentPageSize] = currentPageSize;
    }
    pageSizeOptions.forEach((k) => {
        pageSizeOptionsList.push(<option key={k} value={k}>{pageSizeOptions[k]}</option>);
    })
  return (
    <div id="result-per-page">
        <span>Results</span>
        <select className="pagination mb-0" id="currentPageSize" name="currentPageSize" value={currentPageSize} onChange={(e) => {e.preventDefault(); onChange(pageSizeOptions[e.target.value])}}>
          {pageSizeOptionsList}
        </select>
      </div>
  );
};

export default PageSize;