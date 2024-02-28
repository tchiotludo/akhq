import React from 'react';
import Joi from 'joi-browser';
import Form from '../../../../components/Form/Form';
import Header from '../../../Header';
import { uriTopicIncreasePartition } from '../../../../utils/endpoints';
import { toast } from 'react-toastify';
import { withRouter } from '../../../../utils/withRouter';

class TopicIncreasePartition extends Form {
  state = {
    formData: {
      partition: 1
    },
    selectedCluster: this.props.match.params.clusterId,
    selectedTopic: this.props.match.params.topicId,
    errors: {}
  };

  componentDidMount() {
    this.getTopicsPartitions();
  }

  async getTopicsPartitions() {
    const { selectedCluster, selectedTopic } = this.state;

    let partitions = await this.getApi(uriTopicIncreasePartition(selectedCluster, selectedTopic));
    let form = {};
    form.partition = partitions.data.length;
    this.setState({ formData: form });
  }

  schema = {
    partition: Joi.number().min(1).label('Partition').required()
  };

  async doSubmit() {
    const { formData, selectedCluster, selectedTopic } = this.state;
    const partitionData = {
      partition: formData.partition
    };

    this.postApi(uriTopicIncreasePartition(selectedCluster, selectedTopic), partitionData).then(
      () => {
        this.props.history.push({
          pathname: `/ui/${selectedCluster}/topic`
        });
        toast.success('Topic partition updated');
      }
    );
  }
  render() {
    return (
      <div>
        <form
          encType="multipart/form-data"
          className="khq-form khq-form-config"
          onSubmit={() => this.doSubmit()}
        >
          <Header title="Increase topic partition" history={this.props.history} />
          {this.renderInput('partition', 'Partition', 'Partition', 'number')}
          {this.renderButton(
            'Update',
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

export default withRouter(TopicIncreasePartition);
