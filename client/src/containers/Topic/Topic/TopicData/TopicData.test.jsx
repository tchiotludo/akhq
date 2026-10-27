import { describe, expect, it, vi } from 'vitest';
import { TopicData } from './TopicData';

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
    topicData.state.search.key.text = 'abc';

    topicData._navigateWithCurrentFilters(false);

    expect(topicData.props.router.navigate).toHaveBeenCalledOnce();
    expect(topicData._searchMessages).not.toHaveBeenCalled();
  });
});
