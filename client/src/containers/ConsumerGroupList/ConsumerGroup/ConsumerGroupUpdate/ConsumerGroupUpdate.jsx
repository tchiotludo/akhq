import React from 'react';
import Header from '../../../Header/Header';
import Form from '../../../../components/Form/Form';
import Dropdown from 'react-bootstrap/Dropdown';
import DatePicker from '../../../../components/DatePicker';
import { formatDateTime } from '../../../../utils/converters';
import Joi from 'joi-browser';
import { get, post } from '../../../../utils/api';
import {
  uriConsumerGroup,
  uriConsumerGroupOffsetsByTimestamp,
  uriConsumerGroupUpdate
} from '../../../../utils/endpoints';
import moment from 'moment';
import './styles.scss';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
class ConsumerGroupUpdate extends Form {
  state = {
    clusterId: '',
    consumerGroupId: '',
    timestamp: '',
    groupedTopicOffset: this.props.groupedTopicOffset || {},
    firstOffsets: {},
    lastOffsets: {},
    formData: {},
    errors: {}
  };

  schema = {};

  componentDidMount() {
    const { clusterId, consumerGroupId } = this.props.match.params;

    this.setState({ clusterId, consumerGroupId }, () => {
      this.getGroupedTopicOffset();
    });
  }

  async getGroupedTopicOffset() {
    const { clusterId, consumerGroupId, groupedTopicOffset, timestamp } = this.state;
    const { history } = this.props;
    const momentValue = moment(timestamp);

    const date =
      timestamp.toString().length > 0
        ? formatDateTime(
            {
              year: momentValue.year(),
              monthValue: momentValue.month(),
              dayOfMonth: momentValue.date(),
              hour: momentValue.hour(),
              minute: momentValue.minute(),
              second: momentValue.second(),
              milli: momentValue.millisecond()
            },
            'YYYY-MM-DDThh:mm:ss.SSS'
          ) + 'Z'
        : '';
    try {
      let data = {};
      if (JSON.stringify(groupedTopicOffset) === JSON.stringify({})) {
        data = await get(uriConsumerGroup(clusterId, consumerGroupId));
        data = data.data;
        if (data) {
          this.setState({ groupedTopicOffset: data.groupedTopicOffset }, () =>
            this.createValidationSchema(data.groupedTopicOffset)
          );
        } else {
          this.setState({ groupedTopicOffset: {} });
        }
      } else if (date !== '') {
        data = await get(uriConsumerGroupOffsetsByTimestamp(clusterId, consumerGroupId, date));
        data = data.data;
        this.handleOffsetsByTimestamp(data);
      } else {
        this.createValidationSchema(groupedTopicOffset);
      }
    } finally {
      history.replace({
        ...this.props.location,
        loading: false
      });
    }
  }

  createValidationSchema = groupedTopicOffset => {
    let { formData } = this.state;
    let firstOffsets = {};
    let lastOffsets = {};
    let name = '';

    Object.keys(groupedTopicOffset).forEach(topidId => {
      groupedTopicOffset[topidId].forEach(offset => {
        name = `${topidId}-${offset.partition}`;
        this.schema[name] = Joi.number()
          .min(offset.firstOffset || 0)
          .max(offset.lastOffset || 0)
          .required()
          .label(`Partition ${offset.partition} offset`);
        formData[name] = offset.offset || 0;
        firstOffsets[name] = offset.firstOffset || 0;
        lastOffsets[name] = offset.lastOffset || 0;
      });
    });

    this.setState({ formData, firstOffsets, lastOffsets });
  };

  handleOffsetsByTimestamp = offsets => {
    let { formData } = this.state;
    let topic = '';
    let partition = '';
    offsets.forEach(offset => {
      topic = offset.topic;
      partition = offset.partition;
      formData[`${topic}-${partition}`] = offset.offset;
    });
  };

  resetToFirstOffsets = () => {
    const { firstOffsets } = this.state;
    let { formData } = this.state;

    Object.keys(formData).forEach(name => {
      formData[name] = firstOffsets[name];
    });

    this.setState({ formData });
  };

