import { describe, expect, it } from 'vitest';
import {
  isLargeInteger,
  safeConfigValue,
  formatLargeInteger,
  isNumericValue
} from './largeIntegerUtils.js';

describe('largeIntegerUtils', () => {
  describe('isLargeInteger()', () => {
    it('should return true for values exceeding MAX_SAFE_INTEGER', () => {
      // Kafka's Long.MAX_VALUE that caused the original issue
      expect(isLargeInteger('9223372036854775807')).toBe(true);
      expect(isLargeInteger(9223372036854775807)).toBe(true);

      // Values just over the safe limit
      expect(isLargeInteger('9007199254740992')).toBe(true);
      expect(isLargeInteger(9007199254740992)).toBe(true);

      // Large negative values
      expect(isLargeInteger('-9223372036854775808')).toBe(true);
      expect(isLargeInteger(-9223372036854775808)).toBe(true);
    });

    it('should return false for safe integer values', () => {
      expect(isLargeInteger('0')).toBe(false);
      expect(isLargeInteger(0)).toBe(false);
      expect(isLargeInteger('42')).toBe(false);
      expect(isLargeInteger(42)).toBe(false);

      // Values at the safe boundary
      expect(isLargeInteger('9007199254740991')).toBe(false); // MAX_SAFE_INTEGER
      expect(isLargeInteger(9007199254740991)).toBe(false);
      expect(isLargeInteger('-9007199254740991')).toBe(false);
      expect(isLargeInteger(-9007199254740991)).toBe(false);
    });

    it('should return false for non-integer values', () => {
      expect(isLargeInteger('3.14159')).toBe(false);
      expect(isLargeInteger('1e10')).toBe(false);
      expect(isLargeInteger('not-a-number')).toBe(false);
      expect(isLargeInteger('123abc')).toBe(false);
      expect(isLargeInteger('')).toBe(false);
      expect(isLargeInteger(null)).toBe(false);
      expect(isLargeInteger(undefined)).toBe(false);
    });

    it('should handle whitespace correctly', () => {
      expect(isLargeInteger('  9223372036854775807  ')).toBe(true);
      expect(isLargeInteger('  42  ')).toBe(false);
      expect(isLargeInteger('   ')).toBe(false);
    });
  });

  describe('safeConfigValue()', () => {
    it('should preserve large integers as strings', () => {
      const kafkaMaxValue = '9223372036854775807';
      expect(safeConfigValue(kafkaMaxValue)).toBe(kafkaMaxValue);
      expect(typeof safeConfigValue(kafkaMaxValue)).toBe('string');

      expect(safeConfigValue('9007199254740992')).toBe('9007199254740992');
      expect(typeof safeConfigValue('9007199254740992')).toBe('string');
    });

    it('should convert safe integers to numbers', () => {
      expect(safeConfigValue('42')).toBe(42);
      expect(typeof safeConfigValue('42')).toBe('number');

      expect(safeConfigValue('0')).toBe(0);
      expect(typeof safeConfigValue('0')).toBe('number');

      // At the boundary - should still be a number
      expect(safeConfigValue('9007199254740991')).toBe(9007199254740991);
      expect(typeof safeConfigValue('9007199254740991')).toBe('number');
    });

    it('should preserve non-numeric strings as-is', () => {
      expect(safeConfigValue('localhost')).toBe('localhost');
      expect(safeConfigValue('true')).toBe('true');
      expect(safeConfigValue('')).toBe('');
    });

    it('should handle edge cases', () => {
      expect(safeConfigValue(null)).toBe(null);
      expect(safeConfigValue(undefined)).toBe(undefined);

      // Already a number
      expect(safeConfigValue(42)).toBe(42);
      expect(typeof safeConfigValue(42)).toBe('number');
    });

    it('should handle whitespace in numeric strings', () => {
      expect(safeConfigValue('  42  ')).toBe(42);
      expect(safeConfigValue('  9223372036854775807  ')).toBe('9223372036854775807');
    });
  });

  describe('formatLargeInteger()', () => {
    it('should format large integers correctly', () => {
      expect(formatLargeInteger('9223372036854775807')).toBe('9223372036854775807');
      // When passed a number that's already lost precision, format what we have
      expect(formatLargeInteger(9223372036854775807)).toBe('9223372036854776000');
      expect(formatLargeInteger(42)).toBe('42');
      expect(formatLargeInteger('42')).toBe('42');
    });

    it('should handle edge cases', () => {
      expect(formatLargeInteger(null)).toBe('');
      expect(formatLargeInteger(undefined)).toBe('');
      expect(formatLargeInteger('')).toBe('');
    });
  });

  describe('isNumericValue()', () => {
    it('should return true for integer strings', () => {
      expect(isNumericValue('42')).toBe(true);
      expect(isNumericValue('0')).toBe(true);
      expect(isNumericValue('-42')).toBe(true);
      expect(isNumericValue('9223372036854775807')).toBe(true);
    });

    it('should return true for decimal strings', () => {
      expect(isNumericValue('3.14159')).toBe(true);
      expect(isNumericValue('-3.14159')).toBe(true);
      expect(isNumericValue('0.0')).toBe(true);
    });

    it('should return true for numeric values', () => {
      expect(isNumericValue(42)).toBe(true);
      expect(isNumericValue(3.14159)).toBe(true);
      expect(isNumericValue(0)).toBe(true);
      expect(isNumericValue(-42)).toBe(true);
    });

    it('should return false for non-numeric strings', () => {
      expect(isNumericValue('localhost')).toBe(false);
      expect(isNumericValue('true')).toBe(false);
      expect(isNumericValue('123abc')).toBe(false);
      expect(isNumericValue('1e10')).toBe(false); // Scientific notation not supported
      expect(isNumericValue('')).toBe(false);
      expect(isNumericValue('   ')).toBe(false);
    });

    it('should return false for edge cases', () => {
      expect(isNumericValue(null)).toBe(false);
      expect(isNumericValue(undefined)).toBe(false);
    });

    it('should handle whitespace', () => {
      expect(isNumericValue('  42  ')).toBe(true);
      expect(isNumericValue('  3.14  ')).toBe(true);
    });
  });

  describe('JavaScript precision verification', () => {
    it('should demonstrate the precision loss problem this utility solves', () => {
      const problemValue = '9223372036854775807';
      const convertedValue = +problemValue; // This is what the old code did

      // Verify the precision loss occurs - both conversions lose precision the same way
      expect(convertedValue).toBe(9223372036854776000); // The incorrect value
      expect(Number(problemValue)).toBe(9223372036854776000); // Same loss

      // Verify our utility preserves the correct value
      expect(safeConfigValue(problemValue)).toBe(problemValue);
      expect(formatLargeInteger(safeConfigValue(problemValue))).toBe(problemValue);
    });

    it('should verify MAX_SAFE_INTEGER boundary behavior', () => {
      const safeValue = Number.MAX_SAFE_INTEGER.toString();
      const unsafeValue = (Number.MAX_SAFE_INTEGER + 1).toString();

      // Safe values should be converted to numbers
      expect(typeof safeConfigValue(safeValue)).toBe('number');
      expect(safeConfigValue(safeValue)).toBe(Number.MAX_SAFE_INTEGER);

      // Unsafe values should remain as strings
      expect(typeof safeConfigValue(unsafeValue)).toBe('string');
      expect(safeConfigValue(unsafeValue)).toBe(unsafeValue);
    });
  });
});
