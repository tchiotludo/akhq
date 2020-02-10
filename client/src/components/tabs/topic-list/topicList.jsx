import React from 'react';
import Header from "../../common/header";
import SearchBar from "../../common/searchBar";
import Pagination from "../../common/pagination";
import {Link} from "react-router-dom";
import Tab from "../tab";
import {getTopics, deleteTopic, saveTopic} from "../../../services/fakeTopicService";
import ConfirmModal from "../../common/modal/confirmModal";
import SuccessToast from "../../common/toast/successToast";

// Adaptation of topicList.ftl

class TopicList extends Tab {
    state = {
        topics: [],
        showDeleteModal: false,
        deleteMessage: '',
        deleteData: {},
        createTopicFormData: {
            name: '',
            partition: 1,
            replication: 1,
            cleanup: 'delete',
            retention: 86400000
        }
    };

    componentDidMount() {
        const topics = getTopics();
        this.setState({
            topics
        });
    }

    showDeleteModal = (deleteMessage, deleteData) => {
        this.setState({showDeleteModal: true, deleteMessage, deleteData});
    };

    closeDeleteModal = () => {
        this.setState({showDeleteModal: false, deleteMessage: '', deleteData: {}});
    };

    deleteTopic = () => {
        const {clusterId, topic} = this.state.deleteData;
        deleteTopic(clusterId, topic._id);
        this.closeDeleteModal();

        this.props.history.push({
            pathname: `/${clusterId}/topic`,
            showSuccessToast: true,
            successToastMessage: `Topic '${topic.name}' is deleted`
        });
    };

    renderTopics() {
        const {topics} = this.state;
        const {clusterId} = this.props.data;

        if (topics.length === 0) {
            return (
                <tr>
                    <td colSpan="9">
                        <div className="alert alert-info mb-0" role="alert">
                            No topic available
                        </div>
                    </td>
                </tr>
            );
        }

        let renderedTopics = [];
        for (let topic of topics) {
            topic.size = 0;
            topic.logDirSize = 0;

            //TODO find out where all the extra topic info comes from (i.e skipeConsumerGroups or replicaCount)
            renderedTopics.push(
                <tr key={topic._id}>
                    <td>{topic.name}</td>
                    <td>
                            <span className="text-nowrap">
                                ≈ {topic.size}
                            </span>
                    </td>
                    <td>
                        {topic.logDirSize ? "n/a" : topic.logDirSize}
                    </td>
                    <td>{topic.partition}</td>
                    <td>{topic.replication}</td>
                    <td><span
                        // className="${(topic.getReplicaCount() > topic.getInSyncReplicaCount())?then(" text-warning"
                    >{topic.replication}</span></td>
                    <td></td>
                    {/*<#if skipConsumerGroups == false>
            <td>
                <
                #list topic.getConsumerGroups() as group>
                <
                #assign active=group.isActiveTopic(topic.getName()) >
                <a href="${basePath}/${clusterId}/group/${group.getId()}" className="btn btn-sm mb-1
                    btn-${active ? then("success", "warning")} ">
                    ${group.getId()}
                <span className="badge badge-light">
                                            Lag: ${group.getOffsetLag(topic.getName())}
                                        </span>
            </a>
            <br/>
        </#list>
        </td>
        {/#if*/}
                    <td className="khq-row-action khq-row-action-main">
                        {/*    <a href="${basePath}/${clusterId}/topic/${topic.getName()}${roles?seq_contains("*/}
                        {/*topic/data/read")?then("", "/partitions")}" */}
                        <Link to={`/${clusterId}/topic/${topic.name}`}
                        ><i className="fa fa-search"/></Link>
                    </td>
                    {/*<#if canDelete == true>*/}
                    <td className="khq-row-action">
                        {/*<#if topic.isInternal() == false>*/}
                        <Link
                            to="#"
                            onClick={() => this.showDeleteModal(`Do you want to delete topic: ${topic.name}`,
                                {clusterId: clusterId, topic: topic})}
                            // href="${basePath}/${clusterId}/topic/${topic.getName()}/delete"
                            //data-confirm="Do you want to delete topic: <code>${topic.getName()}</code> ?"
                        >
                            <i className="fa fa-trash"/>
                        </Link>
                        {/*</#if>*/}
                    </td>
                    {/*</#if>*/}
                </tr>
            );
        }

        return renderedTopics;
    }

