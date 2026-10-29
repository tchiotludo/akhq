import { describe, expect, it, vi } from 'vitest';
import { SearchRequestError, buildAuthHeaders, createSseParser, streamSearch } from './sseClient';

const parse = chunks => {
  const frames = [];
  const parser = createSseParser(f => frames.push(f));
  chunks.forEach(c => parser.feed(c));
  parser.end();
  return frames;
};

const streamOf = (chunks, { error } = {}) => {
  const encoder = new TextEncoder();
  let i = 0;
  return {
    getReader: () => ({
      read: async () => {
        if (i < chunks.length) {
          return { done: false, value: encoder.encode(chunks[i++]) };
        }
        if (error) {
          throw error;
        }
        return { done: true };
      }
    })
  };
};

describe('createSseParser', () => {
  it('parses named frames and multi-line data', () => {
    expect(
      parse(['event: searchBody\ndata: {"a":1}\n\nevent: searchEnd\ndata: x\ndata: y\n\n'])
    ).toEqual([
      { event: 'searchBody', data: '{"a":1}' },
      { event: 'searchEnd', data: 'x\ny' }
    ]);
  });

  it('handles frames split across chunks, even inside a multi-byte sequence boundary', () => {
    expect(parse(['event: sear', 'chBody\nda', 'ta: {"x":"é"}\n', '\n'])).toEqual([
      { event: 'searchBody', data: '{"x":"é"}' }
    ]);
  });

  it('handles CRLF, including a CRLF split between chunks, and ignores comments', () => {
    expect(parse([': keep-alive\r\nevent: a\r', '\ndata: 1\r\n\r', '\n'])).toEqual([
      { event: 'a', data: '1' }
    ]);
  });

  it('defaults the event name and flushes a last frame without blank line', () => {
    expect(parse(['data: tail'])).toEqual([{ event: 'message', data: 'tail' }]);
  });
});

describe('buildAuthHeaders', () => {
  it('adds the bearer token only when present', () => {
    expect(buildAuthHeaders({ getItem: () => 'tok' })).toEqual({ Authorization: 'Bearer tok' });
    expect(buildAuthHeaders({ getItem: () => null })).toEqual({});
  });
});

describe('streamSearch', () => {
  it('POSTs the JSON body with credentials and streams events', async () => {
    const fetchImpl = vi.fn().mockResolvedValue({
      ok: true,
      body: streamOf([
        'event: searchBody\ndata: {"percent":1}\n\n',
        'event: searchEnd\ndata: {}\n\n'
      ])
    });
    const events = [];
    await streamSearch({
      url: '/api/c/topic/t/data/search?size=10',
      body: { version: 1, groups: [] },
      headers: { Authorization: 'Bearer t' },
      onEvent: e => events.push(e),
      fetchImpl
    });

    const [url, init] = fetchImpl.mock.calls[0];
    expect(url).toBe('/api/c/topic/t/data/search?size=10');
    expect(init.method).toBe('POST');
    expect(init.credentials).toBe('include');
    expect(init.headers).toMatchObject({
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: 'Bearer t'
    });
    expect(JSON.parse(init.body)).toEqual({ version: 1, groups: [] });
    expect(events.map(e => e.event)).toEqual(['searchBody', 'searchEnd']);
  });

  it('maps a 4xx JSON error to SearchRequestError', async () => {
    const fetchImpl = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      statusText: 'Bad Request',
      json: async () => ({ message: 'Invalid regular expression' })
    });
    await expect(
      streamSearch({ url: '/x', body: {}, headers: {}, onEvent: () => {}, fetchImpl })
    ).rejects.toMatchObject({
      name: 'SearchRequestError',
      status: 400,
      message: 'Invalid regular expression'
    });
  });

  it('falls back to the status text for non-JSON errors', async () => {
    const fetchImpl = vi.fn().mockResolvedValue({
      ok: false,
      status: 502,
      statusText: 'Bad Gateway',
      json: async () => {
        throw new Error('no json');
      }
    });
    const error = await streamSearch({
      url: '/x',
      body: {},
      headers: {},
      onEvent: () => {},
      fetchImpl
    }).catch(e => e);
    expect(error).toBeInstanceOf(SearchRequestError);
    expect(error.message).toBe('Bad Gateway');
  });

  it('resolves silently when aborted before or during the stream', async () => {
    const abort = Object.assign(new Error('aborted'), { name: 'AbortError' });
    await expect(
      streamSearch({
        url: '/x',
        body: {},
        headers: {},
        onEvent: () => {},
        fetchImpl: vi.fn().mockRejectedValue(abort)
      })
    ).resolves.toBeUndefined();

    const onEvent = vi.fn();
    await expect(
      streamSearch({
        url: '/x',
        body: {},
        headers: {},
        onEvent,
        fetchImpl: vi.fn().mockResolvedValue({
          ok: true,
          body: streamOf(['event: a\ndata: 1\n\n'], { error: abort })
        })
      })
    ).resolves.toBeUndefined();
    expect(onEvent).toHaveBeenCalledTimes(1);
  });

  it('propagates network errors', async () => {
    await expect(
      streamSearch({
        url: '/x',
        body: {},
        headers: {},
        onEvent: () => {},
        fetchImpl: vi.fn().mockRejectedValue(new TypeError('Failed to fetch'))
      })
    ).rejects.toThrow('Failed to fetch');
  });
});
