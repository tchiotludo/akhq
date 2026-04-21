/**
 * Utility functions for handling large integer values that exceed JavaScript's
 * safe integer precision (Number.MAX_SAFE_INTEGER = 9007199254740991).
 *
 * This is particularly important for Kafka configuration values which can be
 * 64-bit integers like 9223372036854775807 (Long.MAX_VALUE).
 */

/**
 * Checks if a value is a large integer that would lose precision if converted
 * to a JavaScript number.
 *
 * @param {string|number} value - The value to check
 * @returns {boolean} True if value is a large integer that should remain a string
 */
export function isLargeInteger(value) {
  // Handle null, undefined, empty string
  if (value === null || value === undefined || value === '') {
    return false;
  }

  const stringValue = String(value).trim();

  // Check if it's a valid integer string (no decimals, exponential notation, etc.)
  if (!/^-?\d+$/.test(stringValue)) {
    return false;
  }

  // Convert to number to check magnitude
  const numValue = Number(stringValue);

  // Check if the number exceeds JavaScript's safe integer range
  return Math.abs(numValue) > Number.MAX_SAFE_INTEGER;
}

/**
 * Safely converts a configuration value, preserving large integers as strings
 * while converting safe numbers to JavaScript numbers.
 *
 * @param {string|number} value - The configuration value from backend
 * @returns {string|number} String for large integers, number for safe values
 */
export function safeConfigValue(value) {
  if (value === null || value === undefined || value === '') {
    return value;
  }

  if (isLargeInteger(value)) {
    return String(value).trim();
  }

  const numValue = +value;
  return isNaN(numValue) ? value : numValue;
}

/**
 * Formats a large integer value for display, ensuring it shows the full precision.
 *
 * @param {string|number} value - The value to format
 * @returns {string} Formatted string representation
 */
export function formatLargeInteger(value) {
  if (value === null || value === undefined || value === '') {
    return '';
  }

  return String(value);
}

/**
 * Checks if a value appears to be a numeric string that should be validated as a number.
 * This helps with form validation logic.
 *
 * @param {string|number} value - The value to check
 * @returns {boolean} True if value is numeric (including large integers)
 */
export function isNumericValue(value) {
  if (value === null || value === undefined || value === '') {
    return false;
  }

  const stringValue = String(value).trim();
  return /^-?\d+(\.\d+)?$/.test(stringValue);
}

export default {
  isLargeInteger,
  safeConfigValue,
  formatLargeInteger,
  isNumericValue
};
