import { SEARCH_QUERY_VERSION } from '../../../../../utils/searchQuery';

export const FIELDS = [
  { id: 'value', label: 'Value' },
  { id: 'key', label: 'Key' },
  { id: 'headerKey', label: 'Header key' },
  { id: 'headerValue', label: 'Header value' },
  { id: 'keySubject', label: 'Key subject' },
  { id: 'valueSubject', label: 'Value subject' }
];

export const OPERATORS = [
  { id: 'contains', label: 'Contains' },
  { id: 'notContains', label: 'Not contains' },
  { id: 'equals', label: 'Equals' },
  { id: 'matchesRegex', label: 'Matches regex' }
];

export const DEFAULT_LIMITS = { maxGroups: 10, maxConditions: 50, maxValueLength: 1000 };

export const fieldLabel = id => FIELDS.find(f => f.id === id)?.label || id;

export const emptySimple = () => ({
  field: 'value',
  operator: 'contains',
  text: '',
  termMode: 'any'
});

export const emptyCondition = () => ({
  join: 'AND',
  field: 'value',
  operator: 'contains',
  value: ''
});

export const emptyDraft = () => ({ mode: 'simple', simple: emptySimple(), advanced: null });

const usesTerms = operator => operator === 'contains' || operator === 'notContains';

// Terms are separated by whitespace; double-quoted phrases stay a single term.
export const splitTerms = text => {
  const terms = [];
  const regex = /"([^"]*)"|(\S+)/g;
  let match;
  while ((match = regex.exec(text || '')) !== null) {
    const term = match[1] !== undefined ? match[1] : match[2];
    if (term.length > 0) {
      terms.push(term);
    }
  }
  return terms;
};

const joinTerms = terms => terms.map(t => (/\s/.test(t) ? `"${t}"` : t)).join(' ');

// Joined with AND for Not contains ("none of the terms") and for Contains/All; OR for Contains/Any.
const termJoin = simple =>
  simple.operator === 'notContains' || simple.termMode === 'all' ? 'AND' : 'OR';

export const simpleToConditions = simple => {
  const values = usesTerms(simple.operator) ? splitTerms(simple.text) : [simple.text || ''];
  const join = termJoin(simple);
  return values
    .filter(v => v.length > 0 || simple.operator === 'equals')
    .map((value, index) => ({
      join: index === 0 ? undefined : join,
      field: simple.field,
      operator: simple.operator,
      value
    }));
};

// FR-08: lossless Simple -> Advanced.
export const simpleToAdvanced = simple => {
  const conditions = simpleToConditions(simple);
  return {
    groups: [
      {
        join: undefined,
        conditions:
          conditions.length > 0
            ? conditions
            : [
                {
                  ...emptyCondition(),
                  join: undefined,
                  field: simple.field,
                  operator: simple.operator
                }
              ]
      }
    ]
  };
};

