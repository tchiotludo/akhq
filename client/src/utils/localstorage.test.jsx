import { beforeEach, describe, expect, it } from 'vitest';
import { getTopicFavorites, toggleTopicFavorite } from './localstorage';

describe('topic favourites', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('keeps favourites ordered and scoped to a cluster', () => {
    toggleTopicFavorite('cluster-a', 'payments');
    toggleTopicFavorite('cluster-a', 'customers');
    toggleTopicFavorite('cluster-b', 'payments');

    expect(getTopicFavorites('cluster-a')).toEqual(['payments', 'customers']);
    expect(getTopicFavorites('cluster-b')).toEqual(['payments']);
  });

  it('removes an existing favourite', () => {
    toggleTopicFavorite('cluster-a', 'payments');
    toggleTopicFavorite('cluster-a', 'payments');

    expect(getTopicFavorites('cluster-a')).toEqual([]);
  });

  it.each(['not-json', '[]', '"text"', '42'])('ignores malformed stored data: %s', stored => {
    localStorage.setItem('topicFavorites', stored);

    expect(getTopicFavorites('cluster-a')).toEqual([]);
    expect(toggleTopicFavorite('cluster-a', 'payments')).toEqual(['payments']);
  });

  it('ignores a malformed cluster value', () => {
    localStorage.setItem('topicFavorites', JSON.stringify({ 'cluster-a': 'payments' }));

    expect(getTopicFavorites('cluster-a')).toEqual([]);
  });

  it('normalizes malformed entries and duplicates', () => {
    localStorage.setItem(
      'topicFavorites',
      JSON.stringify({ 'cluster-a': ['payments', 42, '', 'payments', null, 'orders'] })
    );

    expect(getTopicFavorites('cluster-a')).toEqual(['payments', 'orders']);
  });

  it('does not add a new favourite beyond the per-cluster limit', () => {
    const favorites = Array.from({ length: 100 }, (_, index) => `topic-${index}`);
    localStorage.setItem('topicFavorites', JSON.stringify({ 'cluster-a': favorites }));

    expect(toggleTopicFavorite('cluster-a', 'new-topic')).toEqual(favorites);
  });

  it.each(['', null, 42])('ignores invalid new favourites: %s', topic => {
    expect(toggleTopicFavorite('cluster-a', topic)).toEqual([]);
    expect(getTopicFavorites('cluster-a')).toEqual([]);
  });
});
