import React from 'react';
import Form from '../../../../components/Form/Form';
import Header from '../../../Header';
import Joi from 'joi-browser';
import { withRouter } from 'react-router-dom';
import { post } from '../../../../utils/api';
import { uriTopicsProduce } from '../../../../utils/endpoints';
class TopicProduce extends Form {
  state = {
    nHeaders: 1,
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

    post(uriTopicsProduce(), topic).then(res => {
      this.props.history.push({
        pathname: `/${clusterId}/topic`,
        showSuccessToast: true,
        successToastMessage: 'Produced to Topic.'
      });
    });
  }
  renderHeader() {
    let header = (
      <div
        class="row col-sm-10 khq-multiple"
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          minWidth: '80%',
          marginLeft: '0.05%'
        }}
      >
        {this.renderInput(
          '',
          '',
          'Key',
          'headers',
          undefined,
          true,
          'wrapper-class col-sm-6 ',
          'input-class'
        )}

        {this.renderInput(
          '',
          '',
          'Value',
          'headers',
          undefined,
          true,
          'wrapper-class col-sm-6',
          'input-class'
        )}
       <div style={{ paddingBottom: '1.5%' }}>
          <button
            type="button"
            class="btn btn-secondary"
            onClick={() => {
              this.handlePlus();
            }}
          >
            <i class="fa fa-plus"></i>
          </button>
       </div>
      </div>
    );

    let headers = [];
    for (let i = 0; i < this.state.nHeaders; i++) {
      headers.push(header);
    }
    return <>{headers.map(head => head)}</>;
  }
  handlePlus() {
    this.setState({ nHeaders: this.state.nHeaders + 1 });
  }
  render() {
    const { clusterId, topicId } = this.state;
    return (
      <div id="content">
        <form
          encType="multipart/form-data"
          className="khq-form khq-form-config"
          //onSubmit={() => this.doSubmit()}
        >
          <div>
            <Header title={`Produce to ${topicId} `} />
            {this.renderInput('partition', 'Partition', 'Partition', 'partition')}
            {this.renderInput('key', 'Key', 'Key', 'Key')}
            <div></div>
          </div>
          <div className="row">
            <label class="col-sm-2 col-form-label">Headers</label>

            {this.renderHeader()}
          </div>
          {this.renderInput('value', 'Value', 'Value', 'Value')}

          {this.renderButton(
            'Produce',
            () => {
              this.doSubmit();
            },
            undefined,
            'button'
          )}
          <button
            type="button"
            onClick={() => {
              console.log('clicked');
              this.handlePlus();
            }}
          >
            clickme
          </button>
        </form>
      </div>
    );
  }
}

export default withRouter(TopicProduce);
