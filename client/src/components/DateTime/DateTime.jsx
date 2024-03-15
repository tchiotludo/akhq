import React, { Component } from 'react';
import PropTypes from 'prop-types';

import TimeAgo from 'react-timeago';

import { SETTINGS_VALUES } from '../../utils/constants';
import { Tooltip } from '@mui/material';
import { format } from 'date-fns';

class DateTime extends Component {
  render() {
    const date = Date.parse(this.props.isoDateTimeString);
    const isoDate = format(date, 'yyyy-MM-dd HH:mm:ss.SSS');
    const TimeAgoComp = <TimeAgo date={date} title={''} />;
    return (
      <Tooltip
        arrow
        title={
          this.props.dateTimeFormat === SETTINGS_VALUES.TOPIC_DATA.DATE_TIME_FORMAT.ISO
            ? TimeAgoComp
            : isoDate
        }
        interactive={'true'}
      >
        <span>
          {this.props.dateTimeFormat === SETTINGS_VALUES.TOPIC_DATA.DATE_TIME_FORMAT.ISO
            ? isoDate
            : TimeAgoComp}
        </span>
      </Tooltip>
    );
  }
}

DateTime.propTypes = {
  isoDateTimeString: PropTypes.string.isRequired,
  dateTimeFormat: PropTypes.string.isRequired
};

export default DateTime;
