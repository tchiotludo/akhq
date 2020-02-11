import React, {Component} from 'react';
import Sidebar from "./common/sidebar";
import Cluster from "./tabs/cluster";
import Tail from "./tabs/tail";
import Node from "./tabs/node";
import Group from "./tabs/group";
import Acls from "./tabs/acls";
import Connect from "./tabs/connect";
import Schema from "./tabs/schema";
import TopicList from "./tabs/topic-list/topicList";
import TopicCreate from "./tabs/topic-list/topicCreate";
import SuccessToast from "./common/toast/successToast";
import ErrorToast from "./common/toast/errorToast";
import Topic from "./tabs/topic/topic";

class Dashboard extends Component {
    state = {
        clusterId: '',
        topicId: '',
        selectedTab: 'cluster', //cluster | node | topic | tail | group | acls | schema | connect
        action: '',
        showSuccessToast: false,
        successToastMessage: '',
        successToastTimeout: 10000, // in ms
        showErrorToast: false,
        errorToastTitle: '',
        errorToastMessage: '',
        errorToastTimeout: 6000 // in ms
    };

    static getDerivedStateFromProps(nextProps, prevState) {
        const clusterId = nextProps.match.params.clusterId;
        const topicId = nextProps.match.params.topicId;
        const selectedTab = nextProps.match.params.tab;
        const action = nextProps.match.params.action;
        const {
            showSuccessToast,
            successToastMessage,
            showErrorToast,
            errorToastTitle,
            errorToastMessage
        } = nextProps.location;

        return {
            topicId: topicId,
            clusterId: clusterId,
            selectedTab: selectedTab,
            action: action,
            showSuccessToast: showSuccessToast,
            successToastMessage: successToastMessage,
            showErrorToast: showErrorToast,
            errorToastTitle: errorToastTitle,
            errorToastMessage: errorToastMessage
        };
    }

    componentDidMount() {
        this.checkToasts();
    }

    componentWillUnmount() {
        clearTimeout(this.interval);
    }

    checkToasts = () => {
        const {clusterId} = this.state;

        if (this.state.showSuccessToast) {
            this.interval = setTimeout(() => {
                this.props.history.push({
                    pathname: `/${clusterId}/topic`,
                    showSuccessToast: false,
                    successToastMessage: ''
                });
            }, this.state.successToastTimeout);
        }

        if (this.state.showErrorToast) {
            this.interval = setTimeout(() => {
                this.props.history.push({
                    pathname: `/${clusterId}/topic`,
                    showErrorToast: false,
                    errorToastTitle: '',
                    errorToastMessage: ''
                });
            }, this.state.errorToastTimeout);
        }
    };

    renderSelectedTab = (data) => {
        const {selectedTab} = this.state;

        this.checkToasts();

        switch (selectedTab) {
            case 'cluster':
                return <Cluster data={data} history={this.props.history}/>;
            case 'node':
                return <Node data={data} history={this.props.history}/>;
            case 'topic':
                return <TopicList data={data} history={this.props.history}/>;
            case 'tail':
                return <Tail data={data} history={this.props.history}/>;
            case 'group':
                return <Group data={data} history={this.props.history}/>;
            case 'acls':
                return <Acls data={data} history={this.props.history}/>;
            case 'schema':
                return <Schema data={data} history={this.props.history}/>;
            case 'connect':
                return <Connect data={data} history={this.props.history}/>;
            default:
                return <Cluster data={data} history={this.props.history}/>;
        }
    };

    renderActionTab = () => {
        const {clusterId, selectedTab, action} = this.state;

        // eslint-disable-next-line default-case
        switch (selectedTab) {
            case 'topic':
                switch (action) {
                    case 'create':
                        return <TopicCreate clusterId={clusterId}/>;
                    default:
                        return <Topic clusterId={clusterId} topicId={action}/>;
                }
            case 'node':
                break;
            case 'tail':
                break;
            case 'group':
                break;
            case 'acls':
                break;
            case 'schema':
                break;
            case 'connect':
                break;
        }
    };

    render() {
        const {
            selectedTab,
            topicId,
            clusterId,
            action,
            showSuccessToast,
            successToastMessage,
            showErrorToast,
            errorToastTitle,
            errorToastMessage
        } = this.state;

        return (
            <React.Fragment>
                <SuccessToast show={showSuccessToast} message={successToastMessage}/>
                <ErrorToast show={showErrorToast} title={errorToastTitle} message={errorToastMessage}/>
                <Sidebar
                    selectedTab={selectedTab}
                    clusterId={clusterId}
                />
                {action !== undefined ? this.renderActionTab() : this.renderSelectedTab({clusterId, topicId})}
            </React.Fragment>
        );
    }
}

export default Dashboard;