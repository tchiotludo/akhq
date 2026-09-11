import { describe, expect, it } from 'vitest';
import {
  decodeBase64PathSegment,
  decodeUtf8FromBase64,
  encodeBase64PathSegment,
  encodeUtf8ToBase64
} from './base64';

describe('base64 utils', () => {
  it.each([
    { name: 'ASCII principal', principal: 'User:alice', expected: 'VXNlcjphbGljZQ==' },
    { name: 'accented Latin principal', principal: 'User:Renée', expected: 'VXNlcjpSZW7DqWU=' },
    { name: 'CJK principal', principal: 'User:张伟', expected: 'VXNlcjrlvKDkvJ8=' },
    {
      name: 'Devanagari principal with a Base64 slash',
      principal: 'User:प्रिय',
      expected: 'VXNlcjrgpKrgpY3gpLDgpL/gpK8='
    },
    {
      name: 'symbol principal',
      principal: 'User:service/account+ops',
      expected: 'VXNlcjpzZXJ2aWNlL2FjY291bnQrb3Bz'
    },
    { name: 'empty principal', principal: '', expected: '' }
  ])('encodes $name as UTF-8 Base64', ({ principal, expected }) => {
    expect(encodeUtf8ToBase64(principal)).toBe(expected);
  });

  it.each(['User:alice', 'User:张伟', 'User:Renée', 'User:प्रिय', 'User:service/account+ops', ''])(
    'decodes UTF-8 Base64 back to the original principal',
    principal => {
      expect(decodeUtf8FromBase64(encodeUtf8ToBase64(principal))).toBe(principal);
    }
  );

  it('preserves the existing Base64 representation for ASCII principals', () => {
    const principal = 'User:CN=akhq,O=example';

    expect(encodeUtf8ToBase64(principal)).toBe(btoa(principal));
  });

  it('encodes Base64 values for path segments without changing the Base64 value', () => {
    const base64Value = encodeUtf8ToBase64('User:प्रिय');

    expect(base64Value).toBe('VXNlcjrgpKrgpY3gpLDgpL/gpK8=');
    expect(encodeBase64PathSegment(base64Value)).toBe('VXNlcjrgpKrgpY3gpLDgpL%2FgpK8%3D');
    expect(decodeBase64PathSegment(encodeBase64PathSegment(base64Value))).toBe(base64Value);
  });
});
