import React from 'react';
import Joi from 'joi-browser';
import Form from '../../../components/Form/Form';
import Header from '../../Header';
import { uriTopicsCreate, uriTopicDefaultConf } from '../../../utils/endpoints';
import { toast } from 'react-toastify';
import { withRouter } from '../../../utils/withRouter';

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

  componentDidMount() {
    this.getTopicDefaultConf();
  }

  async getTopicDefaultConf() {
    let { formData } = { ...this.state };
    const defaults = await this.getApi(uriTopicDefaultConf());

    formData.retention = defaults.data.retention;
    formData.partition = defaults.data.partition;
    formData.replication = defaults.data.replication;

    this.setState({ formData });
  }

  schema = {
    name: Joi.string().required().label('Name'),
    partition: Joi.number().min(1).label('Partition').required(),
    replication: Joi.number().min(1).label('Replication').required(),
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

    this.postApi(uriTopicsCreate(clusterId), topic).then(() => {
      this.props.history.push({
        pathname: `/ui/${clusterId}/topic`
      });
      toast.success(`Topic '${formData.name}' created`);
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
          <Header title="Create a topic" history={this.props.history} />
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
