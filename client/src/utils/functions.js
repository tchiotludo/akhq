
export const getSelectedTab = (props, tabs) => {
    const url = props.location.pathname.split('/');
    const selectedTab = props.location.pathname.split('/')[url.length - 1];
    return (tabs.includes(selectedTab))? selectedTab : tabs[0];
}

export default { getSelectedTab };