// FR-09: returns { simple } when representable, otherwise { reason }.
export const advancedToSimple = advanced => {
  const groups = advanced?.groups || [];
  if (groups.length === 0 || groups.every(g => !g.conditions || g.conditions.length === 0)) {
    return { simple: emptySimple() };
  }
  if (groups.length > 1) {
    return { reason: 'It has more than one group.' };
  }
  const conditions = groups[0].conditions;
  const { field, operator } = conditions[0];
  if (conditions.some(c => c.field !== field)) {
    return { reason: 'Its conditions target different fields.' };
  }
  if (conditions.some(c => c.operator !== operator)) {
    return { reason: 'Its conditions use different operators.' };
  }
  if (conditions.length === 1) {
    const value = conditions[0].value || '';
    if (usesTerms(operator) && /"/.test(value)) {
      return { reason: 'The value contains a double quote.' };
    }
    return {
      simple: {
        field,
        operator,
        text: usesTerms(operator) ? joinTerms([value].filter(Boolean)) : value,
        termMode: 'any'
      }
    };
  }
  if (!usesTerms(operator)) {
    return { reason: 'Several conditions are only supported for Contains and Not contains.' };
  }
  const joins = new Set(conditions.slice(1).map(c => c.join || 'AND'));
  if (joins.size > 1) {
    return { reason: 'Its conditions mix AND and OR.' };
  }
  const join = [...joins][0];
  if (operator === 'notContains' && join !== 'AND') {
    return { reason: 'Not contains terms can only be combined with AND.' };
  }
  if (conditions.some(c => /"/.test(c.value || ''))) {
    return { reason: 'A value contains a double quote.' };
  }
  return {
    simple: {
      field,
      operator,
      text: joinTerms(conditions.map(c => c.value || '').filter(Boolean)),
      termMode: join === 'AND' ? 'all' : 'any'
    }
  };
};

// FR-10: field name when every row uses the same field, otherwise "Mixed".
export const groupFieldSummary = group => {
  const fields = new Set((group?.conditions || []).map(c => c.field));
  if (fields.size === 0) {
    return '';
  }
  return fields.size === 1 ? fieldLabel([...fields][0]) : 'Mixed';
};

const stripFirstJoin = (items, build) =>
  items.map((item, index) => {
    const built = build(item);
    if (index === 0) {
      delete built.join;
    } else {
      built.join = item.join || 'AND';
    }
    return built;
  });

const conditionsToExpressionGroups = groups =>
  stripFirstJoin(
    groups.filter(g => g.conditions && g.conditions.length > 0),
    group => ({
      conditions: stripFirstJoin(group.conditions, c => ({
        field: c.field,
        operator: c.operator,
        value: c.value
      }))
    })
  );

// Draft -> API expression, or null when there is nothing to search.
export const draftToExpression = draft => {
  const groups =
    draft.mode === 'simple'
      ? conditionsToExpressionGroups(
          simpleToConditions(draft.simple).length > 0
            ? [{ conditions: simpleToConditions(draft.simple) }]
            : []
        )
      : conditionsToExpressionGroups(draft.advanced?.groups || []);
  return groups.length > 0 ? { version: SEARCH_QUERY_VERSION, groups } : null;
};

// Applied expression (URL / legacy) -> draft; Simple when it can be represented, else Advanced.
export const expressionToDraft = expression => {
  if (!expression || !expression.groups || expression.groups.length === 0) {
    return emptyDraft();
  }
  const advanced = {
    groups: expression.groups.map((group, gi) => ({
      join: gi === 0 ? undefined : group.join || 'AND',
      conditions: group.conditions.map((c, ci) => ({
        join: ci === 0 ? undefined : c.join || 'AND',
        field: c.field,
        operator: c.operator,
        value: c.value
      }))
    }))
  };
  const converted = advancedToSimple(advanced);
  if (converted.simple) {
    // Make sure the Simple round trip yields the very same expression.
    const roundTrip = draftToExpression({ mode: 'simple', simple: converted.simple });
    if (JSON.stringify(roundTrip) === JSON.stringify(expression)) {
      return { mode: 'simple', simple: converted.simple, advanced: null };
    }
  }
  return { mode: 'advanced', simple: emptySimple(), advanced };
};

const countConditions = groups => groups.reduce((n, g) => n + g.conditions.length, 0);

const regexHint = value => {
  if (/\(\?<?[=!]|\\[1-9]/.test(value)) {
    return 'Lookarounds and backreferences are not supported.';
  }
  try {
    new RegExp(value, 'i');
  } catch (e) {
    return 'Invalid regular expression.';
  }
  return null;
};

// Returns a list of { path, message }; empty when the draft can be applied.
export const validateDraft = (draft, limits = DEFAULT_LIMITS) => {
  const errors = [];
  const groups =
    draft.mode === 'simple'
      ? [{ conditions: simpleToConditions(draft.simple) }]
      : draft.advanced?.groups || [];

  if (groups.length > limits.maxGroups) {
    errors.push({ path: 'groups', message: `At most ${limits.maxGroups} groups are allowed.` });
  }
  if (countConditions(groups) > limits.maxConditions) {
    errors.push({
      path: 'conditions',
      message: `At most ${limits.maxConditions} conditions are allowed.`
    });
  }
  groups.forEach((group, gi) => {
    if (draft.mode === 'advanced' && group.conditions.length === 0) {
      errors.push({ path: `groups.${gi}`, message: 'A group needs at least one condition.' });
    }
    group.conditions.forEach((c, ci) => {
      const path = `groups.${gi}.conditions.${ci}`;
      const value = c.value || '';
      if (value.length === 0 && c.operator !== 'equals') {
        errors.push({ path, message: 'A value is required.' });
      } else if (value.length > limits.maxValueLength) {
        errors.push({
          path,
          message: `A value can have at most ${limits.maxValueLength} characters.`
        });
      } else if (c.operator === 'matchesRegex') {
        const hint = regexHint(value);
        if (hint) {
          errors.push({ path, message: hint });
        }
      }
    });
  });
  return errors;
};
