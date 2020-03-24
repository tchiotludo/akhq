import React from 'react';
import Form from '../../../../components/Form/Form';
import Header from '../../../Header';
import Joi from 'joi-browser';
import { withRouter } from 'react-router-dom';
import { post, get } from '../../../../utils/api';
import { uriTopicsProduce, uriTopicsPartitions } from '../../../../utils/endpoints';
import moment from 'moment';
import 'react-datepicker/dist/react-datepicker-cssmodules.css';
import history from '../../../../utils/history';

class TopicProduce extends Form {
  state = {
    partitions: [],
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
    errors: {},
    selectableValueFormats: [
      { _id: 'string', name: 'string' },
      { _id: 'json', name: 'json' }
    ]
  };

  schema = {
    partition: Joi.number()
      .min(0)
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
    timestamp: Joi.object().label('Timestamp'),
    value: Joi.string().label('Value')
  };

  async componentDidMount() {
    const { clusterId, topicId } = this.props.match.params;
    this.props.history.push({
      ...this.props.location,
      loading: true
    });
    try {
      let response = await get(uriTopicsPartitions(clusterId, topicId));
      let partitions = response.data.map(item => {
        return { name: item.id, _id: Number(item.id) };
      });
      this.setState({
        partitions,
        formData: {
          ...this.state.formData,
          partition: partitions[0]._id
        }
      });
    } catch (err) {
      console.error('err', err);
    } finally {
      this.props.history.push({
        ...this.props.location,
        loading: false
      });
    }
    this.setState({ clusterId, topicId });
  }

  doSubmit() {
    const { formData } = this.state;
    const { clusterId, topicId } = this.props.match.params;
    const topic = {
      clusterId,
      topicId,
      partition: formData.partition,
      key: formData.key,
      timestamp: formData.timestamp.toDate().toISOString(),
      value: JSON.parse(JSON.stringify(formData.value))
    };

    let headers = {};
    Object.keys(formData).map(key => {
      if (key.includes('hKey')) {
        let keyNumbers = key.replace(/\D/g, '');
        headers[formData[key]] = formData[`hValue${keyNumbers}`];
      }
    });

    topic.headers = headers;
    this.props.history.push({
      ...this.props.location,
      loading: true
    });
    post(uriTopicsProduce(), topic)
      .then(res => {
        this.props.history.push({
          ...this.props.location,
          pathname: `/${clusterId}/topic`,
          showSuccessToast: true,
          successToastMessage: `Produced to ${topicId}.`,
          loading: false
        });
      })
      .catch(err => {
        this.props.history.push({
          ...this.props.location,
          showErrorToast: true,
          errorToastMessage: 'There was an error while producing to topic.',
          loading: false
        });
      });
  }

  renderHeaders() {
    let headers = [];

    Object.keys(this.state.formData).map((key, index) => {
      if (key.includes('hKey')) {
        let keyNumbers = key.replace(/\D/g, '');
        headers.push(this.renderHeader(Number(keyNumbers)));
      }
    });
    return <div data-testId="headers">{headers.map(head => head)}</div>;
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
              data-testId={`button_${position}`}
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
    const {
      clusterId,
      topicId,
      timestamp,
      openDateModal,
      formData,
      partitions,
      selectableValueFormats
    } = this.state;
    return (
      <div id="content" style={{ overflow: 'auto', paddingRight: '20px', marginRight: 0 }}>
        <form encType="multipart/form-data" className="khq-form khq-form-config">
          <div>
            <Header title={`Produce to ${topicId} `} />
            {this.renderSelect('partition', 'Partition', partitions, value => {
              this.setState({ formData: { ...formData, partition: value.target.value } });
            })}
            {this.renderInput('key', 'Key', 'Key', 'Key')}
            <div></div>
          </div>

          {this.renderHeaders()}
          {this.renderSelect('valueFormat', 'Value Format', selectableValueFormats, value => {
            if (value.target.value === 'string') {
              this.schema.value = Joi.string().label('Value');
              this.forceUpdate();
            } else if (value.target.value === 'json') {
              this.schema.value = Joi.any().label('Value');
              this.forceUpdate();
            }
          })}
          {this.renderJSONInput('value', 'Value', value => {
            this.setState({
              formData: {
                ...formData,
                value: value
              }
            });
          })}

          {this.renderDatePicker('timestamp', 'Timestamp', value => {
            this.setState({ formData: { ...this.state.formData, timestamp: value } });
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

export default TopicProduce;
