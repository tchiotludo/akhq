import React from 'react';
import { conditionText } from './preview';

const AppliedFilters = ({ applied, notShareable, inline = false }) => {
  if (!applied) {
    return null;
  }
  return (
    <div className={`applied-filters${inline ? ' inline' : ''}`} data-testid="applied-filters">
      <div className="filters-active">
        <span className="small text-muted">Applied filters</span>
        {applied.groups.map((group, gi) => (
          <React.Fragment key={gi}>
            {gi > 0 && <span className="join-badge group-join-badge">{group.join}</span>}
            <span className={`chip-group${group.conditions.length > 1 ? ' multi' : ''}`}>
              {group.conditions.map((c, ci) => (
                <React.Fragment key={ci}>
                  {ci > 0 && <span className="join-badge">{c.join}</span>}
                  <span className="chip">{conditionText(c)}</span>
                </React.Fragment>
              ))}
            </span>
          </React.Fragment>
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
