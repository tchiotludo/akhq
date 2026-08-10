import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getClusterUIOptions } from './functions.jsx';
import { get } from './api';
import { setUIOptions } from './localstorage.jsx';

vi.mock('./api', () => ({
  get: vi.fn()
}));

describe('getClusterUIOptions()', () => {
  const clusterId = 'local';
  const serverOptions = {
    topic: {
      defaultView: 'ALL',
      skipLastRecord: true
    }
  };

  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('returns cached options without calling server when clusterId is missing', async () => {
    await expect(getClusterUIOptions(undefined)).resolves.toBeNull();
    expect(get).not.toHaveBeenCalled();
  });

  it('stores server options when there are no cached options', async () => {
    get.mockResolvedValue({ data: serverOptions });

    await expect(getClusterUIOptions(clusterId)).resolves.toEqual(serverOptions);
    expect(JSON.parse(localStorage.getItem('uiOptions'))).toEqual({ [clusterId]: serverOptions });
  });

  it('keeps local user options by default', async () => {
    const localOptions = {
      topic: {
        defaultView: 'HIDE_INTERNAL',
        skipLastRecord: false
      }
    };
    setUIOptions(clusterId, localOptions);
    get.mockResolvedValue({ data: serverOptions });

    await expect(getClusterUIOptions(clusterId)).resolves.toEqual(localOptions);
    expect(JSON.parse(localStorage.getItem('uiOptions'))).toEqual({ [clusterId]: localOptions });
  });

  it('refreshes local options when server refresh is enabled', async () => {
    const localOptions = {
      topic: {
        defaultView: 'HIDE_INTERNAL',
        skipLastRecord: false
      }
    };
    const refreshingServerOptions = {
      ...serverOptions,
      refreshFromServer: true
    };
    setUIOptions(clusterId, localOptions);
    get.mockResolvedValue({ data: refreshingServerOptions });

    await expect(getClusterUIOptions(clusterId)).resolves.toEqual(refreshingServerOptions);
    expect(JSON.parse(localStorage.getItem('uiOptions'))).toEqual({
      [clusterId]: refreshingServerOptions
    });
  });

  it('falls back to cached local options when fetching server options fails', async () => {
    setUIOptions(clusterId, serverOptions);
    get.mockRejectedValue(new Error('network error'));

    await expect(getClusterUIOptions(clusterId)).resolves.toEqual(serverOptions);
  });
});
