import React from 'react';
import Form from '../../../components/Form/Form';
import Header from '../../Header';
import Joi from 'joi-browser';
import Dropdown from 'react-bootstrap/Dropdown';
import { formatDateTime } from '../../../utils/converters';
import { popProduceToTopicValues } from '../../../utils/localstorage';
import {
  uriTopics,
  uriTopicsPartitions,
  uriTopicsProduce,
  uriAllSchema
} from '../../../utils/endpoints';
import moment from 'moment';
import DatePicker from '../../../components/DatePicker';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { Tooltip } from '@mui/material';
import { withRouter } from '../../../utils/withRouter';
import { Col, Row } from 'react-bootstrap';
import { format } from 'date-fns';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPlus, faTrash } from '@fortawesome/free-solid-svg-icons';

class TopicProduce extends Form {
  state = {
    partitions: [],
    nHeaders: 1,
    formData: {
      partition: '',
      key: '',
      hKey0: '',
      hValue0: '',
      value: '',
      keyValueSeparator: ':'
    },
    datetime: new Date(),
    openDateModal: false,
    errors: {},
    selectableValueFormats: [
      { _id: 'string', name: 'string' },
      { _id: 'json', name: 'json' }
    ],
    keySchema: [],
    keySchemaSearchValue: '',
    selectedKeySchema: '',
    valueSchema: [],
    valueSchemaSearchValue: '',
    selectedValueSchema: '',
    clusterId: '',
    topicId: '',
    topics: [],
    topicsSearchValue: '',
    multiMessage: false,
    tombstone: false,
    valuePlaceholder: '{"param": "value"}',
    roles: JSON.parse(sessionStorage.getItem('roles'))
  };

  schema = {
    partition: Joi.number().allow(null).label('Partition'),
    key: Joi.string().allow('').label('Key'),
    hKey0: Joi.string().allow('').label('hKey0'),
    hValue0: Joi.string().allow('').label('hValue0'),
    value: Joi.string().allow('').label('Value'),
    keyValueSeparator: Joi.string().min(1).label('keyValueSeparator')
  };

  async componentDidMount() {
    const { clusterId, topicId } = this.props.params;
    const { roles } = this.state;

    let response = await this.getApi(uriTopicsPartitions(clusterId, topicId));
    let partitions = response.data.map(item => {
      return { name: item.id, _id: Number(item.id) };
    });
    partitions.unshift({ name: 'auto assign', _id: null });

    this.setState({
      partitions,
      topicId,
      formData: {
        ...this.state.formData,
        partition: partitions[0]._id
      }
    });

    if (roles.SCHEMA && roles.SCHEMA.includes('READ')) {
      await this.getPreferredSchemaForTopic();
    }

    const topicEventData = popProduceToTopicValues();
    if (Object.keys(topicEventData).length) {
      await this.initByTopicEvent(topicEventData);
    }

    this.setState({ clusterId, topicId });

    this.initAvailableTopics();
  }

  async initAvailableTopics() {
    const { clusterId } = this.props.params;

    const topics = [];
    let page = 0;
    let topicResponseData = {};
    do {
      page = page + 1;
      topicResponseData = await this.getApi(uriTopics(clusterId, '', '', page)).then(
        res => res.data
      );
      topicResponseData.results.forEach(topicData => topics.push(topicData.name));
    } while (page < topicResponseData.pages);

    this.setState({
      topics
    });
  }

  async getPreferredSchemaForTopic() {
    const { clusterId, topicId } = this.props.params;
    let schema = await this.getApi(uriAllSchema(clusterId));
    let keySchema = [];
    let valueSchema = [];
    schema.data.filter(s => s.includes('-key')).map(s => keySchema.push(s));
    schema.data.filter(s => s.includes('-value')).map(s => valueSchema.push(s));
    this.setState({
      keySchema: keySchema,
      valueSchema: valueSchema,
      selectedKeySchema: keySchema.find(s => s === topicId + '-key'),
      selectedValueSchema: valueSchema.find(s => s === topicId + '-value')
    });
  }

  async initByTopicEvent(copyValues) {
    const { headers, ...topicValuesDefault } = copyValues;

    let nHeaders = headers.length === 0 ? 1 : 0;

    let newFormData = {
      ...this.state.formData,
      ...topicValuesDefault
    };

    for (const { key, value } of headers) {
      newFormData[`hKey${nHeaders}`] = key;
      newFormData[`hValue${nHeaders}`] = value;

      this.schema = {
        ...this.schema,
        [`hKey${nHeaders}`]: Joi.string().min(1).label(`hKey${nHeaders}`),
        [`hValue${nHeaders}`]: Joi.string().min(1).label(`hValue${nHeaders}`)
      };

      nHeaders++;
    }

    this.setState({
      formData: newFormData,
      nHeaders: nHeaders
    });
  }

