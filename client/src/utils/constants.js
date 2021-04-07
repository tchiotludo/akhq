// Application version to display in sidebar
export const VERSION = 'Beta';

// Config data types
export const MILLI = 'MILLI';
export const BYTES = 'BYTES';
export const TEXT = 'TEXT';

// Role types
export const ROLE_TYPE = {
  TOPIC: 'topic',
  NODE: 'node',
  GROUP: 'group',
  REGISTRY: 'registry',
  ACLS: 'acls',
  CONNECT: 'connect'
};

// Table actions
export const TABLE_ADD = 'add';
export const TABLE_EDIT = 'edit';
export const TABLE_DELETE = 'delete';
export const TABLE_DETAILS = 'details';
export const TABLE_CONFIG = 'config';
export const TABLE_RESTART = 'restart';
export const TABLE_SHARE = 'share';

// Tab names/route names
export const CLUSTER = 'cluster';
export const NODE = 'node';
export const TOPIC = 'topic';
export const TAIL = 'tail';
export const GROUP = 'group';
export const ACLS = 'acls';
export const SCHEMA = 'schema';
export const CONNECT = 'connect';
export const SETTINGS = 'settings';
export const TOPICS = {
  ALL: 'ALL',
  HIDE_INTERNAL: 'HIDE_INTERNAL',
  HIDE_INTERNAL_STREAM: 'HIDE_INTERNAL_STREAM',
  HIDE_STREAM: 'HIDE_STREAM'
};

export const TYPES = {
  STRING: 'STRING',
  LONG: 'LONG',
  CLASS: 'CLASS',
  PASSWORD: 'PASSWORD',
  INT: 'INT',
  LIST: 'LIST',
  BOOLEAN: 'BOOLEAN',
  DOUBLE: 'DOUBLE',
  SHORT: 'SHORT'
};

export default {
  MILLI,
  BYTES,
  TEXT,
  TABLE_ADD,
  TABLE_EDIT,
  TABLE_DELETE,
  TABLE_DETAILS,
  TABLE_CONFIG,
  TABLE_RESTART,
  TABLE_SHARE,
  CLUSTER,
  NODE,
  TOPIC,
  TAIL,
  TOPICS,
  GROUP,
  ACLS,
  SCHEMA,
  CONNECT,
  TYPES,
  ROLE_TYPE,
  VERSION,
  SETTINGS
};

export const sortBy = (field, reverse, primer) => {
  const key = primer
    ? function(x) {
        return primer(x[field]);
      }
    : function(x) {
        return x[field];
      };

  reverse = !reverse ? 1 : -1;

  return function(a, b) {
    // eslint-disable-next-line
    return (a = key(a)), (b = key(b)), reverse * ((a > b) - (b > a));
  };
};
