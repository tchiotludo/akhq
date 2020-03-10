import React from 'react';
import Form from '../../../../components/Form/Form';
import Header from '../../../Header';
import Joi from 'joi-browser';
import { withRouter } from 'react-router-dom';
import { post } from '../../../../utils/api';
import moment from 'moment';
import 'react-datepicker/dist/react-datepicker-cssmodules.css';
import DatePicker from '../../../../components/DatePicker';

class TopicProduce extends Form {
  state = {
    nHeaders: 1,
    formData: {
      partition: '',
      key: '',
      hKey0: '',
      hValue0: '',
      timestamp: moment(),
      value: ''
    },
    openDateModal: false,
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
    //timestamp: Joi.number().label('Timestamp'),
    value: Joi.number().label('Value')
  };

  doSubmit() {
    const { formData } = this.state;
    const { clusterId } = this.props.match.params;
    const topic = {
      clusterId,
      partition: formData.partition,
      key: formData.key,
      //timestamp: ,
      value: formData.value
    };

    let headers = {};

    Object.keys(formData).map(key => {
      if (key.includes('hKey')) {
        let keyNumbers = key.replace(/\D/g, '');
        headers[formData[key]] = formData[`hValue${keyNumbers}`];
      }
    });

    topic.headers = headers;

    // post(uriTopicsProduce(), topic).then(res => {
    //   this.props.history.push({
    //     pathname: `/${clusterId}/topic`,
    //     showSuccessToast: true,
    //     successToastMessage: 'Produced to Topic.'
    //   });
    // });
  }

  renderHeaders() {
    let headers = [];

    Object.keys(this.state.formData).map((key, index) => {
      if (key.includes('hKey')) {
        let keyNumbers = key.replace(/\D/g, '');
        headers.push(this.renderHeader(Number(keyNumbers)));
      }
    });
    return <>{headers.map(head => head)}</>;
  }

  onHeaderChange = ({ currentTarget: input }, position) => {
    const { formData } = this.state;

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
      <div className="row header-wrapper">
        <label class="col-sm-2 col-form-label">{position === 0 ? 'Header' : ''}</label>

        <div class="row col-sm-10 khq-multiple header-row">
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
          <div className="add-button">
            <button
              type="button"
              class="btn btn-secondary"
              onClick={() => {
                position === 0 ? this.handlePlus() : this.handleRemove(position);
              }}
            >
              <i class={`fa ${position === 0 ? 'fa-plus' : 'fa-trash'}`}></i>
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
        .label(`hKey${nHeaders}`),
      [`hValue${nHeaders}`]: Joi.string()
        .min(1)
        .label(`hValue${nHeaders}`)
    };
    this.setState({ nHeaders: this.state.nHeaders + 1, formData: newFormData });
  }

  handleRemove(position) {
    const state = {
      ...this.state
    };
    delete state.formData[`hKey${position}`];
    delete state.formData[`hValue${position}`];
    this.setState(state);
  }

  render() {
    const { clusterId, topicId, timestamp, openDateModal } = this.state;

    return (
      <div id="content" style={{ overflow: 'auto', paddingRight: '20px', marginRight: 0 }}>
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
          {this.renderDatePicker('timestamp', 'Timestamp', value => {
            console.log('value', value);
            this.setState({ formData: { ...this.state, timestamp: value } });
          })}
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
