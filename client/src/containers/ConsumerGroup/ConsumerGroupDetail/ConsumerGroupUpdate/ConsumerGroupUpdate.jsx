import React from 'react';
import Header from '../../../Header/Header';
import Form from '../../../../components/Form/Form';
import Dropdown from 'react-bootstrap/Dropdown';
import DatePicker from '../../../../components/DatePicker';
import {formatDateTime, groupedTopicOffset} from '../../../../utils/converters';
import Joi from 'joi-browser';
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
    topicOffset: this.props.topicOffset || {},
    firstOffsets: {},
    lastOffsets: {},
    formData: {},
    checked: {},
    errors: {}
  };

  schema = {};

  componentDidMount() {
    const { clusterId, consumerGroupId } = this.props.match.params;

    this.setState({ clusterId, consumerGroupId }, () => {
      this.getTopicOffset();
    });
  }

  async getTopicOffset() {
    const { clusterId, consumerGroupId, topicOffset, timestamp} = this.state;
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
            'YYYY-MM-DDTHH:mm:ss.SSS'
          ) + 'Z'
        : '';

    let data = {};
    if (JSON.stringify(topicOffset) === JSON.stringify({})) {
      data = await this.getApi(uriConsumerGroup(clusterId, consumerGroupId));
      data = data.data;
      const topicOffset = groupedTopicOffset(data.offsets);

      if (data) {
        this.setState({ topicOffset:  topicOffset}, () =>
          this.createValidationSchema(topicOffset)
        );
      } else {
        this.setState({ topicOffset: {} });
      }
    } else if (date !== '') {
      data = await this.getApi(uriConsumerGroupOffsetsByTimestamp(clusterId, consumerGroupId, date));
      data = data.data;
      this.handleOffsetsByTimestamp(data);
    } else {
      this.createValidationSchema(topicOffset);
    }
  }

  createValidationSchema = topicOffset => {
    let { formData, checked} = this.state;
    let firstOffsets = {};
    let lastOffsets = {};
    let name = '';

    Object.keys(topicOffset).forEach(topidId => {
      checked[topidId] = true;
      topicOffset[topidId].forEach(offset => {
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

    this.setState({ formData, firstOffsets, lastOffsets, checked});
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

  unCheckAll = (value)  => {
    const {checked} = this.state;

    Object.keys(checked).forEach(name => {
      checked[name] = value;
    });

    this.setState({ checked});
  }

  resetToLastOffsets = () => {
    const { lastOffsets } = this.state;
    let { formData } = this.state;

    Object.keys(formData).forEach(name => {
      formData[name] = lastOffsets[name];
    });

    this.setState({ formData });
  };

  createSubmitBody = (formData, checked) => {
    let body = [];
    let splitName = [];
    let topic = '';
    let partition = '';

    Object.keys(formData).forEach(name => {
        splitName = name.split('-');
        partition = splitName.pop();
        topic = splitName.join('-');

      if (checked[topic] === true) {
        body.push({
          topic,
          partition,
          offset: formData[name]
        });
      }
    });

    return body;
  };

  async doSubmit() {
    const { clusterId, consumerGroupId, formData, checked } = this.state;

    await this.postApi(
      uriConsumerGroupUpdate(clusterId, consumerGroupId),
      this.createSubmitBody(formData, checked)
    );

    this.setState({ state: this.state });
    toast.success(`Offsets for '${consumerGroupId}' updated successfully.`);
  }

  rendertopicOffset = () => {
    const { topicOffset, checked } = this.state;
    const renderedItems = [];

    Object.keys(topicOffset).forEach(topicId => {
      renderedItems.push(
        <fieldset id={`fieldset-${topicId}`} key={topicId}>
          <legend id={`legend-${topicId}`}>
            <input
              type="checkbox"
              value={topicId}
              checked={checked[topicId] || false}
              onChange={this.checkedtopicOffset}/> {topicId}
          </legend>
          {this.renderPartitionInputs(topicOffset[topicId], topicId, !checked[topicId])}
        </fieldset>
      );
    });

    return renderedItems;
  };

  checkedtopicOffset = (event) => {
    const { checked } = this.state;
    checked[event.target.value] = event.target.checked;

    this.setState({ checked: checked });
  }

  renderPartitionInputs = (offsets, topicId, disabled) => {
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
                `partition-input ${name}-input`,
                { disabled: disabled }
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
          onClick={() => this.unCheckAll(true)}
        >
          Check all
        </div>
        <div
            className="btn btn-secondary"
            type="button"
            style={{ marginRight: '0.5rem' }}
            onClick={() => this.unCheckAll(false)}
        >
          Uncheck all
        </div>
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
                      this.setState({ timestamp: value }, () => this.getTopicOffset());
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
          {this.rendertopicOffset()}
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
