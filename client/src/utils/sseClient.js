export class SearchRequestError extends Error {
  constructor(status, message) {
    super(message);
    this.name = 'SearchRequestError';
    this.status = status;
  }
}

// Incremental parser for a text/event-stream; frames are dispatched on blank lines.
export const createSseParser = onFrame => {
  let buffer = '';
  let eventName = '';
  let dataLines = [];

  const dispatch = () => {
    if (dataLines.length > 0 || eventName) {
      onFrame({ event: eventName || 'message', data: dataLines.join('\n') });
    }
    eventName = '';
    dataLines = [];
  };

  const processLine = line => {
    if (line === '') {
      dispatch();
    } else if (line.startsWith(':')) {
      // comment / keep-alive
    } else {
      const colon = line.indexOf(':');
      const field = colon === -1 ? line : line.slice(0, colon);
      let value = colon === -1 ? '' : line.slice(colon + 1);
      if (value.startsWith(' ')) {
        value = value.slice(1);
      }
      if (field === 'event') {
        eventName = value;
      } else if (field === 'data') {
        dataLines.push(value);
      }
    }
  };

  return {
    feed(text) {
      buffer += text;
      // A trailing "\r" may be the first half of a "\r\n" split across chunks.
      const held = buffer.endsWith('\r') ? '\r' : '';
      const complete = (held ? buffer.slice(0, -1) : buffer).replace(/\r\n?/g, '\n');
      const lines = complete.split('\n');
      buffer = lines.pop() + held;
      lines.forEach(processLine);
    },
    end() {
      if (buffer.length > 0) {
        processLine(buffer.replace(/\r$/, ''));
        buffer = '';
      }
      dispatch();
    }
  };
};

export const buildAuthHeaders = (storage = localStorage) => {
  const token = storage.getItem('jwtToken');
  return token ? { Authorization: 'Bearer ' + token } : {};
};

const readErrorMessage = async response => {
  try {
    const payload = await response.json();
    if (payload && payload.message) {
      return payload.message;
    }
  } catch (e) {
    // not a JSON body
  }
  return response.statusText || `Request failed with status ${response.status}`;
};

/**
 * POSTs `body` as JSON and streams the resulting server-sent events to `onEvent({event, data})`.
 * Resolves when the stream ends or is aborted through `signal`; rejects with a SearchRequestError
 * on a non-2xx response. Credentials follow the axios setup (cookies included, JWT as Bearer).
 */
export const streamSearch = async ({
  url,
  body,
  signal,
  onEvent,
  headers = buildAuthHeaders(),
  fetchImpl = (...args) => fetch(...args)
}) => {
  let response;
  try {
    response = await fetchImpl(url, {
      method: 'POST',
      credentials: 'include',
      signal,
      headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream', ...headers },
      body: JSON.stringify(body)
    });
  } catch (e) {
    if (e.name === 'AbortError') {
      return;
    }
    throw e;
  }

  if (!response.ok) {
    throw new SearchRequestError(response.status, await readErrorMessage(response));
  }

  const parser = createSseParser(onEvent);
  const decoder = new TextDecoder();
  const reader = response.body.getReader();
  try {
    for (;;) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }
      parser.feed(decoder.decode(value, { stream: true }));
    }
    parser.feed(decoder.decode());
    parser.end();
  } catch (e) {
    if (e.name !== 'AbortError') {
      throw e;
    }
  }
};
