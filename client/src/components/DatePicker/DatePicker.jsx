import React from 'react';
import PropTypes from 'prop-types';
import Input from '../Form/Input';
import { createMuiTheme, MuiThemeProvider } from '@material-ui/core';
//import { DateTimePicker } from '@material-ui/pickers';
import DateTimePicker from 'react-datepicker';
import moment from 'moment';
import { formatDateTime } from '../../utils/converters';

const customTheme = createMuiTheme({
  overrides: {
    MuiPickersBasePicker: {
      pickerView: {
        backgroundColor: '#333333'
      }
    },
    MuiPickersToolbar: {
      toolbar: {
        backgroundColor: 'black'
      }
    },
    MuiTab: {
      textColorInherit: {
        backgroundColor: 'black',
        borderColor: 'black'
      }
    },
    MuiTabs: {
      indicator: {
        backgroundColor: '#ecbb13'
      }
    },
    MuiPickerDTTabs: {
      tabs: {
        backgroundColor: 'black'
      }
    },

    MuiTypography: {
      root: {
        color: 'white'
      },
      caption: {
        color: 'white'
      }
    },

    MuiPickersCalendarHeader: {
      daysHeader: {
        color: 'white'
      },
      dayLabel: {
        color: 'white'
      },
      iconButton: {
        backgroundColor: '#333333',
        color: 'white'
      }
    },

    MuiPickersClock: {
      pin: {
        backgroundColor: '#005f81'
      }
    },
    MuiPickersClockPointer: {
      pointer: {
        backgroundColor: '#005f81'
      },
      thumb: {
        backgroundColor: '#005f81',
        borderColor: '#005f81'
      },
      noPoint: {
        backgroundColor: 'rgba(0,95,129,0.3)',
        color: 'white'
      }
    },
    MuiPickersClockNumber: {
      clockNumber: {
        color: 'white'
      },
      clockNumberSelected: {
        backgroundColor: '#005f81',
        color: 'white'
      }
    },
    MuiButton: {
      textPrimary: {
        color: '#005f81'
      }
    },

    MuiDialogActions: {
      root: {
        backgroundColor: '#333333'
      }
    },

    MuiPickersDay: {
      day: {
        color: 'white',
        backgroundColor: '#333333'
      },
      container: {
        backgroundColor: 'black'
      },
      daySelected: {
        backgroundColor: 'black',
        color: 'white',
        hover: {
          backgroundColor: '#005f81'
        }
      },

      dayDisabled: {
        color: 'white'
      },
      current: {
        color: '',
        backgroundColor: ''
      }
    }
  }
});

class DatePicker extends React.Component {
  static propTypes = {
    value: PropTypes.string,
    onChange: PropTypes.func
  };

  state = {
    value: '',
    openDateModal: false
  };

  componentDidMount = () => {
    this.setState({
      value: this.props.value && this.props.value.length > 0 ? this.props.value : new Date()
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
    const { showDateTimeInput, showTimeInput, showTimeSelect } = this.props;
    return (
      <div style={{ display: 'block', padding: 10 }}>
        {showDateTimeInput && (
          <div style={{ marginBottom: 10 }}>
            <input
              value={this.getDisplayValue(value)}
              className="form-control"
              placeholder={this.getDisplayValue(value)}
            />
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
          inline
        />
      </div>
    );
  };
}

export default DatePicker;
