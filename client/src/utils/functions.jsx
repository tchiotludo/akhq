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
  if (!clusterId) {
    return uiOptions;
  }

  try {
    const resOptions = await get(uriUIOptions(clusterId));
    const serverOptions = resOptions.data;

    if (!uiOptions || serverOptions.refreshFromServer) {
      setUIOptions(clusterId, serverOptions);
      return serverOptions;
    }

    return uiOptions;
  } catch (err) {
    console.error('Error:', err);
    return uiOptions ?? {};
  }
}

export const capitalizeTxt = text => {
  return text.charAt(0).toUpperCase() + text.slice(1);
};

export default { getSelectedTab, getClusterUIOptions, capitalizeTxt };
