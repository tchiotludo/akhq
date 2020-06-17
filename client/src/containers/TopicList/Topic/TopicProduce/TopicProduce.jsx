import React from 'react';
import Form from '../../../../components/Form/Form';
import Header from '../../../Header';
import Joi from 'joi-browser';
import Dropdown from 'react-bootstrap/Dropdown';
import { withRouter } from 'react-router-dom';
import { post, get } from '../../../../utils/api';
import { uriTopicsProduce, uriTopicsPartitions } from '../../../../utils/endpoints';
import { formatDateTime } from '../../../../utils/converters';
import moment from 'moment';
import DatePicker from '../../../../components/DatePicker';
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
      value: ''
    },
    datetime: new Date(),
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
      .allow('')
      .label('hKey0'),
    hValue0: Joi.string()
      .allow('')
      .label('hValue0'),
    value: Joi.string()
      .allow('')
      .label('Value')
  };

  async componentDidMount() {
    const { clusterId, topicId } = this.props.match.params;
    this.props.history.replace({
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
      this.props.history.replace({
        ...this.props.location,
        loading: false
      });
    }
    this.setState({ clusterId, topicId });
  }

  doSubmit() {
    const { formData, datetime } = this.state;
    const { clusterId, topicId } = this.props.match.params;
    const topic = {
      clusterId,
      topicId,
      partition: formData.partition,
      key: formData.key,
      timestamp: datetime.toISOString(),
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
    this.props.history.replace({
      ...this.props.location,
      loading: true
    });
    post(uriTopicsProduce(clusterId, topicId), topic)
      .then(() => {
        this.props.history.push({
          ...this.props.location,
          pathname: `/ui/${clusterId}/topic`,
          showSuccessToast: true,
          successToastMessage: `Produced to ${topicId}.`,
          loading: false
        });
      })
      .catch(() => {
        this.props.history.replace({
          ...this.props.location,
          showErrorToast: true,
          errorToastMessage: 'There was an error while producing to topic.',
          loading: false
        });
      });
  }

  renderHeaders() {
    let headers = [];

    Object.keys(this.state.formData).map(key => {
      if (key.includes('hKey')) {
        let keyNumbers = key.replace(/\D/g, '');
        headers.push(this.renderHeader(Number(keyNumbers)));
      }
    });
    return <div data-testId="headers">{headers.map(head => head)}</div>;
  }

  renderHeader(position) {
    return (
      <div className="row header-wrapper">
        <label className="col-sm-2 col-form-label">{position === 0 ? 'Header' : ''}</label>

        <div className="row col-sm-10 khq-multiple header-row">
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
              className="btn btn-secondary"
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
      topicId,
      openDateModal,
      formData,
      partitions,
      selectableValueFormats,
      datetime
    } = this.state;
    let date = moment(datetime);
    return (
      <div style={{ overflow: 'hidden', paddingRight: '20px', marginRight: 0 }}>
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
          <div style={{ display: 'flex', flexDirection: 'row', width: '100%', padding: 0 }}>
            <label
              style={{ padding: 0, alignItems: 'center', display: 'flex' }}
              className="col-sm-2 col-form-label"
            >
              Timestamp:
            </label>
            <Dropdown style={{ width: '100%', padding: 0, margin: 0 }}>
              <Dropdown.Toggle
                style={{
                  width: '100%',
                  display: 'flex',
                  flexDirection: 'row',
                  alignItems: 'center',
                  padding: 0,
                  margin: 0
                }}
                className="nav-link dropdown-toggle"
              >
                <input
                  className="form-control"
                  value={
                    datetime !== '' &&
                    ' ' +
                      formatDateTime(
                        {
                          year: date.year(),
                          monthValue: date.month(),
                          dayOfMonth: date.date(),
                          hour: date.hour(),
                          minute: date.minute(),
                          second: date.second()
                        },
                        'DD-MM-YYYY HH:mm'
                      )
                  }
                  placeholder={
                    datetime !== '' &&
                    ' ' +
                      formatDateTime(
                        {
                          year: date.year(),
                          monthValue: date.month(),
                          dayOfMonth: date.date(),
                          hour: date.hour(),
                          minute: date.minute(),
                          second: date.second()
                        },
                        'DD-MM-YYYY HH:mm'
                      )
                  }
                />
              </Dropdown.Toggle>
              <Dropdown.Menu>
                <div className="input-group">
                  <DatePicker
                    showTimeInput
                    value={datetime}
                    onChange={value => {
                      this.setState({
                        datetime: value
                      });
                    }}
                  />
                </div>
              </Dropdown.Menu>
            </Dropdown>
          </div>

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
