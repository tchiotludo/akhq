import { describe, expect, it, vi } from 'vitest';
import { SchemaList } from './SchemaList';

const createSchemaList = (state, locationSearch = '') => {
  const list = new SchemaList({
    params: { clusterId: 'cluster-a' },
    router: { navigate: vi.fn() },
    location: { pathname: '/ui/cluster-a/schema', search: locationSearch }
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
  list.getSchemaRegistry = vi.fn();

  return list;
};

describe('SchemaList search submission', () => {
  it('refreshes in place when re-submitting an unchanged search', () => {
    const list = createSchemaList(
      { searchData: { search: 'abc' }, pageNumber: 1 },
      '?search=abc&page=1'
    );

    list.handleSearch({ searchData: { search: 'abc' } });

    expect(list.props.router.navigate).not.toHaveBeenCalled();
    expect(list.getSchemaRegistry).toHaveBeenCalledOnce();
  });

  it('navigates when the submitted search changes the url', () => {
    const list = createSchemaList(
      { searchData: { search: 'abc' }, pageNumber: 1 },
      '?search=abc&page=1'
    );

    list.handleSearch({ searchData: { search: 'xyz' } });

    expect(list.props.router.navigate).toHaveBeenCalledOnce();
    expect(list.getSchemaRegistry).not.toHaveBeenCalled();
  });
});
