import React from 'react';
import { FIELDS, OPERATORS } from './searchModel';

const SimpleSearch = ({ simple, onChange, disabled }) => {
  const set = patch => onChange({ ...simple, ...patch });
  const isTermOperator = simple.operator === 'contains';

  return (
    <div className="simple-search d-flex flex-wrap align-items-center" data-testid="simple-search">
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
        className="form-control search-text"
        value={simple.text}
        disabled={disabled}
        placeholder={simple.operator === 'matchesRegex' ? 'Regular expression' : 'Text to search'}
        onChange={e => set({ text: e.target.value })}
      />
      {isTermOperator && (
        <div className="btn-group term-mode" role="group" aria-label="Term logic">
          {['any', 'all'].map(mode => (
            <button
              key={mode}
              type="button"
              disabled={disabled}
              aria-pressed={simple.termMode === mode}
              className={`btn btn-sm ${simple.termMode === mode ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => set({ termMode: mode })}
            >
              {mode === 'any' ? 'Any' : 'All'}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

export default SimpleSearch;
