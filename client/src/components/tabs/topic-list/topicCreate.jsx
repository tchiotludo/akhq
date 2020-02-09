import React from 'react';
import Form from '../../common/form/form';
import Joi from 'joi-browser';
import Header from "../../common/header";
import {withRouter} from 'react-router-dom';
import {saveTopic} from "../../../services/fakeTopicService";

class TopicCreate extends Form {
    state = {
        formData: {
            name: '',
            partition: 0,
            replication: 0,
            cleanup: '',
            retention: 0
        },
        errors: {}
    };

    schema = {
        name: Joi.string().required().label('Name'),
        partition: Joi.number().min(1).label('Partition').required(),
        replication: Joi.number().min(1).label('Replication').required(),
        cleanup: Joi.string().required(),
        retention: Joi.number().label('Retention')
    };

    componentDidMount() {
        this.setState({formData: this.props.location.state.formData})
    }

    onCleanupChange = value => {
        let {formData} = {...this.state};
        formData.cleanup = value;

        this.setState({formData});
    };

    doSubmit() {
        const {formData} = this.state;
        const {clusterId} = this.props;
        const topic = {}; // || topicService.getTopic(clusterId, topicId)

        topic.clusterId = clusterId;
        topic.name = formData.name;
        topic.partition = formData.partition;
        topic.replication = formData.replication;
        topic.cleanup = formData.cleanup;
        topic.retention = formData.retention;

        const response = saveTopic(topic);

        if (response.error) {
            this.props.history.push({
                pathname: `/${clusterId}/topic`,
                showErrorToast: true,
                errorToastTitle: response.error.title,
                errorToastMessage: response.error.message
            });
        } else {
            this.props.history.push({
                pathname: `/${clusterId}/topic`,
                showSuccessToast: true,
                successToastMessage: `Topic '${topic.name}' is created`
            });
        }
    };

    render() {
        return (
            <div id="content">
                <form encType="multipart/form-data" className="khq-form khq-form-config"
                      onSubmit={() => this.doSubmit()}>
                    <Header title="Create a topic"/>
                    {this.renderInput('name', 'Name', 'Name')}
                    {this.renderInput('partition', 'Partition', 'Partition', 'number')}
                    {this.renderInput('replication', 'Replicator Factor', 'Replicator Factor',
                        'number')}
                    {this.renderRadioGroup('cleanup', 'Cleanup Policy', [
                        'Delete',
                        'Compact',
                        'Delete and Compact'
                    ], this.onCleanupChange)}
                    {this.renderInput('retention', 'Retention', 'Retention', 'number')}

                    {this.renderButton('Create', undefined, undefined, 'submit')}
                    {/*<Modal show={this.state.showConfirmModal} handleClose={this.hideConfirmModal}>*/}
                    {/*    <p>Modal</p>*/}
                    {/*    <p>Data</p>*/}
                    {/*</Modal>*/}
                    {/*<button type="button" onClick={this.showConfirmModal}>*/}
                    {/*    Create*/}
                    {/*</button>*/}
                </form>
            </div>
        );
    }
}

export default withRouter(TopicCreate);