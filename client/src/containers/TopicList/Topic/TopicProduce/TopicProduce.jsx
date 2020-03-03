import React, { Component } from 'react';
import Form from '../../../../components/Form/Form';
import Header from '../../../Header';
import Joi from 'joi-browser';
import { withRouter } from 'react-router-dom';
import { post } from '../../../../utils/api';
//import { uriTopicProduce} from '../../../../utils//endpoints';
class TopicProduce extends Component {
  state = {
    formData: {
      partition: '',
      key: '',
      headers: [
        {
          key: '',
          value: ''
        }
      ],
      timestamp: ''
    },
    errors: {}
  };
  componentDidMount() {
    const { clusterId, topicId } = this.props.match.params;

    this.setState({ clusterId, topicId });
  }
  schema = {
    partition: Joi.number()
      .min(1)
      .label('Partition')
      .required(),
    key: Joi.string()
      .min(1)
      .label('Key')
      .required(),
    headers: [
      {
        key: Joi.string()
          .min(1)
          .label('Key')
          .required(),
        value: Joi.string()
          .min(1)
          .label('Value')
          .required()
      }
    ],
    timestamp: Joi.number().label('Timestamp'),
    value: Joi.number().label('Value')
  };

  onCleanupChange = value => {
    let { formData } = { ...this.state };
    formData.cleanup = value;

    this.setState({ formData });
  };
  /*
  doSubmit() {
    const { formData } = this.state;
    const { clusterId } = this.props.match.params;
    const topic = {
      clusterId,
      partition: formData.partition,
      key: formData.key,
      headers: formData.headers,
      timestamp: formData.timestamp,
      value: formData.value
    };

    post(uriTopicProduce(), topic).then(res => {
      this.props.history.push({
        pathname: `/${clusterId}/topic`,
        showSuccessToast: true,
        successToastMessage: `Peroduced to Topic.`
      });
    });
  }
*/
  render() {
    const { clusterId, topicId } = this.state;
    console.log('topicId' + topicId);
    return (
      /*    <div id="content">
        <form
          encType="multipart/form-data"
          className="khq-form khq-form-config"
          onSubmit={() => this.doSubmit()}
        >
          <Header title={`Produce to ${topicId} `} />
          {this.renderInput('partition', 'Partition', 'Partition', 'number')}
          {this.renderInput('key', 'Key', 'Key', 'number')}

          {this.renderInput('headers', 'Headers', 'Headers', 'number')}
          {this.renderInput('value', 'Value', 'Value', 'number')}

          {this.renderButton(
            'Produce',
            () => {
              this.doSubmit();
            },
            undefined,
            'button'
          )}
        </form>
      </div>*/
      <div></div>
    );
  }
}

export default TopicProduce;
