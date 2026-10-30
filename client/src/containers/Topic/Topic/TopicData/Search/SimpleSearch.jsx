import React from 'react';
import { FIELDS, OPERATORS } from './searchModel';

const HINT = {
  matchesRegex: 'Regex searches for a hit anywhere in the selected field.'
};

const SimpleSearch = ({ simple, onChange, disabled, actions }) => {
  const set = patch => onChange({ ...simple, ...patch });
  const isTermOperator = simple.operator === 'contains';

  return (
    <div className="simple-search quick-search" data-testid="simple-search">
      <div className="quick-search-row">
        <strong>Search</strong>
        <select
          aria-label="Field"
          className="form-control"
          value={simple.field}
          disabled={disabled}
          onChange={e => set({ field: e.target.value })}
        >
          {FIELDS.map(f => (
            <option key={f.id} value={f.id}>
              {f.label}
            </option>
          ))}
        </select>
        <select
          aria-label="Operator"
          className="form-control"
          value={simple.operator}
          disabled={disabled}
          onChange={e => set({ operator: e.target.value })}
        >
          {OPERATORS.map(o => (
            <option key={o.id} value={o.id}>
              {o.label}
            </option>
          ))}
        </select>
        <input
          aria-label="Search text"
          type="text"
          className="form-control"
          value={simple.text}
          disabled={disabled}
          placeholder={
            simple.operator === 'matchesRegex' ? 'Regular expression' : 'Enter text to search'
          }
          onChange={e => set({ text: e.target.value })}
        />
        {actions}
      </div>
      <div className="quick-search-options">
        {isTermOperator && (
          <>
            <label htmlFor="quick-term-logic">Match terms</label>
            <select
              id="quick-term-logic"
              className="form-control"
              value={simple.termMode}
              disabled={disabled}
              onChange={e => set({ termMode: e.target.value })}
            >
              <option value="any">Any</option>
              <option value="all">All</option>
            </select>
          </>
        )}
        {isTermOperator && <span>Separate terms with spaces.</span>}
        <span className="help-text">
          {HINT[simple.operator] || 'Search in the selected message field.'}
        </span>
      </div>
    </div>
  );
};

export default SimpleSearch;
