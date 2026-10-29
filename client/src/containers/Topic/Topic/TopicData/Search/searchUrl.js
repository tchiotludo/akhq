import {
  decodeSearchQuery,
  encodeSearchQuery,
  isShareable,
  legacyQueryToExpression
} from '../../../../../utils/searchQuery';

export const SEARCH_QUERY_PARAM = 'searchQuery';
const LEGACY_PARAMS = [
  'searchByKey',
  'searchByValue',
  'searchByHeaderKey',
  'searchByHeaderValue',
  'searchByKeySubject',
  'searchByValueSubject'
];

/**
 * Reads the applied search from a URLSearchParams.
 * `searchQuery` wins over the legacy `searchBy*` parameters. Never throws:
 * an invalid `searchQuery` yields `{ applied: null, error }`.
 */
export const readSearchFromQuery = query => {
  const encoded = query.get(SEARCH_QUERY_PARAM);
  if (encoded) {
    const { expression, error } = decodeSearchQuery(encoded);
    return expression
      ? { applied: expression.groups.length > 0 ? expression : null, error: null, legacy: false }
      : { applied: null, error, legacy: false };
  }
  return { applied: legacyQueryToExpression(query), error: null, legacy: true };
};

/**
 * Writes the applied search into URLSearchParams (mutated and returned in `params`).
 * Legacy parameters are always dropped. When the encoded query exceeds the cap it is omitted
 * and `notShareable` is true: the search still runs, it just cannot be restored from the URL.
 */
export const writeSearchToParams = (params, applied) => {
  params.delete(SEARCH_QUERY_PARAM);
  LEGACY_PARAMS.forEach(p => params.delete(p));

  if (!applied || !applied.groups || applied.groups.length === 0) {
    return { params, notShareable: false };
  }
  const encoded = encodeSearchQuery(applied);
  if (!isShareable(encoded)) {
    return { params, notShareable: true };
  }
  params.set(SEARCH_QUERY_PARAM, encoded);
  return { params, notShareable: false };
};

export const sameExpression = (a, b) => JSON.stringify(a || null) === JSON.stringify(b || null);
