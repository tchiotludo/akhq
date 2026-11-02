import { uriUIOptions } from './endpoints';
import { getUIOptions, setUIOptions } from './localstorage';
import { get } from './api';

export const getSelectedTab = (props, tabs) => {
  const url = props.location.pathname.split('/');
  const selectedTab = props.location.pathname.split('/')[url.length - 1];
  return tabs.includes(selectedTab) ? selectedTab : tabs[0];
};

export async function getClusterUIOptions(clusterId) {
  const uiOptions = getUIOptions(clusterId);
  if (!uiOptions && clusterId) {
    try {
      const resOptions = await get(uriUIOptions(clusterId));
      setUIOptions(clusterId, resOptions.data);
      return resOptions.data;
    } catch (err) {
      console.error('Error:', err);
      return {};
    }
  } else {
    return uiOptions;
  }
}

export const capitalizeTxt = text => {
  return text.charAt(0).toUpperCase() + text.slice(1);
};

// Checks whether navigating to the given pathname/search would keep the current URL untouched.
// Submitting unchanged filters would otherwise be a navigation no-op, leaving the page without
// any refresh: callers use this to reload data in place instead.
export const isSameLocation = (location, pathname, search) => {
  const normalizedSearch = search ? (search.startsWith('?') ? search : `?${search}`) : '';
  return location.pathname === pathname && location.search === normalizedSearch;
};

export default { getSelectedTab, getClusterUIOptions, capitalizeTxt, isSameLocation };
