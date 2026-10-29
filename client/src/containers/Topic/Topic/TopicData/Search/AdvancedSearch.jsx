import React from 'react';
import { FIELDS, OPERATORS, emptyCondition, groupFieldSummary } from './searchModel';

const JoinSelect = ({ value, onChange, label, disabled }) => (
  <select
    aria-label={label}
    className="form-control join-select"
    value={value || 'AND'}
    disabled={disabled}
    onChange={e => onChange(e.target.value)}
  >
    <option value="AND">AND</option>
    <option value="OR">OR</option>
  </select>
);

export const ConditionRow = ({ condition, index, first, error, onChange, onRemove, disabled }) => {
  const set = patch => onChange({ ...condition, ...patch });
  return (
    <div className="condition-row d-flex flex-wrap align-items-center" data-testid="condition-row">
      {first ? (
        <span className="join-placeholder" />
      ) : (
        <JoinSelect
          value={condition.join}
          label={`Join for condition ${index + 1}`}
          disabled={disabled}
          onChange={join => set({ join })}
        />
      )}
      <select
        aria-label="Field"
        className="form-control"
        value={condition.field}
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
        value={condition.operator}
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
        aria-label="Value"
        type="text"
        className={`form-control search-text${error ? ' is-invalid' : ''}`}
        value={condition.value}
        disabled={disabled}
        onChange={e => set({ value: e.target.value })}
      />
      <button
        type="button"
        className="btn btn-sm btn-secondary"
        aria-label="Remove condition"
        disabled={disabled}
        onClick={onRemove}
      >
        ×
      </button>
      {error && (
        <div className="invalid-feedback d-block w-100" role="alert">
          {error}
        </div>
      )}
    </div>
  );
};

export const GroupCard = ({ group, index, errors, onChange, onRemove, disabled }) => {
  const setCondition = (ci, condition) =>
    onChange({
      ...group,
      conditions: group.conditions.map((c, i) => (i === ci ? condition : c))
    });
  const removeCondition = ci =>
    onChange({ ...group, conditions: group.conditions.filter((_, i) => i !== ci) });

  return (
    <div className="search-group card" data-testid="search-group">
      <div className="group-title d-flex align-items-center">
        {index > 0 && (
          <JoinSelect
            value={group.join}
            label={`Join for group ${index + 1}`}
            disabled={disabled}
            onChange={join => onChange({ ...group, join })}
          />
        )}
        <strong>Group {index + 1}</strong>
        <span className="group-scope text-muted" data-testid="group-scope">
          Scope: {groupFieldSummary(group) || '—'}
        </span>
        <button
          type="button"
          className="btn btn-sm btn-secondary ml-auto"
          disabled={disabled}
          onClick={onRemove}
        >
          Remove group
        </button>
      </div>
      <div className="group-rows">
        {group.conditions.map((c, ci) => (
          <ConditionRow
            key={ci}
            condition={c}
            index={ci}
            first={ci === 0}
            error={errors[`groups.${index}.conditions.${ci}`]}
            disabled={disabled}
            onChange={next => setCondition(ci, next)}
            onRemove={() => removeCondition(ci)}
          />
        ))}
        {errors[`groups.${index}`] && (
          <div className="invalid-feedback d-block" role="alert">
            {errors[`groups.${index}`]}
          </div>
        )}
      </div>
      <div>
        <button
          type="button"
          className="btn btn-sm btn-secondary"
          disabled={disabled}
          onClick={() =>
            onChange({ ...group, conditions: [...group.conditions, { ...emptyCondition() }] })
          }
        >
          + Add condition
        </button>
      </div>
    </div>
  );
};

const AdvancedSearch = ({ advanced, errors, onChange, disabled }) => {
  const groups = advanced.groups;
  return (
    <div className="advanced-search" data-testid="advanced-search">
      {groups.map((g, gi) => (
        <GroupCard
          key={gi}
          group={g}
          index={gi}
          errors={errors}
          disabled={disabled}
          onChange={next => onChange({ groups: groups.map((x, i) => (i === gi ? next : x)) })}
          onRemove={() => onChange({ groups: groups.filter((_, i) => i !== gi) })}
        />
      ))}
      <button
        type="button"
        className="btn btn-sm btn-secondary"
        disabled={disabled}
        onClick={() =>
          onChange({
            groups: [
              ...groups,
              { join: 'AND', conditions: [{ ...emptyCondition(), join: undefined }] }
            ]
          })
        }
      >
        + Add group
      </button>
    </div>
  );
};

export default AdvancedSearch;
