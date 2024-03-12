import lowerCase from 'lodash/lowerCase';
import moment from 'moment';

export function calculateTopicOffsetLag(topicOffsets, topicId) {
  let offsetLag = 0;
  let offset = 0;
  let lastOffset = 0;

  topicOffsets
    .filter(topicOffset => topicOffset.topic === topicId)
    .forEach(topicOffset => {
      offset = topicOffset.offset || 0;
      lastOffset = topicOffset.lastOffset || 0;
      offsetLag += lastOffset - offset;
    });

  return offsetLag;
}

export function groupedTopicOffset(offsets) {
  return (offsets || []).reduce((accumulator, r) => {
    if (accumulator[r.topic] === undefined) {
      accumulator[r.topic] = [];
    }

    accumulator[r.topic].push(r);

    return accumulator;
  }, Object.create(null));
}

/**
 * If the utc parameter is true (which is the default value),
 * the date and time will be converted to UTC time before formatting.
 * If utc is false, the date and time will be formatted in the local time zone.
 * Finally, the formatted date and time string is returned as a string
 */
export function formatDateTime(value, format, utc = true) {
  let milli = value.milli || 0;
  const date = new Date(
    value.year,
    value.monthValue,
    value.dayOfMonth,
    value.hour,
    value.minute,
    value.second,
    milli
  );

  return utc
    ? moment(date.toISOString()).utc().format(format).toString()
    : moment(date.toISOString()).format(format).toString();
}

export function handleConvert(value, unit, exclude) {
  exclude = exclude || '';
  const convert = require('convert-units');
  return convert(value).from(unit).toBest(exclude);
}

export function handleType(value) {
  return lowerCase(value.plural);
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

/**
 * This function is responsible for showing the bytes in a
 * friendly way to the user, making the leases for upper
 * measures such as MB, GB, TB etc.
 *
 * @param {*} bytes value in bytes to show
 * @param {*} decimals decimal size place
 * @returns
 */
export function showBytes(bytes, decimals = 3) {
  if (bytes === null || bytes === undefined) return '';
  if (bytes === 0) return '0 B';

  const kbytes = 1024;
  const decimalCheck = decimals < 0 ? 0 : decimals;
  const measures = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

  const identification = Math.floor(Math.log(bytes) / Math.log(kbytes));

  return (
    parseFloat((bytes / Math.pow(kbytes, identification)).toFixed(decimalCheck)) +
    ' ' +
    measures[identification]
  );
}

export function organizeRoles(roles) {
  let newRoles = {};

  if (!roles) {
    return JSON.stringify(newRoles);
  }

  roles.forEach(role => {
    role.resources.forEach(resource => {
      role.actions.forEach(action => {
        if (newRoles[resource] === undefined) {
          newRoles[resource] = [];
        }
        newRoles[resource].push(action);
      });
    });
  });

  return JSON.stringify(newRoles);
}

export function transformListObjsToViewOptions(list, id, name) {
  return list.map(elem => {
    return {
      _id: elem[id],
      name: elem[name]
    };
  });
}

export function transformStringArrayToViewOptions(list) {
  return list.map(elem => {
    return {
      _id: elem,
      name: elem
    };
  });
}

export default { showTime, showBytes };
