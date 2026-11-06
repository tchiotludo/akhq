const UI_PATH = '/ui';

function extractPrefixFromUrl() {
  // Extract prefix from current URL path
  const path = window.location.pathname;
  const uiIndex = path.indexOf(UI_PATH);

  if (uiIndex >= 0) {
    return path.substring(0, uiIndex);
  }

  return '';
}

export default () => {
  // Use environment variable or detect from window location
  let prefix = extractPrefixFromUrl() || '/';

  if (prefix.endsWith('/')) {
    prefix = prefix.slice(0, -1);
  }

  return prefix;
};