  doSubmit() {
    const {
      formData,
      topicId,
      datetime,
      selectedKeySchema,
      selectedValueSchema,
      multiMessage,
      tombstone
    } = this.state;
    const { clusterId } = this.props.params;

    let value;
    if (tombstone) {
      value = null;
    } else {
      value = multiMessage ? formData.value : JSON.parse(JSON.stringify(formData.value));
    }
    const topic = {
      clusterId,
      topicId,
      topics: [topicId],
      partition: formData.partition,
      key: formData.key,
      timestamp: datetime.toISOString(),
      value: value,
      keySchema: selectedKeySchema,
      valueSchema: selectedValueSchema,
      multiMessage: multiMessage,
      keyValueSeparator: formData.keyValueSeparator
    };

    const headers = [];
    Object.keys(formData).forEach(key => {
      if (key.includes('hKey')) {
        let keyNumbers = key.replace(/\D/g, '');
        headers.push({
          key: formData[key],
          value: formData[`hValue${keyNumbers}`]
        });
      }
    });

    topic.headers = headers;

    this.postApi(uriTopicsProduce(clusterId, topicId), topic).then(() => {
      toast.success(`Produced to ${topicId}.`);
      this.props.router.navigate({ pathname: `/ui/${clusterId}/topic/${topicId}` });
    });
  }

  renderMultiMessage(tombstone) {
    const { formData, multiMessage } = this.state;

    return (
      <Row className="form-group align-items-center">
        <Col xs="2">
          <label>Multi message</label>
        </Col>
        <Col xs="auto">
          {this.renderCheckbox(
            'isMultiMessage',
            multiMessage,
            () => {
              this.setState({
                multiMessage: !multiMessage,
                valuePlaceholder: this.getPlaceholderValue(
                  !multiMessage,
                  formData.keyValueSeparator
                )
              });
            },
            { disabled: tombstone }
          )}
        </Col>
        <Col xs="auto">
          <label>Separator</label>
        </Col>
        <Col xs="2">
          <input
            className="form-control"
            type="text"
            id="keyValueSeparator"
            placeholder=":"
            disabled={!multiMessage}
            onChange={event => {
              this.setState({
                formData: { ...formData, keyValueSeparator: event.target.value },
                valuePlaceholder: this.getPlaceholderValue(!multiMessage, event.target.value)
              });
            }}
          />
        </Col>
      </Row>
    );
  }

  renderTombstone(multiMessage) {
    const { tombstone } = this.state;

    return (
      <Row>
        <Col xs="2">
          <label className="col-sm-2 col-form-label">Tombstone</label>
        </Col>
        <Col xs="auto">
          {this.renderCheckbox(
            'isTombstone',
            tombstone,
            () => {
              this.setState({ tombstone: !tombstone });
            },
            { disabled: multiMessage }
          )}
        </Col>
      </Row>
    );
  }

  getPlaceholderValue(isMultiMessage, keyValueSeparator) {
    if (isMultiMessage) {
      return (
        'key1' +
        keyValueSeparator +
        '{"param": "value1"}\n' +
        'key2' +
        keyValueSeparator +
        '{"param": "value2"}'
      );
    } else {
      return '{"param": "value"}';
    }
  }

  renderHeaders() {
    let headers = [];

    Object.keys(this.state.formData).forEach(key => {
      if (key.includes('hKey')) {
        let keyNumbers = key.replace(/\D/g, '');
        headers.push(this.renderHeader(Number(keyNumbers)));
      }
    });
    return (
      <div data-testid="headers" className="form-group">
        {headers.map(head => head)}
      </div>
    );
  }

  renderHeader(position) {
    return (
      <Row>
        <Col xs="2">
          <label className="col-sm-2 col-form-label">{position === 0 ? 'Header' : ''}</label>
        </Col>

        <Col>{this.renderInput(`hKey${position}`, '', 'Key', 'text', true, '', 'input-class')}</Col>
        <Col>
          {this.renderInput(`hValue${position}`, '', 'Value', 'text', true, '', 'input-class')}
        </Col>
        <Col xs="auto">
          <button
            type="button"
            className="btn btn-secondary"
            data-testid={`button_${position}`}
            onClick={() => {
              position === 0 ? this.handlePlus() : this.handleRemove(position);
            }}
          >
            <FontAwesomeIcon icon={position === 0 ? faPlus : faTrash} />
          </button>
        </Col>
      </Row>
    );
  }