  resetToLastOffsets = () => {
    const { lastOffsets } = this.state;
    let { formData } = this.state;

    Object.keys(formData).forEach(name => {
      formData[name] = lastOffsets[name];
    });

    this.setState({ formData });
  };

  createSubmitBody = formData => {
    let body = [];
    let splitName = [];
    let topic = '';
    let partition = '';

    Object.keys(formData).forEach(name => {
      splitName = name.split('-');
      topic = splitName[0];
      partition = splitName[1];
      body.push({
        topic,
        partition,
        offset: formData[name]
      });
    });

    return body;
  };

  async doSubmit() {
    const { clusterId, consumerGroupId, formData } = this.state;
    const { history } = this.props;
    history.replace({
      loading: true
    });
    try {
      await post(
        uriConsumerGroupUpdate(clusterId, consumerGroupId),
        this.createSubmitBody(formData)
      );

      this.setState({ state: this.state }, () =>
        this.props.history.replace({
          loading: false
        })
      );
      toast.success(`Offsets for '${consumerGroupId}' updated successfully.`);
    } catch (err) {
      this.props.history.replace({
        loading: false
      });
      console.error('Error:', err);
    }
  }

  renderGroupedTopicOffset = () => {
    const { groupedTopicOffset } = this.state;
    const renderedItems = [];

    Object.keys(groupedTopicOffset).forEach(topidId => {
      renderedItems.push(
        <fieldset id={`fieldset-${topidId}`} key={topidId}>
          <legend id={`legend-${topidId}`}>{topidId}</legend>
          {this.renderPartitionInputs(groupedTopicOffset[topidId], topidId)}
        </fieldset>
      );
    });

    return renderedItems;
  };

  renderPartitionInputs = (offsets, topicId) => {
    const renderedInputs = [];

    offsets.forEach(offset => {
      const name = `${topicId}-${offset.partition}`;

      renderedInputs.push(
        <div className="form-group row" key={name}>
          <div className="col-sm-10 partition-input">
            <span id={`${topicId}-${offset.partition}-input`}>
              {this.renderInput(
                name,
                `Partition: ${offset.partition}`,
                'Offset',
                'number',
                undefined,
                true,
                'partition-input-div',
                `partition-input ${name}-input`
              )}
            </span>
          </div>
        </div>
      );
    });

    return renderedInputs;
  };

  renderResetButton = () => {
    const { timestamp } = this.state;
    const { loading } = this.props.history.location;

    return (
      <span>
        <div
          className="btn btn-secondary"
          type="button"
          style={{ marginRight: '0.5rem' }}
          onClick={() => this.resetToFirstOffsets()}
        >
          Reset to first offsets
        </div>
        <div
          className="btn btn-secondary"
          type="button"
          style={{ marginRight: '0.5rem' }}
          onClick={() => this.resetToLastOffsets()}
        >
          Reset to last offsets
        </div>
        <div
          className="btn btn-secondary"
          type="button"
          style={{ marginRight: '0.5rem', padding: 0 }}
        >
          <Dropdown>
            <Dropdown.Toggle>Filter datetime</Dropdown.Toggle>
            {!loading && (
              <Dropdown.Menu>
                <div>
                  <DatePicker
                    showTimeInput
                    showDateTimeInput
                    value={timestamp}
                    label={''}
                    onChange={value => {
                      this.setState({ timestamp: value }, () => this.getGroupedTopicOffset());
                    }}
                  />
                </div>
              </Dropdown.Menu>
            )}
          </Dropdown>
        </div>
      </span>
    );
  };

  render() {
    const { consumerGroupId } = this.state;

    return (
      <div>
        <Header title={`Update offsets: ${consumerGroupId}`} history={this.props.history} />
        <form
          className="khq-form khq-update-consumer-group-offsets"
          onSubmit={() => this.handleSubmit()}
        >
          {this.renderGroupedTopicOffset()}
          {this.renderButton(
            'Update',
            this.handleSubmit,
            undefined,
            'submit',
            this.renderResetButton()
          )}
        </form>
      </div>
    );
  }
}

export default ConsumerGroupUpdate;
