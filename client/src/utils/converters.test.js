import { describe, expect, it } from 'vitest';
import { parseISO } from 'date-fns';
import { formatDateTime, showTime } from './converters.js';

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
