import { describe, expect, it } from 'vitest';
import { uriTopics } from './endpoints';

describe('uriTopics favourites', () => {
  it('encodes repeated favourites and search values', () => {
    const url = new URL(uriTopics('cluster-a', 'orders & payments', 'ALL', 2, 25, [
      'orders & payments',
      'café+events'
    ]));

    expect(url.searchParams.getAll('favorite')).toEqual(['orders & payments', 'café+events']);
    expect(url.searchParams.get('search')).toBe('orders & payments');
    expect(url.searchParams.get('show')).toBe('ALL');
    expect(url.searchParams.get('page')).toBe('2');
    expect(url.searchParams.get('uiPageSize')).toBe('25');
  });

  it('preserves the legacy page-size omission and supports an empty list', () => {
    const url = new URL(uriTopics('cluster-a', '', 'HIDE_INTERNAL', 1, 1));

    expect(url.searchParams.getAll('favorite')).toEqual([]);
    expect(url.searchParams.has('uiPageSize')).toBe(false);
  });

  it('bounds the serialized favourite query', () => {
    const favorites = Array.from({ length: 100 }, (_, index) => `topic-${index}-${'x'.repeat(100)}`);
    const url = uriTopics('cluster-a', '', 'ALL', 1, 25, favorites);

    expect(new URL(url).search.length).toBeLessThanOrEqual(4096);
    expect(new URL(url).searchParams.getAll('favorite')[0]).toBe(favorites[0]);
    expect(new URL(url).searchParams.getAll('favorite').length).toBeLessThan(favorites.length);
  });
});
