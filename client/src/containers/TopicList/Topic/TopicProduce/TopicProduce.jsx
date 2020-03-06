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
      hKey0: '',
      hValue0: '',
      timestamp: '',
      value: ''
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
    hKey0: Joi.string()
      .min(1)
      .label('hKey0')
      .required(),
    hValue0: Joi.string()
      .min(1)
      .label('hValue0')
      .required(),
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

  renderHeaders() {
    let headers = [];

    Object.keys(this.state.formData).map((key, index) => {
      if (key.includes('hKey')) {
        headers.push(this.renderHeader(index));
      }
    });
    return <>{headers.map(head => head)}</>;
  }

  onHeaderChange = ({ currentTarget: input }, position) => {
    const { formData } = this.state;

    console.log(
      'onHeaderChange im in: --inputname-- ',
      input.name,
      ' ---position---',
      position,
      ` ----formData.headers[${position}][${input.name}]---- `,
      formData.headers[position][input.name]
    );

    const errors = { ...this.state.errors };
    const errorMessage = this.validateProperty(input);
    if (errorMessage) {
      errors[input.name] = errorMessage;
    } else {
      delete errors[input.name];
    }

    formData.headers[position][input.name] = input.value;
    this.setState({ formData, errors });
  };

  renderHeader(position) {
    const { formData } = this.state;
    return (
      <div className="row">
        <label class="col-sm-2 col-form-label">{position === 0 ? 'Header' : ''}</label>

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
            `hKey${position}`,
            '',
            'Key',
            'text',
            undefined,
            true,
            'wrapper-class col-sm-6 ',
            'input-class'
          )}

          {this.renderInput(
            `hValue${position}`,
            '',
            'Value',
            'text',
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
      </div>
    );
  }

  handlePlus() {
    const { formData, nHeaders } = this.state;

    let newFormData = {
      ...formData,
      [`hKey${nHeaders}`]: '',
      [`hValue${nHeaders}`]: ''
    };
    this.schema = {
      ...this.schema,
      [`hKey${nHeaders}`]: Joi.string()
        .min(1)
        .label(`hKey${nHeaders}`)
        .required(),
      [`hValue${nHeaders}`]: Joi.string()
        .min(1)
        .label(`hValue${nHeaders}`)
        .required()
    };
    this.setState({ nHeaders: this.state.nHeaders + 1, formData: newFormData }, () => {
      console.log('schema', this.schema);
    });
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

          {this.renderHeaders()}

          {this.renderInput('value', 'Value', 'Value', 'Value')}

          {this.renderButton(
            'Produce',
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

export default withRouter(TopicProduce);
