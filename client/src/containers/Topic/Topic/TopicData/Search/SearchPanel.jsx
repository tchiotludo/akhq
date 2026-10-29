import React, { useMemo, useState } from 'react';
import SimpleSearch from './SimpleSearch';
import AdvancedSearch from './AdvancedSearch';
import AppliedFilters from './AppliedFilters';
import {
  advancedToSimple,
  draftToExpression,
  emptySimple,
  simpleToAdvanced,
  validateDraft
} from './searchModel';
import './search.scss';

const SearchPanel = ({
  draft,
  onDraftChange,
  applied,
  onApply,
  onClear,
  onStop,
  isSearching,
  notShareable,
  limits
}) => {
  const [blockedReason, setBlockedReason] = useState(null);
  const errors = useMemo(() => validateDraft(draft, limits), [draft, limits]);
  const errorsByPath = useMemo(
    () => Object.fromEntries(errors.map(e => [e.path, e.message])),
    [errors]
  );
  const globalErrors = errors.filter(e => e.path === 'groups' || e.path === 'conditions');
  const hasCriteria = draftToExpression(draft) !== null;

  const switchToAdvanced = () => {
    setBlockedReason(null);
    onDraftChange({
      mode: 'advanced',
      simple: draft.simple,
      advanced: simpleToAdvanced(draft.simple)
    });
  };

  const switchToSimple = () => {
    const result = advancedToSimple(draft.advanced);
    if (result.simple) {
      setBlockedReason(null);
      onDraftChange({ mode: 'simple', simple: result.simple, advanced: null });
    } else {
      setBlockedReason(result.reason);
    }
  };

  const startNewSimple = () => {
    setBlockedReason(null);
    onDraftChange({ mode: 'simple', simple: emptySimple(), advanced: null });
  };

  const submit = e => {
    e.preventDefault();
    if (errors.length === 0 && !isSearching) {
      onApply(draftToExpression(draft));
    }
  };

  const clear = () => {
    setBlockedReason(null);
    onClear();
  };

  return (
    <form className="search-panel" onSubmit={submit} data-testid="search-panel">
      <div className="d-flex align-items-center search-panel-header">
        <div className="btn-group" role="group" aria-label="Search mode">
          <button
            type="button"
            className={`btn btn-sm ${draft.mode === 'simple' ? 'btn-primary' : 'btn-secondary'}`}
            aria-pressed={draft.mode === 'simple'}
            onClick={draft.mode === 'simple' ? undefined : switchToSimple}
          >
            Simple
          </button>
          <button
            type="button"
            className={`btn btn-sm ${draft.mode === 'advanced' ? 'btn-primary' : 'btn-secondary'}`}
            aria-pressed={draft.mode === 'advanced'}
            onClick={draft.mode === 'advanced' ? undefined : switchToAdvanced}
          >
            Advanced
          </button>
        </div>
      </div>

      {draft.mode === 'simple' ? (
        <SimpleSearch
          simple={draft.simple}
          onChange={simple => onDraftChange({ ...draft, simple })}
        />
      ) : (
        <AdvancedSearch
          advanced={draft.advanced}
          errors={errorsByPath}
          onChange={advanced => onDraftChange({ ...draft, advanced })}
        />
      )}

      {blockedReason && (
        <div className="alert alert-warning" role="alert" data-testid="switch-blocked">
          This search cannot be shown in Simple mode. {blockedReason}
          <button type="button" className="btn btn-sm btn-link" onClick={startNewSimple}>
            Start a new simple search
          </button>
        </div>
      )}

      <div aria-live="polite">
        {draft.mode === 'simple' && errorsByPath['groups.0.conditions.0'] && (
          <div className="text-danger">{errorsByPath['groups.0.conditions.0']}</div>
        )}
        {globalErrors.map(e => (
          <div key={e.path} className="text-danger">
            {e.message}
          </div>
        ))}
      </div>

      <div className="search-actions">
        {isSearching ? (
          <button type="button" className="btn btn-secondary" onClick={onStop}>
            Stop
          </button>
        ) : (
          <button type="submit" className="btn btn-primary" disabled={errors.length > 0}>
            Search
          </button>
        )}
        <button
          type="button"
          className="btn btn-secondary"
          disabled={!hasCriteria && !applied}
          onClick={clear}
        >
          Clear
        </button>
      </div>

      <AppliedFilters applied={applied} notShareable={notShareable} />
    </form>
  );
};

export default SearchPanel;
