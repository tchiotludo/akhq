import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TopicList } from './TopicList';

const createTopicList = (pageNumber, locationSearch = '') => {
  const topicList = new TopicList({
    params: { clusterId: 'cluster-a' },
    router: { navigate: vi.fn() },
    location: { pathname: '/ui/cluster-a/topic', search: locationSearch }
  });

  topicList.state = {
    ...topicList.state,
    selectedCluster: 'cluster-a',
    pageNumber,
    currentPageSize: 25,
    searchData: { search: '', topicListView: 'HIDE_INTERNAL' },
    topics: [{ id: 'payments', favorite: false }],
    favorites: [],
    roles: { TOPIC_DATA: [] },
    uiOptions: { topic: { skipConsumerGroups: true, skipLastRecord: true } }
  };
  topicList.setState = (update, callback) => {
    const nextState = typeof update === 'function' ? update(topicList.state) : update;
    topicList.state = { ...topicList.state, ...nextState };
    callback?.();
  };
  topicList.getTopics = vi.fn();

  return topicList;
};

describe('TopicList favourites', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('updates the star immediately and refreshes in place when page one keeps the url', () => {
    const topicList = createTopicList(
      1,
      '?search=&topicListView=HIDE_INTERNAL&page=1&uiPageSize=25'
    );

    topicList.handleFavorite({ id: 'payments' });

    expect(topicList.state.topics).toEqual([{ id: 'payments', favorite: true }]);
    expect(topicList.state.preserveTopicsDuringNextLoad).toBe(true);
    expect(topicList.props.router.navigate).not.toHaveBeenCalled();
    expect(topicList.getTopics).toHaveBeenCalledOnce();
  });

  it('lets the page-reset navigation perform the single refresh from later pages', () => {
    const topicList = createTopicList(
      2,
      '?search=&topicListView=HIDE_INTERNAL&page=2&uiPageSize=25'
    );

    topicList.handleFavorite({ id: 'payments' });

    expect(topicList.state.pageNumber).toBe(1);
    expect(topicList.props.router.navigate).toHaveBeenCalledOnce();
    expect(topicList.getTopics).not.toHaveBeenCalled();
  });

  it('passes stored favourites to the topic request', async () => {
    const topicList = createTopicList(1);
    topicList.state.favorites = ['payments', 'orders'];
    topicList.getApi = vi.fn().mockResolvedValue({
      data: {
        results: [],
        page: 1,
        pageSize: 25
      }
    });

    await TopicList.prototype.getTopics.call(topicList);

    const request = new URL(topicList.getApi.mock.calls[0][0], 'http://localhost');
    expect(request.searchParams.getAll('favorite')).toEqual(['payments', 'orders']);
  });

  it('marks rows whose names are stored as favourites', () => {
    const topicList = createTopicList(1);
    topicList.state.favorites = ['payments'];

    topicList.handleTopics([
      {
        name: 'payments',
        size: 0,
        logDirSize: 0,
        partitions: [],
        replicaCount: 1,
        inSyncReplicaCount: 1,
        internal: false
      },
      {
        name: 'orders',
        size: 0,
        logDirSize: 0,
        partitions: [],
        replicaCount: 1,
        inSyncReplicaCount: 1,
        internal: false
      }
    ]);

    expect(topicList.state.topics.map(topic => [topic.id, topic.favorite])).toEqual([
      ['payments', true],
      ['orders', false]
    ]);
  });
});

describe('TopicList search submission', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('refreshes in place when re-submitting an unchanged search', () => {
    const topicList = createTopicList(
      1,
      '?search=abc&topicListView=HIDE_INTERNAL&page=1&uiPageSize=25'
    );

    topicList.handleSearch({
      searchData: { search: 'abc', topicListView: 'HIDE_INTERNAL' },
      keepSearch: false
    });

    expect(topicList.state.pageNumber).toBe(1);
    expect(topicList.props.router.navigate).not.toHaveBeenCalled();
    expect(topicList.getTopics).toHaveBeenCalledOnce();
  });

  it('navigates when the submitted search changes the url', () => {
    const topicList = createTopicList(
      1,
      '?search=abc&topicListView=HIDE_INTERNAL&page=1&uiPageSize=25'
    );

    topicList.handleSearch({
      searchData: { search: 'xyz', topicListView: 'HIDE_INTERNAL' },
      keepSearch: false
    });

    expect(topicList.props.router.navigate).toHaveBeenCalledOnce();
    expect(topicList.getTopics).not.toHaveBeenCalled();
  });
});
