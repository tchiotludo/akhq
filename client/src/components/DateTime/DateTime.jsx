import React, { Component } from 'react';
import PropTypes from 'prop-types';

import TimeAgo from 'react-timeago';
import { Tooltip } from '@material-ui/core';

import { SETTINGS_VALUES } from '../../utils/constants';

class DateTime extends Component {
  render() {
    const isoDate = this.props.isoDateTimeString;
    const TimeAgoComp = <TimeAgo date={Date.parse(isoDate)} title={''} />;
    return (
      <Tooltip
        arrow
        title={
          this.props.dateTimeFormat === SETTINGS_VALUES.TOPIC_DATA.DATE_TIME_FORMAT.ISO
            ? TimeAgoComp
            : isoDate
        }
        interactive
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
