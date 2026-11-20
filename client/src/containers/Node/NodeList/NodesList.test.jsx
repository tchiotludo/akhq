import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { NodesList } from './NodesList';

vi.mock('../../Header', () => ({ default: ({ title }) => <h1>{title}</h1> }));
afterEach(cleanup);

function instance() {
  const list = new NodesList({ params: { clusterId: 'test' } });
  list.setState = update => {
    list.state = { ...list.state, ...update };
  };
  return list;
}

describe('Nodes loading', () => {
  it('retains brokers when quorum and partition queries fail', async () => {
    const list = instance();
    list.getApi = async url => {
      if (url.endsWith('/quorum') || url.endsWith('/partitions')) throw new Error('not available');
      return { data: { nodes: [{ id: 0, host: 'broker', port: 9092 }], controller: { id: 0 } } };
    };
    await list.getNodes();
    expect(list.state.data[0].id).toBe(0);
    expect(list.state.quorum.status).toBe('UNAVAILABLE');
    expect(list.state.loading).toBe(false);
    expect(list.state.data[0].partition).toBe('Unavailable');
    // A broker compatibility controller ID is never used as the KRaft leader.
    expect(list.state.data[0].controller).toBeUndefined();
  });

  it('ignores an earlier quorum response after switching clusters', async () => {
    const list = instance();
    let resolveOld;
    list.getApi = async url => {
      if (url.includes('/test/node/quorum'))
        return new Promise(resolve => {
          resolveOld = resolve;
        });
      if (url.endsWith('/quorum'))
        return { data: { status: 'AVAILABLE', leaderId: 200, voters: [] } };
      if (url.endsWith('/partitions')) return { data: [] };
      return { data: { nodes: [] } };
    };
    const old = list.getNodes();
    list.props = { params: { clusterId: 'new' } };
    await list.getNodes();
    resolveOld({ data: { status: 'AVAILABLE', leaderId: 100, voters: [] } });
    await old;
    expect(list.state.selectedCluster).toBe('new');
    expect(list.state.quorum.leaderId).toBe(200);
  });

  it('ignores partition statistics for brokers absent from the current list', async () => {
    const list = instance();
    list.getApi = async () => ({ data: [{ id: 999, countLeader: 0, totalPartitions: 0 }] });
    await list.handleData({ nodes: [{ id: 1, host: 'broker', port: 9092 }] });
    expect(list.state.data).toHaveLength(1);
    expect(list.state.data[0].partition).toBe('0 (0.00%)');
  });

  it('finishes loading statistics when a broker has no partitions', async () => {
    const list = instance();
    list.getApi = async () => ({ data: [{ id: 1, countLeader: 0, totalPartitions: 0 }] });
    await list.handleData({ nodes: [{ id: 1, host: 'broker', port: 9092 }] });
    expect(list.state.data[0].partition).toBe('0 (0.00%)');
    expect(list.state.loading).toBe(false);
  });
});

function renderLayout(brokerIds, voterIds, extra = {}) {
  const list = instance();
  list.state = {
    ...list.state,
    selectedCluster: 'test',
    loading: false,
    quorumLoading: false,
    data: brokerIds.map(id => ({ id, host: `node${id}:9092`, rack: '', partition: '1 (33.33%)' })),
    quorum: {
      status: 'AVAILABLE',
      leaderId: voterIds[0],
      voters: voterIds.map(id => ({ id, leader: id === voterIds[0], endpoints: [] }))
    },
    ...extra
  };
  render(<MemoryRouter>{list.render()}</MemoryRouter>);
  return screen.getAllByRole('table');
}

describe('node roles across cluster layouts', () => {
  it('shows combined roles in both views and active status only in the controller view', () => {
    const [brokers, controllers] = renderLayout([0, 1, 2], [0, 1, 2]);
    expect(within(brokers).getAllByText('Broker')).toHaveLength(3);
    expect(within(brokers).getAllByText('Controller')).toHaveLength(3);
    expect(within(brokers).queryByText('KRaft leader')).toBeNull();
    expect(within(brokers).queryByText('Active')).toBeNull();
    expect(within(brokers).queryByText('False')).toBeNull();
    expect(within(controllers).getAllByText('Active')).toHaveLength(1);
    expect(within(controllers).getAllByText('Not active')).toHaveLength(2);
    expect(screen.getByRole('region', { name: 'Visible node roles' }).textContent).toMatch(
      /Both roles\s*3/
    );
    expect(within(brokers).getAllByRole('link')[0].getAttribute('href')).toBe('/ui/test/node/0');
  });

  it('keeps dedicated broker and controller roles separate', () => {
    const [brokers, controllers] = renderLayout([1, 2, 3], [100, 101, 102]);
    expect(within(brokers).queryByText('Controller')).toBeNull();
    expect(within(controllers).queryByText('Broker')).toBeNull();
    expect(screen.getByRole('region', { name: 'Visible node roles' }).textContent).toMatch(
      /Both roles\s*0/
    );
  });

  it('identifies the shared node in a mixed layout without assigning the role to other brokers', () => {
    const [brokers] = renderLayout([0, 1, 2], [0, 100, 101]);
    expect(within(brokers).getAllByText('Controller')).toHaveLength(1);
    const combinedRow = within(brokers).getByText('Controller').closest('tr');
    expect(within(combinedRow).getByText('0')).toBeTruthy();
    expect(screen.getByRole('region', { name: 'Visible node roles' }).textContent).toMatch(
      /Both roles\s*1/
    );
  });

  it('preserves brokers and marks controller discovery unknown after a quorum timeout', () => {
    const [brokers] = renderLayout([1, 2, 3], [], { quorum: { status: 'TIMED_OUT' } });
    expect(within(brokers).getAllByText('Broker')).toHaveLength(3);
    expect(within(brokers).getAllByText('Controller role unknown')).toHaveLength(3);
    expect(screen.getAllByRole('table')).toHaveLength(1);
    expect(screen.getByRole('region', { name: 'Visible node roles' }).textContent).toMatch(
      /Both roles\s*Unknown/
    );
  });

  it('preserves controller membership when broker discovery fails', () => {
    renderLayout([], [0, 1, 2], { nodeError: true });
    const controllers = within(screen.getByRole('region', { name: 'KRaft controllers' })).getByRole(
      'table'
    );
    expect(within(controllers).getAllByText('Broker role unknown')).toHaveLength(3);
    expect(within(controllers).getByText('Active')).toBeTruthy();
    expect(screen.getByText('Broker information could not be loaded.').getAttribute('role')).toBe(
      'alert'
    );
    expect(screen.getByRole('region', { name: 'Visible node roles' }).textContent).toMatch(
      /Brokers\s*Unknown/
    );
  });
});
