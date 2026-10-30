import { describe, expect, it } from 'vitest';
import {
  MAX_SHAREABLE_LENGTH,
  decodeSearchQuery,
  encodeSearchQuery,
  isShareable,
  legacyQueryToExpression,
  legacyToExpression
} from './searchQuery';

const expression = {
  version: 1,
  groups: [
    {
      conditions: [
        { field: 'value', operator: 'contains', value: 'héllo wörld 张伟' },
        { join: 'OR', field: 'key', operator: 'matchesRegex', value: '^a+/b?$' }
      ]
    }
  ]
};

describe('searchQuery codec', () => {
  it('round trips with the v1 prefix and url-safe characters', () => {
    const encoded = encodeSearchQuery(expression);
    expect(encoded.startsWith('v1.')).toBe(true);
    expect(encoded).toMatch(/^v1\.(z\.)?[A-Za-z0-9_-]+$/);
    expect(decodeSearchQuery(encoded)).toEqual({ expression });
  });

  it('rejects unknown versions', () => {
    expect(decodeSearchQuery('v2.abc').error).toMatch(/version/);
    expect(decodeSearchQuery('abc').error).toMatch(/version/);
  });

  it('rejects corrupted or invalid payloads without throwing', () => {
    expect(decodeSearchQuery('v1.!!!').error).toBeDefined();
    expect(decodeSearchQuery('v1.AAAA').error).toBeDefined();
    expect(decodeSearchQuery('').error).toBeDefined();
    expect(decodeSearchQuery(undefined).error).toBeDefined();
  });

  it('rejects a payload that is not a list of groups', () => {
    const wrong = 'v1.' + btoa('{"a":1}').replace(/=+$/, '');
    expect(decodeSearchQuery(wrong).error).toBeDefined();
  });

  it('keeps a typical query short and deflates large ones', () => {
    const small = {
      version: 1,
      groups: [{ conditions: [{ field: 'value', operator: 'contains', value: 'error' }] }]
    };
    expect(encodeSearchQuery(small).length).toBeLessThan(40);
    const repeated = {
      version: 1,
      groups: [
        {
          conditions: Array.from({ length: 20 }, (_, i) => ({
            ...(i ? { join: 'OR' } : {}),
            field: 'value',
            operator: 'contains',
            value: 'repeated-value-repeated-value'
          }))
        }
      ]
    };
    const encoded = encodeSearchQuery(repeated);
    expect(encoded.startsWith('v1.z.')).toBe(true);
    expect(decodeSearchQuery(encoded)).toEqual({ expression: repeated });
  });

  it('flags queries over the shareable cap', () => {
    const big = {
      version: 1,
      groups: [
        {
          conditions: Array.from({ length: 50 }, (_, i) => ({
            join: i ? 'OR' : undefined,
            field: 'value',
            operator: 'contains',
            value: Array.from({ length: 12 }, () => Math.random().toString(36).slice(2)).join('')
          }))
        }
      ]
    };
    const encoded = encodeSearchQuery(big);
    expect(encoded.length).toBeGreaterThan(MAX_SHAREABLE_LENGTH);
    expect(isShareable(encoded)).toBe(false);
    expect(isShareable(encodeSearchQuery(expression))).toBe(true);
  });
});

describe('legacy conversion', () => {
  it('converts fields to AND groups and contains terms to OR conditions', () => {
    expect(
      legacyToExpression({
        key: { text: 'a b', type: 'C' },
        value: { text: 'x y', type: 'N' },
        headerKey: { text: '', type: 'C' }
      })
    ).toEqual({
      version: 1,
      groups: [
        {
          conditions: [
            { field: 'key', operator: 'contains', value: 'a' },
            { join: 'OR', field: 'key', operator: 'contains', value: 'b' }
          ]
        },
        { join: 'AND', conditions: [{ field: 'value', operator: 'notContains', value: 'x y' }] }
      ]
    });
  });

  it('returns null when nothing is searched', () => {
    expect(legacyToExpression({})).toBeNull();
  });

  it('reads legacy URL parameters', () => {
    const query = new URLSearchParams('searchByValue=foo_E&searchByKey=bar_C');
    expect(legacyQueryToExpression(query)).toEqual({
      version: 1,
      groups: [
        { conditions: [{ field: 'key', operator: 'contains', value: 'bar' }] },
        { join: 'AND', conditions: [{ field: 'value', operator: 'equals', value: 'foo' }] }
      ]
    });
  });
});
