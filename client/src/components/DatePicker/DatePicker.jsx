import React, { Component } from 'react';
import PropTypes from 'prop-types';
import DateTimePicker from 'react-datepicker';
import moment from 'moment';
import { formatDateTime } from '../../utils/converters';

class DatePicker extends Component {
  state = {
    value: '',
    openDateModal: false
  };

  componentDidMount = () => {
    this.setState({
      value: this.props.value ? this.props.value : new Date()
    });
  };

  onChange = value => {
    this.setState({ value }, () => {
      this.props.onChange && this.props.onChange(value);
    });
  };

  getDisplayValue = value => {
    let date = moment(value);
    try {
      return formatDateTime(
        {
          year: date.year(),
          monthValue: date.month(),
          dayOfMonth: date.date(),
          hour: date.hour(),
          minute: date.minute(),
          second: date.second()
        },
        'DD-MM-YYYY HH:mm'
      );
    } catch (e) {
      return '';
    }
  };

  render = () => {
    const { value } = this.state;
    const { showDateTimeInput, showTimeInput, showTimeSelect, onClear, label } = this.props;
    return (
      <div style={{ display: 'block', padding: 10 }}>
        {showDateTimeInput && (
          <div
            style={{
              marginBottom: 10,
              display: 'flex',
              flexDirection: 'row',
              alignItems: 'center'
            }}
          >
            {label && <div style={{ marginRight: 10 }}>{label}</div>}
            <input
              value={this.getDisplayValue(value)}
              className="form-control"
              readOnly={true}
              placeholder={this.getDisplayValue(value)}
            />
            {onClear && (
              <button
                className="btn btn-primary me-0"
                onClick={() => {
                  this.setState(
                    {
                      value: new Date()
                    },
                    () => {
                      onClear && onClear();
                    }
                  );
                }}
              >
                Clear
              </button>
            )}
          </div>
        )}

        <DateTimePicker
          className="date-block"
          calendarClassName={showTimeInput ? 'date-block' : ''}
          selected={value}
          onChange={date => {
            this.onChange(date);
          }}
          showTimeSelect={showTimeSelect}
          showTimeInput={showTimeInput}
          dateFormat="MM/dd/yyyy h:mm aa"
          timeIntervals={15}
          inline
        />
      </div>
    );
  };
}

DatePicker.propTypes = {
  label: PropTypes.string,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Date)]),
  onChange: PropTypes.func,
  showDateTimeInput: PropTypes.bool,
  showTimeInput: PropTypes.bool,
  showTimeSelect: PropTypes.bool,
  onClear: PropTypes.func
};

export default DatePicker;