  handlePlus(providedKey, providedValue) {
    const { formData, nHeaders } = this.state;

    let newFormData = {
      ...formData,
      [`hKey${nHeaders}`]: providedKey || '',
      [`hValue${nHeaders}`]: providedValue || ''
    };
    this.schema = {
      ...this.schema,
      [`hKey${nHeaders}`]: Joi.string().min(1).label(`hKey${nHeaders}`),
      [`hValue${nHeaders}`]: Joi.string().min(1).label(`hValue${nHeaders}`)
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

  renderResults = (results, searchValue, selectedValue, tag) => {
    const { roles } = this.state;

    return (
      <div style={{ maxHeight: 'calc(100vh - 300px)', overflowY: 'auto', minHeight: '89px' }}>
        {results
          .filter(key => {
            if (searchValue.length > 0) {
              return key.includes(searchValue);
            }
            return results;
          })
          .map((key, index) => {
            let selected = selectedValue === key ? 'selected' : '';
            return (
              <Dropdown.Item key={index} active={selected}>
                <Tooltip
                  title={
                    selectedValue === key && tag !== 'topicId'
                      ? 'Click to unselect option'
                      : 'Click to select option'
                  }
                >
                  <div
                    onClick={() => {
                      if (tag === 'key') {
                        selectedValue === key
                          ? this.setState({ selectedKeySchema: '' })
                          : this.setState({ selectedKeySchema: key });
                      } else if (tag === 'value') {
                        selectedValue === key
                          ? this.setState({ selectedValueSchema: '' })
                          : this.setState({ selectedValueSchema: key });
                      } else if (tag === 'topicId') {
                        if (selectedValue !== key) {
                          this.setState({ topicId: key });
                          if (roles.SCHEMA && roles.SCHEMA.includes('READ')) {
                            this.getPreferredSchemaForTopic();
                          }
                        }
                      }
                    }}
                    role="option"
                    id={`bs-select-${index}-0`}
                    aria-selected="false"
                  >
                    <span className="text">{key}</span>
                  </div>
                </Tooltip>
              </Dropdown.Item>
            );
          })}
      </div>
    );
  };

  render() {
    const {
      topicId,
      topics,
      topicsSearchValue,
      formData,
      partitions,
      datetime,
      keySchema,
      keySchemaSearchValue,
      selectedKeySchema,
      valueSchema,
      valueSchemaSearchValue,
      selectedValueSchema,
      multiMessage,
      tombstone
    } = this.state;
    let date = moment(datetime);
    return (
      <div>
        <form encType="multipart/form-data" className="khq-form khq-form-config">
          <Header title={`Produce to ${topicId} `} />
          {this.renderDropdown(
            'Topic',
            topicsSearchValue,
            topicId,
            value => {
              this.setState({
                topicsSearchValue: value.target.value
              });
            },
            this.renderResults(topics, topicsSearchValue, topicId, 'topicId')
          )}
          {this.renderSelect(
            'partition',
            'Partition',
            partitions,
            value => {
              this.setState({ formData: { ...formData, partition: value.target.value } });
            },
            'col-sm-10'
          )}
          {this.renderDropdown(
            'Key schema',
            keySchemaSearchValue,
            selectedKeySchema,
            value => {
              this.setState({
                keySchemaSearchValue: value.target.value,
                selectedKeySchema: value.target.value
              });
            },
            this.renderResults(keySchema, keySchemaSearchValue, selectedKeySchema, 'key')
          )}

          {this.renderInput(
            'key',
            'Key',
            'Key',
            'Key',
            undefined,
            undefined,
            undefined,
            undefined,
            { disabled: multiMessage }
          )}

          {this.renderHeaders()}

          {this.renderDropdown(
            'Value schema',
            valueSchemaSearchValue,
            selectedValueSchema,
            value => {
              this.setState({
                valueSchemaSearchValue: value.target.value,
                selectedValueSchema: value.target.value
              });
            },
            this.renderResults(valueSchema, valueSchemaSearchValue, selectedValueSchema, 'value')
          )}

          {this.renderMultiMessage(tombstone)}

          {this.renderTombstone(multiMessage)}

          {this.renderJSONInput(
            'value',
            'Value',
            value => {
              this.setState({
                formData: {
                  ...formData,
                  value: value
                }
              });
            },
            multiMessage, // true -> 'text' mode; json, protobuff, ... mode otherwise
            { placeholder: this.getPlaceholderValue(multiMessage, formData.keyValueSeparator) },
            { readOnly: tombstone }
          )}
          <div style={{ display: 'flex', flexDirection: 'row', width: '100%', padding: 0 }}>
            <label
              style={{ padding: 0, alignItems: 'center', display: 'flex' }}
              className="col-sm-2 col-form-label"
            >
              Timestamp {format(new Date(), 'z')}
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
                  defaultValue={
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
                    showDateTimeInput
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

export default withRouter(TopicProduce);
