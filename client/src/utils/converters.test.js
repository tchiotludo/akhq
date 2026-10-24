import { describe, expect, it } from 'vitest';
import { parseISO } from 'date-fns';
import { formatDateTime, showBytes, showTime } from './converters.js';

describe('formatDateTime()', () => {
  let date = parseISO('2021-04-03T00:00:00.000Z');
  let value = {
    year: date.getFullYear(),
    monthValue: date.getMonth(),
    dayOfMonth: date.getDate(),
    hour: date.getHours(),
    minute: date.getMinutes(),
    second: date.getSeconds()
  };

  it('full format', () => {
    let formatted = formatDateTime(value, "yyyy-MM-dd'T'HH:mm:ss.SSSxxx");
    let formattedUtc = formatDateTime(value, "yyyy-MM-dd'T'HH:mm:ss.SSSxxx", true);
    expect(formatted).toBe('2021-04-03T02:00:00.000+02:00');
    expect(formattedUtc, 'utc').toBe('2021-04-03T00:00:00.000+00:00');
  });

  it('human readable format', () => {
    let formatted = formatDateTime(value, 'dd-MM-yyyy HH:mm');
    let formattedUtc = formatDateTime(value, 'dd-MM-yyyy HH:mm', true);
    expect(formatted).toBe('03-04-2021 02:00');
    expect(formattedUtc, 'utc').toBe('03-04-2021 00:00');
  });
});

describe('showTime()', () => {
  it('should show correct textual time', () => {
    expect(showTime(1000)).toBe('1 seconds ');
    expect(showTime(1000000)).toBe('16 minutes 40 seconds');
    expect(showTime(1000000000)).toBe('1 weeks 4 days');
    expect(showTime(36000000000)).toBe('1 years 1 months');
  });
});

describe('showBytes()', () => {
  it('returns empty string for null, undefined, empty and negative values', () => {
    expect(showBytes(null)).toBe('');
    expect(showBytes(undefined)).toBe('');
    expect(showBytes('')).toBe('');
    expect(showBytes(-1)).toBe('');
  });

  it('returns empty string for NaN, Infinity and non-numeric values', () => {
    expect(showBytes(NaN)).toBe('');
    expect(showBytes(Infinity)).toBe('');
    expect(showBytes('abc')).toBe('');
    expect(showBytes({})).toBe('');
  });

  it('formats zero', () => {
    expect(showBytes(0)).toBe('0 B');
  });

  it('formats values in the right unit', () => {
    expect(showBytes(512)).toBe('512 B');
    expect(showBytes(1024)).toBe('1 KB');
    expect(showBytes(1536)).toBe('1.5 KB');
    expect(showBytes(1048576)).toBe('1 MB');
    expect(showBytes('2048')).toBe('2 KB');
  });

  it('respects decimals', () => {
    expect(showBytes(1500, 0)).toBe('1 KB');
    expect(showBytes(1500, 1)).toBe('1.5 KB');
  });

  it('clamps the unit index for fractional values below 1', () => {
    expect(showBytes(0.5)).toBe('0.5 B');
  });

  it('clamps the unit index for very large values', () => {
    expect(showBytes(Number.MAX_VALUE)).not.toContain('undefined');
    expect(showBytes(Math.pow(1024, 9))).toBe('1024 YB');
  });
});
