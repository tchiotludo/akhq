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
  const [expanded, setExpanded] = useState(false);
  // Errors are only shown once the user tries to search, in both modes.
  const [showErrors, setShowErrors] = useState(false);
  const hasCriteria = draftToExpression(draft) !== null;
  const errors = useMemo(() => {
    const found = validateDraft(draft, limits);
    if (draft.mode === 'simple' && !hasCriteria && found.length === 0) {
      found.push({ path: 'groups.0.conditions.0', message: 'Enter a term to search.' });
    }
    return found;
  }, [draft, limits, hasCriteria]);
  const shownErrors = showErrors ? errors : [];
  const errorsByPath = useMemo(
    () => Object.fromEntries(shownErrors.map(e => [e.path, e.message])),
    [shownErrors]
  );
  const globalErrors = shownErrors.filter(e => e.path === 'groups' || e.path === 'conditions');

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

  const selectMode = mode => {
    setExpanded(true);
    if (mode === draft.mode) return;
    setShowErrors(false);
    mode === 'advanced' ? switchToAdvanced() : switchToSimple();
  };

  const startNewSimple = () => {
    setBlockedReason(null);
    onDraftChange({ mode: 'simple', simple: emptySimple(), advanced: null });
  };

  const submit = e => {
    e.preventDefault();
    if (isSearching) return;
    if (errors.length > 0) {
      setShowErrors(true);
      return;
    }
    setShowErrors(false);
    onApply(draftToExpression(draft));
  };

  const clear = () => {
    setBlockedReason(null);
    setShowErrors(false);
    onClear();
  };

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
      <button type="submit" className="btn btn-primary">
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
      <div className={`search-panel-header${expanded ? ' is-open' : ''}`}>
        <button
          type="button"
          className="search-panel-toggle"
          aria-expanded={expanded}
          onClick={() => setExpanded(!expanded)}
        >
          <span className="search-panel-chevron" aria-hidden="true">
            {expanded ? '▾' : '▸'}
          </span>
          <strong>Search messages</strong>
        </button>
        {!expanded && <AppliedFilters inline applied={applied} notShareable={notShareable} />}
        <div className="btn-group btn-group-sm search-mode" role="group" aria-label="Search mode">
          {['simple', 'advanced'].map(mode => (
            <button
              key={mode}
              type="button"
              className={`btn ${draft.mode === mode ? 'btn-primary' : 'btn-secondary'}`}
              aria-pressed={draft.mode === mode}
              onClick={() => selectMode(mode)}
            >
              {mode === 'simple' ? 'Simple' : 'Advanced'}
            </button>
          ))}
        </div>
      </div>

      {expanded && (
        <>
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
                <small>Conditions are evaluated from left to right.</small>
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
        </>
      )}

      {expanded && <AppliedFilters applied={applied} notShareable={notShareable} />}
    </form>
  );
};

export default SearchPanel;
