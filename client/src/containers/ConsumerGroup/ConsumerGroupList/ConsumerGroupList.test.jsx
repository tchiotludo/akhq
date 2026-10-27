import { describe, expect, it, vi } from 'vitest';
import { ConsumerGroupList } from './ConsumerGroupList';

const createConsumerGroupList = (state, locationSearch = '') => {
  const list = new ConsumerGroupList({
    params: { clusterId: 'cluster-a' },
    router: { navigate: vi.fn() },
    location: { pathname: '/ui/cluster-a/group', search: locationSearch }
  });

  list.state = {
    ...list.state,
    selectedCluster: 'cluster-a',
    ...state
  };
  list.setState = (update, callback) => {
    const nextState = typeof update === 'function' ? update(list.state) : update;
    list.state = { ...list.state, ...nextState };
    callback?.();
  };
  list.getConsumerGroup = vi.fn();

  return list;
};

describe('ConsumerGroupList search submission', () => {
  it('refreshes in place when re-submitting an unchanged search', () => {
    const list = createConsumerGroupList({ search: 'abc', pageNumber: 1 }, '?search=abc&page=1');

    list.handleSearch({ searchData: { search: 'abc' } });

    expect(list.props.router.navigate).not.toHaveBeenCalled();
    expect(list.getConsumerGroup).toHaveBeenCalledOnce();
  });

  it('navigates when the submitted search changes the url', () => {
    const list = createConsumerGroupList({ search: 'abc', pageNumber: 1 }, '?search=abc&page=1');

    list.handleSearch({ searchData: { search: 'xyz' } });

    expect(list.props.router.navigate).toHaveBeenCalledOnce();
    expect(list.getConsumerGroup).not.toHaveBeenCalled();
  });
});
