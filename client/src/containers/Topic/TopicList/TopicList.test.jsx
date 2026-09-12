/*eslint-disable*/
import React from 'react';
import { describe, it, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import TopicList from './TopicList';

// vi.hoisted ensures the mock reference is available inside the vi.mock factory
const mockGet = vi.hoisted(() => vi.fn());

vi.mock('../../../prefix', () => ({ default: () => '' }));

vi.mock('react-router-dom', () => ({
  useLocation: vi.fn(() => ({ search: '', pathname: '/ui/test/topic' })),
  useNavigate: vi.fn(() => vi.fn()),
  useNavigationType: vi.fn(() => 'PUSH'),
  useParams: () => ({ clusterId: 'test-cluster' }),
  Link: ({ children }) => children
}));

vi.mock('axios', async importActual => {
  const mod = await importActual();
  return {
    ...mod,
    default: {
      ...mod.default,
      isCancel: () => true,
      CancelToken: { source: () => ({ token: 'mock-token', cancel: vi.fn() }) }
    }
  };
});

// Skip consumer groups and last record to avoid extra API calls in tests
vi.mock('../../../utils/functions', () => ({
  getClusterUIOptions: vi.fn().mockResolvedValue({
    topic: { skipConsumerGroups: true, skipLastRecord: true },
    topicData: {}
  })
}));

vi.mock('../../../utils/api', () => ({
  get: mockGet,
  put: vi.fn(),
  post: vi.fn(),
  remove: vi.fn()
}));

const ALIASED_TOPIC = {
  name: 'technical.topic.name',
  alias: 'Human Readable',
  size: 0,
  logDirSize: null,
  partitions: [],
  replicaCount: 1,
  inSyncReplicaCount: 1,
  internal: false
};

const PLAIN_TOPIC = {
  name: 'plain-topic',
  alias: null,
  size: 0,
  logDirSize: null,
  partitions: [],
  replicaCount: 1,
  inSyncReplicaCount: 1,
  internal: false
};

function makeTopicsResponse(topics) {
  return { data: { results: topics, page: 1, pageSize: 25 } };
}

describe('TopicList alias rendering', () => {
  beforeEach(() => {
    sessionStorage.setItem('roles', JSON.stringify({ TOPIC: ['READ'], TOPIC_DATA: [] }));
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.clearAllMocks();
  });

  it('shows alias as primary name and real name as secondary when alias is set', async ({ expect }) => {
    mockGet.mockResolvedValue(makeTopicsResponse([ALIASED_TOPIC]));

    render(<TopicList />);

    await waitFor(() => {
      expect(screen.getByText('Human Readable')).toBeTruthy();
      expect(screen.getByText('technical.topic.name', { selector: 'small' })).toBeTruthy();
    }, { timeout: 3000 });
  });

  it('shows only the real topic name when no alias is configured', async ({ expect }) => {
    mockGet.mockResolvedValue(makeTopicsResponse([PLAIN_TOPIC]));

    render(<TopicList />);

    await waitFor(() => {
      expect(screen.getByText('plain-topic')).toBeTruthy();
    }, { timeout: 3000 });

    expect(screen.queryByRole('cell', { name: /plain-topic/i })).toBeTruthy();
    // No <small> elements should appear for topics without an alias
    const smalls = screen.queryAllByText('plain-topic', { selector: 'small' });
    expect(smalls.length).toBe(0);
  });
});
