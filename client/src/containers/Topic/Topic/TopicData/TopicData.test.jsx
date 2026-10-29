import { describe, expect, it, vi } from 'vitest';
import { streamSearch } from '../../../../utils/sseClient';
import { encodeSearchQuery } from '../../../../utils/searchQuery';
import { TopicData } from './TopicData';

vi.mock('../../../../utils/sseClient', async importOriginal => ({
  ...(await importOriginal()),
  streamSearch: vi.fn(() => new Promise(() => {}))
}));

const expression = {
  version: 1,
  groups: [{ conditions: [{ field: 'key', operator: 'contains', value: 'abc' }] }]
};

const createTopicData = (locationSearch = '') => {
  const topicData = new TopicData({
    params: { clusterId: 'cluster-a', topicId: 'topic-a' },
    router: { navigate: vi.fn() },
    location: { pathname: '/ui/cluster-a/topic/topic-a/data', search: locationSearch }
  });

  topicData.state = {
    ...topicData.state,
    selectedCluster: 'cluster-a',
    selectedTopic: 'topic-a'
  };
  topicData.setState = (update, callback) => {
    const nextState = typeof update === 'function' ? update(topicData.state) : update;
    topicData.state = { ...topicData.state, ...nextState };
    callback?.();
  };
  topicData._stopEventSource = vi.fn();
  topicData._searchMessages = vi.fn();
  topicData._getSingleMessage = vi.fn();

  return topicData;
};

describe('TopicData filter submission', () => {
  it('reloads in place when re-submitting unchanged filters', () => {
    const topicData = createTopicData('?sort=Oldest&partition=All&size=50');

    topicData._navigateWithCurrentFilters(false);

    expect(topicData.props.router.navigate).not.toHaveBeenCalled();
    expect(topicData._stopEventSource).toHaveBeenCalledWith(false);
    expect(topicData._searchMessages).toHaveBeenCalledWith(false);
  });

  it('navigates when the submitted filters change the url', () => {
    const topicData = createTopicData('?sort=Oldest&partition=All&size=50');
    topicData.state.applied = expression;

    topicData._navigateWithCurrentFilters(false);

    expect(topicData.props.router.navigate).toHaveBeenCalledOnce();
    expect(topicData.props.router.navigate.mock.calls[0][0].search).toContain('searchQuery=v1.');
    expect(topicData._searchMessages).not.toHaveBeenCalled();
  });
});

describe('TopicData search expression', () => {
  it('applying a search stores it and navigates with a compact searchQuery', () => {
    const topicData = createTopicData('?sort=Oldest');

    topicData._applySearch(expression);

    expect(topicData.state.applied).toEqual(expression);
    expect(topicData.state.searchNotShareable).toBe(false);
    expect(topicData.props.router.navigate.mock.calls[0][0].search).toMatch(
      /searchQuery=v1\.[\w-]+/
    );
  });

  it('clearing resets draft and applied search and drops searchQuery from the url', () => {
    const topicData = createTopicData('?sort=Oldest&searchQuery=' + encodeSearchQuery(expression));
    topicData.state.applied = expression;
    topicData.state.draft = { mode: 'advanced', simple: {}, advanced: { groups: [] } };

    topicData._clearSearch();

    expect(topicData.state.applied).toBeNull();
    expect(topicData.state.draft.mode).toBe('simple');
    expect(topicData.props.router.navigate.mock.calls[0][0].search).not.toContain('searchQuery');
  });

  it('restores the search from searchQuery or legacy params and ignores corrupt ones', () => {
    const topicData = createTopicData();
    const fromQuery = q => topicData._searchStateFromQuery(new URLSearchParams(q), topicData.state);

    expect(fromQuery('searchQuery=' + encodeSearchQuery(expression)).applied).toEqual(expression);

    const legacy = fromQuery('searchByKey=abc_C');
    expect(legacy.applied).toEqual(expression);
    expect(legacy.draft.mode).toBe('simple');

    expect(fromQuery('searchQuery=v1.!!!').applied).toBeNull();
  });

  it('keeps a search too long for the url across navigations', () => {
    const topicData = createTopicData();
    topicData.state.applied = expression;
    topicData.state.searchNotShareable = true;

    expect(
      topicData._searchStateFromQuery(new URLSearchParams('size=10'), topicData.state)
    ).toEqual({});
  });

  it('streams the search with a POST body and scope-only query parameters', () => {
    const topicData = createTopicData('?sort=Oldest');
    topicData.state.applied = expression;
    topicData.state.sortBy = 'Oldest';

    topicData._startEventSource(false);

    const call = streamSearch.mock.calls[0][0];
    expect(call.body).toEqual(expression);
    expect(call.url).toContain('/data/search?sort=Oldest');
    expect(call.url).not.toContain('searchQuery');
  });

  it('downloads with the applied expression, not the draft', () => {
    const topicData = createTopicData();
    topicData.state.applied = expression;
    topicData.state.draft = {
      mode: 'simple',
      simple: { field: 'value', operator: 'contains', text: 'draft-only', termMode: 'any' },
      advanced: null
    };
    topicData.postApi = vi.fn(() => new Promise(() => {}));

    topicData._downloadAllMatchingFilters();

    expect(topicData.postApi).toHaveBeenCalledWith(
      expect.stringContaining('/data/download'),
      expression
    );
  });
});
