import React from 'react';
import { conditionText, expressionPreview } from './preview';

const AppliedFilters = ({ applied, notShareable }) => {
  if (!applied) {
    return null;
  }
  const conditions = applied.groups.flatMap(g => g.conditions);
  return (
    <div className="applied-filters" data-testid="applied-filters">
      <span className="text-muted">Applied:</span>
      {conditions.map((c, i) => (
        <span key={i} className="badge badge-secondary applied-chip">
          {conditionText(c)}
        </span>
      ))}
      <div className="query-preview text-muted" data-testid="query-preview">
        {expressionPreview(applied)}
        <small className="d-block">Conditions are evaluated from left to right.</small>
      </div>
      {notShareable && (
        <div className="text-warning" role="status">
          This search is too long to be shared by link.
        </div>
      )}
    </div>
  );
};

export default AppliedFilters;
