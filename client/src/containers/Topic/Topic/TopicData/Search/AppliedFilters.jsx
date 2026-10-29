import React from 'react';
import { conditionText } from './preview';

const AppliedFilters = ({ applied, notShareable }) => {
  if (!applied) {
    return null;
  }
  const conditions = applied.groups.flatMap(g => g.conditions);
  return (
    <div className="applied-filters" data-testid="applied-filters">
      <div className="small text-muted">Applied filters</div>
      <div className="filters-active">
        {conditions.map((c, i) => (
          <span key={i} className="chip">
            {conditionText(c)}
          </span>
        ))}
      </div>
      {notShareable && (
        <div className="text-warning small" role="status">
          This search is too long to be shared by link.
        </div>
      )}
    </div>
  );
};

export default AppliedFilters;
