import React from 'react';
import Header from '../../../Header/Header';
import Form from '../../../../components/Form/Form';
import Dropdown from 'react-bootstrap/Dropdown';
import DatePicker from '../../../../components/DatePicker/DatePicker';
import { formatDateTime } from '../../../../utils/converters';
import Joi from 'joi-browser';
import { get, post } from '../../../../utils/api';
import {
  uriConsumerGroupGroupedTopicOffset,
  uriConsumerGroupUpdate
} from '../../../../utils/endpoints';
import { Link } from 'react-router-dom';
import moment from 'moment';
import './styles.scss';

class ConsumerGroupUpdate extends Form {
  state = {
    clusterId: '',
    consumerGroupId: '',
    timestamp: '',
    groupedTopicOffset: {},
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
    const { clusterId, consumerGroupId, timestamp } = this.state;
    const { history } = this.props;
    let data = {};

    history.push({
      ...this.props.location,
      loading: true
    });
    try {
      data = await get(uriConsumerGroupGroupedTopicOffset(clusterId, consumerGroupId, timestamp));
      data = data.data;

      if (data) {
        this.setState({ groupedTopicOffset: data.groupedTopicOffset }, () =>
          this.createValidationSchema(data.groupedTopicOffset)
        );
      } else {
        this.setState({ groupedTopicOffset: {} });
      }
    } catch (err) {
      history.replace('/error', { errorData: err });
    } finally {
      history.push({
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

    Object.keys(groupedTopicOffset).map(topidId => {
      groupedTopicOffset[topidId].map(offset => {
        name = `offset[${topidId}][${offset.partition}]`;
        this.schema[name] = Joi.number()
          .min(offset.firstOffset)
          .max(offset.lastOffset)
          .required()
          .label(`Partition ${offset.partition} offset`);
        formData[name] = offset.offset;
        firstOffsets[name] = offset.firstOffset;
        lastOffsets[name] = offset.lastOffset;
      });
    });

    this.setState({ formData, firstOffsets, lastOffsets });
  };

  resetToFirstOffsets = () => {
    const { firstOffsets } = this.state;
    let { formData } = this.state;

    Object.keys(formData).map(name => {
      formData[name] = firstOffsets[name];
    });

    this.setState({ formData });
  };

  resetToLastOffsets = () => {
    const { lastOffsets } = this.state;
    let { formData } = this.state;

    Object.keys(formData).map(name => {
      formData[name] = lastOffsets[name];
    });

    this.setState({ formData });
  };

  async doSubmit() {
    const { clusterId, consumerGroupId, formData } = this.state;
    const { history, location } = this.props;
    history.push({
      loading: true
    });
    try {
      await post(uriConsumerGroupUpdate(), {
        clusterId,
        groupId: consumerGroupId,
        offsets: formData
      });

      this.setState({ state: this.state }, () =>
        this.props.history.push({
          showSuccessToast: true,
          successToastMessage: `Offsets for '${consumerGroupId}' is updated`,
          loading: false
        })
      );
    } catch (err) {
      console.log(err);
      this.props.history.push({
        showErrorToast: true,
        errorToastTitle: `Failed to update offsets for ${consumerGroupId}`,
        errorToastMessage: err.response.data.title,
        loading: false
      });
      console.error('Error:', err.response);
    }
  }

  renderGroupedTopicOffset = () => {
    const { groupedTopicOffset } = this.state;
    const renderedItems = [];

    Object.keys(groupedTopicOffset).map(topidId => {
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

    offsets.map(offset => {
      const { formData } = this.state;
      const name = `offset[${topicId}][${offset.partition}]`;

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
                'partition-input ${name}-input'
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
        <Link
          className="btn btn-secondary"
          type="button"
          style={{ marginRight: '0.5rem' }}
          onClick={() => this.resetToFirstOffsets()}
        >
          Reset to first offsets
        </Link>
        <Link
          className="btn btn-secondary"
          type="button"
          style={{ marginRight: '0.5rem' }}
          onClick={() => this.resetToLastOffsets()}
        >
          Reset to last offsets
        </Link>
        <Link
          className="btn btn-secondary"
          type="button"
          style={{ marginRight: '0.5rem', padding: 0 }}
        >
          <Dropdown>
            <Dropdown.Toggle>Filter datetime</Dropdown.Toggle>
            {!loading && (
              <Dropdown.Menu>
                <div className="filter-datetime">
                  <DatePicker
                    name={'datetime-picker'}
                    value={timestamp}
                    label={''}
                    onChange={value => {
                      const momentValue = moment(value);

                      const timestamp =
                        formatDateTime(
                          {
                            year: momentValue.year(),
                            monthValue: momentValue.month() + 1,
                            dayOfMonth: momentValue.date(),
                            hour: momentValue.hour(),
                            minute: momentValue.minute(),
                            second: momentValue.second(),
                            milli: momentValue.millisecond()
                          },
                          'YYYY-MM-DDThh:mm:ss.SSS'
                        ) + 'Z';

                      this.setState({ timestamp }, () => this.getGroupedTopicOffset());
                    }}
                  />
                </div>
              </Dropdown.Menu>
            )}
          </Dropdown>
        </Link>
      </span>
    );
  };

  render() {
    const { consumerGroupId } = this.state;

    return (
      <div id="content">
        <Header title={`Update offsets: ${consumerGroupId}`} />
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
