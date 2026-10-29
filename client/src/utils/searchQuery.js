import { deflateSync, inflateSync, strFromU8, strToU8 } from 'fflate';

export const SEARCH_QUERY_VERSION = 1;
export const SEARCH_QUERY_PREFIX = 'v1.';
export const MAX_SHAREABLE_LENGTH = 2000;

const LEGACY_FIELDS = ['key', 'value', 'headerKey', 'headerValue', 'keySubject', 'valueSubject'];
const LEGACY_OPERATORS = { C: 'contains', N: 'notContains', E: 'equals' };

const toBase64Url = bytes => {
  let binary = '';
  bytes.forEach(b => {
    binary += String.fromCharCode(b);
  });
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
};

const fromBase64Url = text => {
  if (!/^[A-Za-z0-9_-]*$/.test(text)) {
    throw new Error('Invalid base64url payload');
  }
  const padded = text.replace(/-/g, '+').replace(/_/g, '/') + '==='.slice((text.length + 3) % 4);
  return Uint8Array.from(atob(padded), c => c.charCodeAt(0));
};

export const encodeSearchQuery = expression =>
  SEARCH_QUERY_PREFIX + toBase64Url(deflateSync(strToU8(JSON.stringify(expression))));

export const isShareable = encoded => !!encoded && encoded.length <= MAX_SHAREABLE_LENGTH;

// Returns { expression } or { error }; never throws.
export const decodeSearchQuery = encoded => {
  if (typeof encoded !== 'string' || encoded.length === 0) {
    return { error: 'Empty search query' };
  }
  const dot = encoded.indexOf('.');
  const version = dot > 0 ? encoded.slice(0, dot) : '';
  if (version !== 'v1') {
    return { error: `Unsupported search query version: ${version || 'unknown'}` };
  }
  try {
    const expression = JSON.parse(strFromU8(inflateSync(fromBase64Url(encoded.slice(dot + 1)))));
    if (
      !expression ||
      expression.version !== SEARCH_QUERY_VERSION ||
      !Array.isArray(expression.groups)
    ) {
      return { error: 'Invalid search query structure' };
    }
    return { expression };
  } catch (e) {
    return { error: 'Corrupted search query' };
  }
};

// Legacy `searchBy<Field>=<text>_<C|N|E>` values -> expression (AND across fields, contains terms OR-ed).
export const legacyToExpression = search => {
  const groups = [];
  LEGACY_FIELDS.forEach(field => {
    const item = search && search[field];
    if (!item || !item.text) {
      return;
    }
    const operator = LEGACY_OPERATORS[item.type] || 'contains';
    const values =
      operator === 'contains'
        ? item.text.split(/\s+/).filter(term => term.length > 0)
        : [item.text];
    if (values.length === 0) {
      return;
    }
    groups.push({
      ...(groups.length > 0 ? { join: 'AND' } : {}),
      conditions: values.map((value, index) => ({
        ...(index > 0 ? { join: 'OR' } : {}),
        field,
        operator,
        value
      }))
    });
  });
  return groups.length > 0 ? { version: SEARCH_QUERY_VERSION, groups } : null;
};

export const legacyQueryToExpression = query => {
  const search = {};
  LEGACY_FIELDS.forEach(field => {
    const raw = query.get(`searchBy${field.charAt(0).toUpperCase()}${field.slice(1)}`);
    if (raw) {
      const pos = raw.lastIndexOf('_');
      search[field] = { text: raw.substring(0, pos), type: raw.substring(pos + 1) };
    }
  });
  return legacyToExpression(search);
};
