import React, { useMemo, useState } from 'react';
import SimpleSearch from './SimpleSearch';
import AdvancedSearch from './AdvancedSearch';
import AppliedFilters from './AppliedFilters';
import { expressionPreview } from './preview';
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

  const preview = expressionPreview(draftToExpression(draft));

  const actions = isSearching ? (
    <>
      <button type="button" className="btn btn-secondary" onClick={onStop}>
        Stop
      </button>
      <button
        type="button"
        className="btn btn-secondary"
        disabled={!hasCriteria && !applied}
        onClick={clear}
      >
        Clear
      </button>
    </>
  ) : (
    <>
      <button type="submit" className="btn btn-primary" disabled={errors.length > 0}>
        Search
      </button>
      <button
        type="button"
        className="btn btn-secondary"
        disabled={!hasCriteria && !applied}
        onClick={clear}
      >
        Clear
      </button>
    </>
  );

  return (
    <form className="search-panel" onSubmit={submit} data-testid="search-panel">
      <div className="search-panel-header">
        <strong>Search messages</strong>
        <button
          type="button"
          className="btn btn-secondary"
          onClick={draft.mode === 'simple' ? switchToAdvanced : switchToSimple}
        >
          {draft.mode === 'simple' ? 'Advanced filters' : 'Simple search'}
        </button>
      </div>

      {blockedReason && (
        <div className="alert alert-warning" role="alert" data-testid="switch-blocked">
          This search cannot be shown in Simple mode. {blockedReason}
          <button type="button" className="btn btn-sm btn-link" onClick={startNewSimple}>
            Start a new simple search
          </button>
        </div>
      )}

      {draft.mode === 'simple' ? (
        <SimpleSearch
          simple={draft.simple}
          actions={actions}
          onChange={simple => onDraftChange({ ...draft, simple })}
        />
      ) : (
        <>
          <AdvancedSearch
            advanced={draft.advanced}
            errors={errorsByPath}
            onChange={advanced => onDraftChange({ ...draft, advanced })}
          />
          <div className="query-preview">
            <span>
              Query preview: <code data-testid="query-preview">{preview || '—'}</code>
              <small className="d-block">Conditions are evaluated from left to right.</small>
            </span>
            <div className="query-actions">{actions}</div>
          </div>
        </>
      )}

      <div aria-live="polite">
        {draft.mode === 'simple' && errorsByPath['groups.0.conditions.0'] && (
          <div className="text-danger small">{errorsByPath['groups.0.conditions.0']}</div>
        )}
        {globalErrors.map(e => (
          <div key={e.path} className="text-danger small">
            {e.message}
          </div>
        ))}
      </div>

      <AppliedFilters applied={applied} notShareable={notShareable} />
    </form>
  );
};

export default SearchPanel;
