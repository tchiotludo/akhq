import React from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen, within } from '@testing-library/react';
import MetadataQuorum from './MetadataQuorum';

afterEach(cleanup);

const quorum = {
  status: 'AVAILABLE',
  leaderId: 100,
  leaderEpoch: 2,
  highWatermark: 0,
  fetchedAt: 1700000000000,
  voters: [
    { id: 100, leader: true, endpoints: [], logEndOffset: 0, lag: 0, lastFetchTimestamp: null },
    {
      id: 0,
      leader: false,
      endpoints: ['CONTROLLER://node0:9093'],
      logEndOffset: null,
      lag: null,
      lastFetchTimestamp: null
    }
  ]
};

describe('metadata quorum display', () => {
  it('does not assume controller-only roles when broker discovery failed', () => {
    render(<MetadataQuorum quorum={quorum} loading={false} brokerIds={null} />);
    expect(screen.getAllByText('Broker role unknown')).toHaveLength(2);
  });
  it('shows controller-only and combined voters without linking them to broker-only APIs', () => {
    const { container } = render(
      <MetadataQuorum quorum={quorum} loading={false} brokerIds={new Set([0])} />
    );
    expect(screen.getByText('Broker')).toBeTruthy();
    expect(screen.getAllByText('Controller')).toHaveLength(2);
    expect(screen.getByText('Not reported')).toBeTruthy();
    expect(screen.getByText('CONTROLLER://node0:9093')).toBeTruthy();
    expect(screen.getByText('Active')).toBeTruthy();
    expect(screen.getByText('Not active')).toBeTruthy();
    expect(container.querySelectorAll('a').length).toBe(0);
    expect(container.textContent).toContain('High watermark: 0');
    expect(container.textContent).not.toContain('Invalid Date');
  });

  it.each(['UNSUPPORTED', 'FORBIDDEN', 'TIMED_OUT', 'UNAVAILABLE'])(
    'explains %s without displaying a false leader',
    status => {
      render(<MetadataQuorum quorum={{ status }} loading={false} brokerIds={new Set()} />);
      expect(screen.getByRole('status').textContent.length).toBeGreaterThan(10);
      expect(screen.queryByRole('table')).toBeNull();
    }
  );

  it('distinguishes loading and an empty permission-filtered quorum', () => {
    const { rerender } = render(
      <MetadataQuorum quorum={null} loading={true} brokerIds={new Set()} />
    );
    expect(screen.getByRole('status').textContent).toContain('Loading');
    rerender(
      <MetadataQuorum
        quorum={{ ...quorum, leaderId: null, voters: [] }}
        loading={false}
        brokerIds={new Set()}
      />
    );
    expect(screen.getByText('No controller voters are visible to this user.')).toBeTruthy();
  });

  it.each([null, 999])(
    'keeps active status unknown for an unavailable or hidden leader %s',
    leaderId => {
      render(
        <MetadataQuorum quorum={{ ...quorum, leaderId }} loading={false} brokerIds={new Set([0])} />
      );
      const rows = within(screen.getByRole('table')).getAllByRole('row').slice(1);
      rows.forEach(row => expect(within(row).getAllByRole('cell')[2].textContent).toBe('Unknown'));
      expect(screen.queryByText('Active')).toBeNull();
      expect(screen.queryByText('Not active')).toBeNull();
    }
  );

  it('updates the active controller after an election, including node ID zero', () => {
    const { rerender } = render(
      <MetadataQuorum quorum={quorum} loading={false} brokerIds={new Set([0])} />
    );
    const activeRow = () => screen.getByText('Active').closest('tr');
    expect(within(activeRow()).getAllByRole('cell')[0].textContent).toBe('100');
    rerender(
      <MetadataQuorum
        quorum={{ ...quorum, leaderId: 0 }}
        loading={false}
        brokerIds={new Set([0])}
      />
    );
    expect(within(activeRow()).getAllByRole('cell')[0].textContent).toBe('0');
  });

  it('keeps malformed or missing timestamps readable without crashing the table', () => {
    render(
      <MetadataQuorum
        quorum={{
          ...quorum,
          fetchedAt: 'invalid',
          voters: quorum.voters.map(voter => ({ ...voter, lastFetchTimestamp: -1 }))
        }}
        loading={false}
        brokerIds={new Set()}
      />
    );
    expect(screen.getByRole('table')).toBeTruthy();
    expect(screen.queryByText(/Invalid Date/)).toBeNull();
    const rows = within(screen.getByRole('table')).getAllByRole('row').slice(1);
    rows.forEach(row =>
      expect(within(row).getAllByRole('cell').at(-1).textContent).toBe('Unknown')
    );
  });
});
