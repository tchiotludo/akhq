import { describe, expect, it, vi } from 'vitest';
import { ConnectList } from './ConnectList';

const createConnectList = (state, locationSearch = '') => {
  const list = new ConnectList({
    params: { clusterId: 'cluster-a', connectId: 'connect-a' },
    router: { navigate: vi.fn() },
    location: { pathname: '/ui/cluster-a/connect/connect-a', search: locationSearch }
  });

  list.state = {
    ...list.state,
    clusterId: 'cluster-a',
    connectId: 'connect-a',
    ...state
  };
  list.setState = (update, callback) => {
    const nextState = typeof update === 'function' ? update(list.state) : update;
    list.state = { ...list.state, ...nextState };
    callback?.();
  };
  list.getConnectDefinitions = vi.fn();

  return list;
};

describe('ConnectList search submission', () => {
  it('refreshes in place when re-submitting an unchanged search', () => {
    const list = createConnectList(
      { searchData: { search: 'abc' }, pageNumber: 1, statusFilter: '' },
      '?search=abc&page=1'
    );

    list.handleSearch({ searchData: { search: 'abc' } });

    expect(list.props.router.navigate).not.toHaveBeenCalled();
    expect(list.getConnectDefinitions).toHaveBeenCalledOnce();
  });

  it('navigates when the submitted search changes the url', () => {
    const list = createConnectList(
      { searchData: { search: 'abc' }, pageNumber: 1, statusFilter: '' },
      '?search=abc&page=1'
    );

    list.handleSearch({ searchData: { search: 'xyz' } });

    expect(list.props.router.navigate).toHaveBeenCalledOnce();
    expect(list.getConnectDefinitions).not.toHaveBeenCalled();
  });
});