    render() {
        const {topics} = this.state;
        const {clusterId} = this.props.data;

        return (
            <div id="content">
                <Header title="Topics"/>
                <SearchBar pagination={true} topicListView={true}/>
                <div className="table-responsive">
                    <table className="table table-bordered table-striped table-hover mb-0">
                        <thead className="thead-dark">
                        <tr>
                            <th colSpan="3">Topics</th>
                            <th colSpan="1">Partitions</th>
                            <th colSpan="2">Replications</th>
                            {/*<#if skipConsumerGroups== false>*/}
                            <th>Consumers Groups</th>
                            {/*</#if>*/}
                            <th colSpan="2" className="khq-row-action"/>
                        </tr>
                        </thead>
                        <thead className="thead-dark">
                        <tr>
                            <th className="text-nowrap">Name</th>
                            <th className="text-nowrap">Size</th>
                            <th className="text-nowrap">Weight</th>
                            <th className="text-nowrap">Total</th>
                            <th className="text-nowrap">Factor</th>
                            <th className="text-nowrap">In Sync</th>
                            {/*<#if skipConsumerGroups== false>*/}
                            <th className="text-nowrap">Consumer Groups</th>
                            {/*</#if>*/}
                            <th className="khq-row-action"></th>
                            {/*<#if canDelete== true>*/}
                            <th className="khq-row-action"></th>
                            {/*</#if>*/}
                        </tr>
                        </thead>
                        <tbody>
                        {/*#if topics?size == 0*/}
                        {this.renderTopics()}
                        {/*</#if>
    <#list topics as topic>
    <tr>
        <td>${topic.getName()}</td>
        <td>
                            <span className="text-nowrap">
                                ≈ ${topic.getSize()}
                            </span>
        </td>
        <td>
            #if topic.getLogDirSize().isEmpty()
            n/a
            <#else>
            ${functions.filesize(topic.getLogDirSize().get())}
        </#if>
    </td>
    <td>${topic.getPartitions() ? size}</td>
    <td>${topic.getReplicaCount()}</td>
    <td><span className="${(topic.getReplicaCount() > topic.getInSyncReplicaCount())?then("
              text-warning", "")}">${topic.getInSyncReplicaCount()}</span></td>
    <#if skipConsumerGroups == false>
    <td>
        <
        #list topic.getConsumerGroups() as group>
        <
        #assign active=group.isActiveTopic(topic.getName()) >
        <a href="${basePath}/${clusterId}/group/${group.getId()}" className="btn btn-sm mb-1
            btn-${active ? then("success", "warning")} ">
            ${group.getId()}
        <span className="badge badge-light">
                                            Lag: ${group.getOffsetLag(topic.getName())}
                                        </span>
    </a>
    <br/>
</#list>
</td>
</#if>
    <td className="khq-row-action khq-row-action-main">
        <a href="${basePath}/${clusterId}/topic/${topic.getName()}${roles?seq_contains("
           topic/data/read")?then("", "/partitions")}" ><i className="fa fa-search"></i></a>
    </td>
    <#if canDelete == true>
    <td className="khq-row-action">
        <
        #if topic.isInternal() == false>
        <a
            href="${basePath}/${clusterId}/topic/${topic.getName()}/delete"
            data-confirm="Do you want to delete topic: <code>${topic.getName()}</code> ?"
        ><i className="fa fa-trash"></i></a>
    </#if>
</td>
</#if>
</tr>
</#list>*/}
                        </tbody>
                    </table>
                </div>

                <Pagination/>

                {/*#if roles?seq_contains("topic/insert") == true*/}
                <aside>
                    <Link to={{
                        pathname: `/${clusterId}/topic/create`,
                        state: {formData: this.state.createTopicFormData}
                    }} className="btn btn-primary">Create a topic</Link>
                </aside>
                {/*</#if>*/}

                <ConfirmModal
                    show={this.state.showDeleteModal}
                    handleCancel={this.closeDeleteModal}
                    handleConfirm={this.deleteTopic}
                    message={this.state.deleteMessage}
                />
            </div>
        );
    }
}

export default TopicList;