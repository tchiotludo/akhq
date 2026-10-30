import { describe, expect, it } from 'vitest';
import {
  advancedToSimple,
  draftToExpression,
  emptyDraft,
  expressionToDraft,
  groupFieldSummary,
  simpleToAdvanced,
  splitTerms,
  validateDraft
} from './searchModel';

const simple = (over = {}) => ({
  field: 'value',
  operator: 'contains',
  text: '',
  termMode: 'any',
  ...over
});
const cond = (value, over = {}) => ({
  join: 'OR',
  field: 'value',
  operator: 'contains',
  value,
  ...over
});

describe('splitTerms', () => {
  it('splits on whitespace and keeps quoted phrases', () => {
    expect(splitTerms('a  "b c" d')).toEqual(['a', 'b c', 'd']);
    expect(splitTerms('  ')).toEqual([]);
    expect(splitTerms('""  x')).toEqual(['x']);
  });
});

describe('simple -> expression / advanced (FR-05, FR-08)', () => {
  it('returns null for an empty simple search', () => {
    expect(draftToExpression({ mode: 'simple', simple: simple() })).toBeNull();
  });

  it('Contains/Any becomes OR conditions', () => {
    const exp = draftToExpression({ mode: 'simple', simple: simple({ text: 'a "b c"' }) });
    expect(exp).toEqual({
      version: 1,
      groups: [
        {
          conditions: [
            { field: 'value', operator: 'contains', value: 'a' },
            { join: 'OR', field: 'value', operator: 'contains', value: 'b c' }
          ]
        }
      ]
    });
  });

  it('Contains/All and Not contains become AND conditions', () => {
    const all = simpleToAdvanced(simple({ text: 'a b', termMode: 'all' }));
    expect(all.groups[0].conditions.map(c => c.join)).toEqual([undefined, 'AND']);
    const not = simpleToAdvanced(simple({ text: 'a b', operator: 'notContains' }));
    expect(not.groups[0].conditions.map(c => c.join)).toEqual([undefined, 'AND']);
  });

  it('keeps regex and equals values as one condition', () => {
    const rx = simpleToAdvanced(simple({ text: 'a b+', operator: 'matchesRegex' }));
    expect(rx.groups[0].conditions).toHaveLength(1);
    expect(rx.groups[0].conditions[0].value).toBe('a b+');
    const eq = simpleToAdvanced(simple({ text: 'a b', operator: 'equals' }));
    expect(eq.groups[0].conditions[0].value).toBe('a b');
  });

  it('keeps an empty row in Advanced for an empty simple draft', () => {
    const adv = simpleToAdvanced(simple({ field: 'key' }));
    expect(adv.groups[0].conditions).toEqual([
      { join: undefined, field: 'key', operator: 'contains', value: '' }
    ]);
  });
});

describe('advanced -> simple (FR-09)', () => {
  const group = conditions => ({ groups: [{ conditions }] });

  it('converts one condition', () => {
    expect(advancedToSimple(group([cond('x phrase')])).simple.text).toBe('"x phrase"');
  });

  it('converts uniform Contains conditions (OR -> any, AND -> all)', () => {
    expect(advancedToSimple(group([cond('a'), cond('b')])).simple).toMatchObject({
      text: 'a b',
      termMode: 'any'
    });
    expect(advancedToSimple(group([cond('a'), cond('b', { join: 'AND' })])).simple.termMode).toBe(
      'all'
    );
  });

  it('round trips simple -> advanced -> simple', () => {
    const s = simple({ text: 'a "b c" d', termMode: 'all' });
    expect(advancedToSimple(simpleToAdvanced(s)).simple).toEqual(s);
  });

  it.each([
    ['several groups', { groups: [{ conditions: [cond('a')] }, { conditions: [cond('b')] }] }],
    ['mixed fields', group([cond('a'), cond('b', { field: 'key' })])],
    ['mixed operators', group([cond('a'), cond('b', { operator: 'equals' })])],
    ['mixed joins', group([cond('a'), cond('b'), cond('c', { join: 'AND' })])],
    [
      'several equals',
      group([cond('a', { operator: 'equals' }), cond('b', { operator: 'equals' })])
    ],
    [
      'not contains with OR',
      group([cond('a', { operator: 'notContains' }), cond('b', { operator: 'notContains' })])
    ],
    ['quote in value', group([cond('a"b')])]
  ])('refuses %s with a reason', (_, advanced) => {
    const result = advancedToSimple(advanced);
    expect(result.simple).toBeUndefined();
    expect(result.reason).toBeTruthy();
  });

  it('treats an empty advanced draft as an empty simple draft', () => {
    expect(advancedToSimple({ groups: [] }).simple.text).toBe('');
  });
});

