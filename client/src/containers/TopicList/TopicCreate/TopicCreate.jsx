import React from 'react';
import Joi from 'joi-browser';
import { withRouter } from 'react-router-dom';
import Form from '../../../components/Form/Form';
import Header from '../../Header';
import { post } from '../../../utils/api';
import { uriTopicsCreate } from '../../../utils/endpoints';

class TopicCreate extends Form {
  state = {
    formData: {
      name: '',
      partition: 1,
      replication: 1,
      cleanup: 'delete',
      retention: 86400000
    },
    errors: {}
  };

  schema = {
    name: Joi.string()
      .required()
      .label('Name'),
    partition: Joi.number()
      .min(1)
      .label('Partition')
      .required(),
    replication: Joi.number()
      .min(1)
      .label('Replication')
      .required(),
    cleanup: Joi.string().required(),
    retention: Joi.number().label('Retention')
  };

  onCleanupChange = value => {
    let { formData } = { ...this.state };
    formData.cleanup = value;

    this.setState({ formData });
  };

  async doSubmit() {
    const { formData } = this.state;
    const { clusterId } = this.props.match.params;
    const { history } = this.props;
    const topic = {
      cluster: clusterId,
      name: formData.name,
      partition: formData.partition,
      replication: formData.replication,
      configs: {
        'cleanup.policy': formData.cleanup === 'deleteAndCompact' ? '' : formData.cleanup,
        'retention.ms': formData.retention
      }
    };

    history.replace({
      loading: true
    });

    post(uriTopicsCreate(clusterId), topic)
      .then(res => {
        this.props.history.push({
          pathname: `/${clusterId}/topic`,
          showSuccessToast: true,
          successToastMessage: `Topic '${formData.name}' is created`,
          loading: false
        });
      })
      .catch(err => {
        console.log('err', err);
        this.props.history.replace({
          showErrorToast: true,
          errorToastTitle: `Failed to create topic '${formData.name}'`,
          errorToastMessage: err.response.data.message,
          loading: false
        });
      });
  }
  render() {
    return (
      <div>
        <form
          encType="multipart/form-data"
          className="khq-form khq-form-config"
          onSubmit={() => this.doSubmit()}
        >
          <Header title="Create a topic" />
          {this.renderInput('name', 'Name', 'Name')}
          {this.renderInput('partition', 'Partition', 'Partition', 'number')}
          {this.renderInput('replication', 'Replicator Factor', 'Replicator Factor', 'number')}
          {this.renderRadioGroup(
            'cleanup',
            'Cleanup Policy',
            ['Delete', 'Compact', 'Delete and Compact'],
            this.onCleanupChange
          )}
          {this.renderInput('retention', 'Retention', 'Retention', 'number')}

          {this.renderButton(
            'Create',
            () => {
              this.doSubmit();
            },
            undefined,
            'button'
          )}
        </form>
      </div>
    );
  }
}

export default withRouter(TopicCreate);
