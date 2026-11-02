import { describe, expect, it } from 'vitest';
import { readSearchFromQuery, sameExpression, writeSearchToParams } from './searchUrl';
import { encodeSearchQuery } from '../../../../../utils/searchQuery';

const expression = {
  version: 1,
  groups: [{ conditions: [{ field: 'value', operator: 'contains', value: 'abc' }] }]
};

describe('searchUrl', () => {
  it('round trips an applied search through the URL and keeps other params', () => {
    const params = new URLSearchParams('sort=Oldest&partition=2');
    const { notShareable } = writeSearchToParams(params, expression);
    expect(notShareable).toBe(false);
    expect(params.get('sort')).toBe('Oldest');
    expect(params.get('partition')).toBe('2');
    expect(params.get('searchQuery')).toMatch(/^v1\./);

    const read = readSearchFromQuery(new URLSearchParams(params.toString()));
    expect(read).toEqual({ applied: expression, error: null, legacy: false });
  });

  it('reads nothing from a URL without search', () => {
    expect(readSearchFromQuery(new URLSearchParams('sort=Oldest'))).toEqual({
      applied: null,
      error: null,
      legacy: true
    });
  });

  it('reads legacy parameters', () => {
    const read = readSearchFromQuery(new URLSearchParams('searchByValue=abc_C'));
    expect(read.legacy).toBe(true);
    expect(read.applied).toEqual(expression);
  });

  it('prefers searchQuery over legacy parameters', () => {
    const q = new URLSearchParams('searchByKey=zzz_E');
    q.set('searchQuery', encodeSearchQuery(expression));
    expect(readSearchFromQuery(q).applied).toEqual(expression);
  });

  it('reports a corrupt searchQuery without throwing', () => {
    const read = readSearchFromQuery(new URLSearchParams('searchQuery=v1.!!!'));
    expect(read.applied).toBeNull();
    expect(read.error).toBeTruthy();
    expect(readSearchFromQuery(new URLSearchParams('searchQuery=v9.abc')).error).toMatch(/version/);
  });

  it('rewrites legacy parameters to searchQuery on write', () => {
    const params = new URLSearchParams('searchByValue=abc_C&searchByKey=k_E&size=10');
    writeSearchToParams(params, expression);
    expect(params.has('searchByValue')).toBe(false);
    expect(params.has('searchByKey')).toBe(false);
    expect(params.get('size')).toBe('10');
    expect(params.has('searchQuery')).toBe(true);
  });

  it('clears the search from the URL', () => {
    const params = new URLSearchParams('size=10');
    writeSearchToParams(params, expression);
    writeSearchToParams(params, null);
    expect(params.toString()).toBe('size=10');
    writeSearchToParams(params, { version: 1, groups: [] });
    expect(params.toString()).toBe('size=10');
  });

  it('omits an over-cap query and flags it as not shareable', () => {
    const big = {
      version: 1,
      groups: [
        {
          conditions: Array.from({ length: 50 }, (_, i) => ({
            ...(i ? { join: 'OR' } : {}),
            field: 'value',
            operator: 'contains',
            value: Array.from({ length: 12 }, () => Math.random().toString(36).slice(2)).join('')
          }))
        }
      ]
    };
    const params = new URLSearchParams('searchByValue=old_C');
    const { notShareable } = writeSearchToParams(params, big);
    expect(notShareable).toBe(true);
    expect(params.has('searchQuery')).toBe(false);
    expect(params.has('searchByValue')).toBe(false);
  });

  it('compares expressions structurally', () => {
    expect(sameExpression(expression, JSON.parse(JSON.stringify(expression)))).toBe(true);
    expect(sameExpression(null, undefined)).toBe(true);
    expect(sameExpression(expression, null)).toBe(false);
  });
});

describe('sameExpression key order', () => {
  it('ignores key order and undefined joins', () => {
    const a = {
      version: 1,
      groups: [
        {
          conditions: [
            { field: 'value', operator: 'contains', value: 'a' },
            { join: 'OR', field: 'value', operator: 'contains', value: 'b' }
          ]
        }
      ]
    };
    const b = {
      groups: [
        {
          conditions: [
            { value: 'a', operator: 'contains', field: 'value', join: undefined },
            { value: 'b', operator: 'contains', field: 'value', join: 'OR' }
          ]
        }
      ],
      version: 1
    };
    expect(sameExpression(a, b)).toBe(true);
  });
});

describe('simple search URL round trip', () => {
  it('stays in Simple mode after applying multiple terms', async () => {
    const { draftToExpression, emptyDraft, expressionToDraft } = await import('./searchModel');
    const draft = emptyDraft();
    draft.simple = { ...draft.simple, text: 'foo bar', termLogic: 'any' };
    const applied = draftToExpression(draft);
    const decoded = readSearchFromQuery(
      new URLSearchParams(writeSearchToParams(new URLSearchParams(), applied).params)
    ).applied;
    expect(expressionToDraft(decoded).mode).toBe('simple');
  });
});
