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

export const ConditionRow = ({
  condition,
  index,
  first,
  error,
  onChange,
  onRemove,
  onAdd,
  canRemove,
  disabled
}) => {
  const set = patch => onChange({ ...condition, ...patch });
  return (
    <div className="condition-row-wrapper">
      <div className="condition-row" data-testid="condition-row">
        {first ? (
          <span className="row-prefix">WHERE</span>
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
          className={`form-control${error ? ' is-invalid' : ''}`}
          value={condition.value}
          disabled={disabled}
          onChange={e => set({ value: e.target.value })}
        />
        <button
          type="button"
          className="btn btn-secondary"
          aria-label="Remove condition"
          disabled={disabled || !canRemove}
          onClick={onRemove}
        >
          ×
        </button>
        <button
          type="button"
          className="btn btn-secondary"
          aria-label="Add condition"
          disabled={disabled}
          onClick={onAdd}
        >
          +
        </button>
      </div>
      {error && (
        <div className="invalid-feedback d-block" role="alert">
          {error}
        </div>
      )}
    </div>
  );
};

export const GroupCard = ({ group, index, count, errors, onChange, onRemove, disabled }) => {
  const setCondition = (ci, condition) =>
    onChange({
      ...group,
      conditions: group.conditions.map((c, i) => (i === ci ? condition : c))
    });
  const removeCondition = ci =>
    onChange({ ...group, conditions: group.conditions.filter((_, i) => i !== ci) });
  const addConditionAfter = ci => {
    const conditions = [...group.conditions];
    conditions.splice(ci + 1, 0, { ...emptyCondition() });
    onChange({ ...group, conditions });
  };

  return (
    <div className="search-group" data-testid="search-group">
      {index > 0 && (
        <div className="group-join">
          <span className="small">Join with previous group</span>
          <JoinSelect
            value={group.join}
            label={`Join for group ${index + 1}`}
            disabled={disabled}
            onChange={join => onChange({ ...group, join })}
          />
        </div>
      )}
      <div className="group-header">
        <strong>Group {index + 1}</strong>
        <span className="small group-scope" data-testid="group-scope">
          Fields: {groupFieldSummary(group) || '—'}
        </span>
        {count > 1 && (
          <button
            type="button"
            className="btn btn-secondary ml-auto"
            disabled={disabled}
            onClick={onRemove}
          >
            Remove group
          </button>
        )}
      </div>
      <div className="condition-rows">
        {group.conditions.map((c, ci) => (
          <ConditionRow
            key={ci}
            condition={c}
            index={ci}
            first={ci === 0}
            error={errors[`groups.${index}.conditions.${ci}`]}
            disabled={disabled}
            canRemove={group.conditions.length > 1}
            onChange={next => setCondition(ci, next)}
            onRemove={() => removeCondition(ci)}
            onAdd={() => addConditionAfter(ci)}
          />
        ))}
        {errors[`groups.${index}`] && (
          <div className="invalid-feedback d-block" role="alert">
            {errors[`groups.${index}`]}
          </div>
        )}
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
          count={groups.length}
          errors={errors}
          disabled={disabled}
          onChange={next => onChange({ groups: groups.map((x, i) => (i === gi ? next : x)) })}
          onRemove={() => onChange({ groups: groups.filter((_, i) => i !== gi) })}
        />
      ))}
      <div className="groups-toolbar">
        <button
          type="button"
          className="btn btn-secondary"
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
          + Group
        </button>
      </div>
    </div>
  );
};

export default AdvancedSearch;
