import {uriUIOptions} from "./endpoints";
import {getUIOptions, setUIOptions} from "./localstorage";
import {get} from "./api";

export const getSelectedTab = (props, tabs) => {
    const url = props.location.pathname.split('/');
    const selectedTab = props.location.pathname.split('/')[url.length - 1];
    return (tabs.includes(selectedTab))? selectedTab : tabs[0];
}

export async function setClusterUIOptions(clusterId) {

    const uiOptions = getUIOptions(clusterId);
    if (!uiOptions && clusterId) {
        try {
            const resOptions = await get(uriUIOptions(clusterId));
            setUIOptions(clusterId, resOptions.data);
        } catch(err) {
            console.error('Error:', err);
        }
    }
}

export default { getSelectedTab, setClusterUIOptions };