describe('advanced expression / summary (FR-06, FR-10)', () => {
  it('strips the join of the first group and first condition', () => {
    const exp = draftToExpression({
      mode: 'advanced',
      advanced: {
        groups: [
          { join: 'OR', conditions: [cond('a', { join: 'AND' }), cond('b')] },
          { join: 'OR', conditions: [cond('c', { join: 'AND' })] }
        ]
      }
    });
    expect(exp.groups[0].join).toBeUndefined();
    expect(exp.groups[0].conditions[0].join).toBeUndefined();
    expect(exp.groups[0].conditions[1].join).toBe('OR');
    expect(exp.groups[1].join).toBe('OR');
    expect(exp.groups[1].conditions[0].join).toBeUndefined();
  });

  it('summarises the group scope', () => {
    expect(groupFieldSummary({ conditions: [cond('a'), cond('b')] })).toBe('Value');
    expect(groupFieldSummary({ conditions: [cond('a'), cond('b', { field: 'key' })] })).toBe(
      'Mixed'
    );
    expect(groupFieldSummary({ conditions: [] })).toBe('');
  });
});

describe('expressionToDraft', () => {
  it('returns an empty simple draft for no expression', () => {
    expect(expressionToDraft(null)).toEqual(emptyDraft());
  });

  it('restores a simple draft when representable', () => {
    const exp = draftToExpression({ mode: 'simple', simple: simple({ text: 'a b' }) });
    expect(expressionToDraft(exp)).toMatchObject({ mode: 'simple', simple: { text: 'a b' } });
  });

  it('restores an advanced draft otherwise and round trips', () => {
    const exp = {
      version: 1,
      groups: [
        { conditions: [{ field: 'key', operator: 'equals', value: 'k' }] },
        { join: 'OR', conditions: [{ field: 'value', operator: 'matchesRegex', value: 'x+' }] }
      ]
    };
    const draft = expressionToDraft(exp);
    expect(draft.mode).toBe('advanced');
    expect(draftToExpression(draft)).toEqual(exp);
  });
});

describe('validateDraft', () => {
  const adv = conditions => ({ mode: 'advanced', advanced: { groups: [{ conditions }] } });

  it('accepts valid drafts and the empty simple draft', () => {
    expect(validateDraft({ mode: 'simple', simple: simple() })).toEqual([]);
    expect(validateDraft(adv([cond('a')]))).toEqual([]);
  });

  it('rejects empty values except for equals', () => {
    expect(validateDraft(adv([cond('')]))).toHaveLength(1);
    expect(validateDraft(adv([cond('', { operator: 'equals' })]))).toEqual([]);
  });

  it('flags regex problems', () => {
    const rx = v => validateDraft(adv([cond(v, { operator: 'matchesRegex' })]));
    expect(rx('(')).toHaveLength(1);
    expect(rx('(?=a)')).toHaveLength(1);
    expect(rx('(a)\\1')).toHaveLength(1);
    expect(rx('^a+$')).toEqual([]);
  });

  it('enforces limits', () => {
    const limits = { maxGroups: 1, maxConditions: 2, maxValueLength: 3 };
    expect(validateDraft(adv([cond('abcd')]), limits)).toHaveLength(1);
    expect(validateDraft(adv([cond('a'), cond('b'), cond('c')]), limits)).toHaveLength(1);
    expect(
      validateDraft(
        {
          mode: 'advanced',
          advanced: { groups: [{ conditions: [cond('a')] }, { conditions: [cond('b')] }] }
        },
        limits
      )
    ).toHaveLength(1);
  });

  it('flags an empty group', () => {
    expect(
      validateDraft({ mode: 'advanced', advanced: { groups: [{ conditions: [] }] } })
    ).toHaveLength(1);
  });
});
