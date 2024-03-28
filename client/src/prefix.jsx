export default () => {
  // eslint-disable-next-line no-undef
  let prefix = AKHQ_PREFIX_PATH;

  // eslint-disable-next-line no-undef
  if (prefix === undefined) {
    prefix = '/';
  }

  if (prefix.endsWith('/')) {
    prefix = prefix.substr(-1);
  }

  return prefix;
};
