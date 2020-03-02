import _ from 'lodash';

export function handleConvert(value, unit, exclude) {
  exclude = exclude || '';
  const convert = require('convert-units');
  return convert(value)
    .from(unit)
    .toBest(exclude);
}

export function handleType(value) {
  return _.lowerCase(value.plural);
}

export function showTime(milliseconds) {
  if (!milliseconds) return '0 seconds';
  //converts value to bigger type possible: year, month, week, day, hour, minute, second
  const value = handleConvert(milliseconds, 'ms', { exclude: ['ms', 'mu', 'ns'] });
  const valueIsSecond = Boolean(value.unit === 's'); //check if is second
  // create value to show
  const valueToSHow = valueIsSecond
    ? `${Number(value.val.toFixed(3))} ${handleType(value)}`
    : `${Math.floor(value.val)} ${handleType(value)}`;

  // if value is not second - convert its decimal part into bigger tipe possible
  const decimalPart = valueIsSecond
    ? ''
    : handleConvert(value.val - Math.floor(value.val), value.unit, {
        exclude: ['ms', 'mu', 'ns']
      });
  // create decimalPart to show
  const decimalPartToShow =
    decimalPart && decimalPart.val > 0
      ? `${Math.floor(decimalPart.val)} ${handleType(decimalPart)}`
      : '';

  return `${valueToSHow} ${decimalPartToShow}`;
}

export function showBytes(bytes,dPlaces=2) {
  if (!bytes) return '0B';
  const value = handleConvert(bytes, 'B');
  return `${value.val.toFixed(dPlaces)}${value.unit}`;
}

export default { showTime, showBytes };
