import React from 'react';
import PropTypes from 'prop-types';
import Input from '../Form/Input';
import { createMuiTheme, MuiThemeProvider } from '@material-ui/core';
import { DateTimePicker } from '@material-ui/pickers';
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
    value: moment(),
    openDateModal: false
  };

  componentDidMount = () => {
    this.setState({ value: this.props.value });
  };

  onChange = value => {
    this.setState({ value }, () => {
      this.props.onChange && this.props.onChange(value);
    });
  };

  getDisplayValue = value => {
    try {
      return formatDateTime(
        {
          year: value.year(),
          monthValue: value.month(),
          dayOfMonth: value.date(),
          hour: value.hour(),
          minute: value.minute(),
          second: value.second()
        },
        'DD-MM-YYYY HH:mm'
      );
    } catch (e) {
      return '';
    }
  };

  render = () => {
    const { value, openDateModal } = this.state;
    const { name, label, error } = this.props;
    return (
      <MuiThemeProvider theme={customTheme}>
        <DateTimePicker
          value={this.state.value}
          onChange={date => {
            this.onChange(date);
          }}
          open={openDateModal}
          onClose={() => {
            this.setState({ openDateModal: false });
          }}
          TextFieldComponent={() => (
            <Input
              name={name}
              label={label}
              error={error}
              value={this.getDisplayValue(value)}
              onClick={() => {
                this.setState({ openDateModal: true });
              }}
            />
          )}
        />
      </MuiThemeProvider>
    );
  };
}

export default DatePicker;
