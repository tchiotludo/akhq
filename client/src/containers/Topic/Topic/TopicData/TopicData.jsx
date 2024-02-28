import React from 'react';
import Dropdown from 'react-bootstrap/Dropdown';
import ProgressBar from 'react-bootstrap/ProgressBar';
import Table from '../../../../components/Table/Table';
import { formatDateTime } from '../../../../utils/converters';
import {
  uriSchemaId,
  uriTopicData,
  uriTopicDataDelete,
  uriTopicDataDownload,
  uriTopicDataSearch,
  uriTopicDataSingleRecord,
  uriTopicsPartitions,
  basePath
} from '../../../../utils/endpoints';
import Pagination from '../../../../components/Pagination/Pagination';
import moment from 'moment';
import DatePicker from '../../../../components/DatePicker';
import camelCase from 'lodash/camelCase';
import constants from '../../../../utils/constants';
import AceEditor from 'react-ace';
import ConfirmModal from '../../../../components/Modal/ConfirmModal';

import 'ace-builds/webpack-resolver';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-dracula';
import 'ace-builds/src-noconflict/ext-searchbox';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Root from '../../../../components/Root';
import DateTime from '../../../../components/DateTime';
import { capitalizeTxt, getClusterUIOptions } from '../../../../utils/functions';
import { setProduceToTopicValues, setUIOptions } from '../../../../utils/localstorage';
import Select from '../../../../components/Form/Select';
import * as LosslessJson from 'lossless-json';
import { Buffer } from 'buffer';
import { EventSourcePolyfill } from 'event-source-polyfill';
import { withRouter } from '../../../../utils/withRouter';
import { Badge } from 'react-bootstrap';

class TopicData extends Root {
  state = {
    sortBy: 'Oldest',
    sortOptions: ['Oldest', 'Newest'],
    partitionCount: 0,
    partition: 'All',
    partitionOptions: [],
    offsetsOptions: [],
    timestamp: '',
    endTimestamp: '',
    search: {
      key: { text: '', type: 'C' },
      value: { text: '', type: 'C' },
      headerKey: { text: '', type: 'C' },
      headerValue: { text: '', type: 'C' },
      keySubject: { text: '', type: 'C' },
      valueSubject: { text: '', type: 'C' }
    },
    offsets: {},
    offsetsSearch: '',
    openDateModal: false,
    messages: [],
    pageNumber: 1,
    nextPage: '',
    recordCount: 0,
    showFilters: '',
    showDeleteModal: false,
    showDownloadModal: false,
    deleteMessage: '',
    compactMessageToDelete: '',
    selectedCluster: this.props.clusterId,
    registryType: this.props.registryType,
    selectedTopic: this.props.topicId,
    cleanupPolicy: '',
    datetime: '',
    endDatetime: '',
    roles: JSON.parse(sessionStorage.getItem('roles')),
    canDeleteRecords: false,
    percent: 0,
    loading: true,
    canDownload: false,
    dateTimeFormat: constants.SETTINGS_VALUES.TOPIC_DATA.DATE_TIME_FORMAT.RELATIVE
  };

  searchFilterTypes = [
    {
      _id: 'C',
      name: 'Contains'
    },
    {
      _id: 'N',
      name: 'Not Contains'
    },
    {
      _id: 'E',
      name: 'Equals'
    }
  ];

  eventSource;

  constructor(props) {
    super(props);
    this._handleCheckbox = this._handleCheckbox.bind(this);
  }

  componentDidMount = () => {
    this._checkProps();
  };

  componentWillUnmount = () => {
    super.componentWillUnmount();
    this._stopEventSource();
  };

  async _checkProps() {
    const { clusterId, topicId } = this.props.params;
    const query = new URLSearchParams(this.props.location.search);
    const uiOptions = await getClusterUIOptions(clusterId);
    this.setState(
      prevState => ({
        selectedCluster: clusterId,
        selectedTopic: topicId,
        registryType: this.props.registryType,
        sortBy: query.get('sort')
          ? query.get('sort')
          : uiOptions && uiOptions.topicData && uiOptions.topicData.sort
            ? uiOptions.topicData.sort
            : this.state.sortBy,
        partition: query.get('partition') ? query.get('partition') : this.state.partition,
        datetime: query.get('timestamp') ? new Date(query.get('timestamp')) : this.state.datetime,
        endDatetime: query.get('endTimestamp')
          ? new Date(query.get('endTimestamp'))
          : this.state.endDatetime,
        offsetsSearch: query.get('after') ? query.get('after') : this.state.offsetsSearch,
        search: this._buildSearchFromQueryString(query),
        offsets: query.get('offset')
          ? this._getOffsetsByOffset(query.get('partition'), query.get('offset'))
          : query.get('after')
            ? this._getOffsetsByAfterString(query.get('after'))
            : this.state.offsets,
        dateTimeFormat:
          uiOptions && uiOptions.topicData && uiOptions.topicData.dateTimeFormat
            ? uiOptions.topicData.dateTimeFormat
            : prevState.dateTimeFormat
      }),
      () => {
        if (query.get('single') !== null) {
          this._getSingleMessage(query.get('partition'), query.get('offset'));
          this.setState({ canDownload: true });
        } else {
          this._getMessages();
        }
      }
    );
  }

  _buildSearchFromQueryString(query) {
    const { search } = this.state;

    Object.keys(search).forEach(value => {
      const searchFilter = query.get(`searchBy${capitalizeTxt(value)}`);
      if (searchFilter) {
        const pos = searchFilter.lastIndexOf('_');
        search[value].text = searchFilter.substr(0, pos);
        search[value].type = searchFilter.substr(pos + 1);
      }
    });
    return search;
  }

  _startEventSource = changePage => {
    let { selectedCluster, selectedTopic, nextPage } = this.state;

    let lastPercentVal = 0.0;
    const percentUpdateDelta = 0.5;

    let self = this;
    this.setState(
      { messages: [], pageNumber: 1, percent: 0, isSearching: true, recordCount: 0 },
      () => {
        const filters = this._buildFilters();
        if (changePage) {
          this._setUrlHistory(filters + '&after=' + nextPage);
        } else {
          this._setUrlHistory(filters);
        }
        this.eventSource = new EventSourcePolyfill(
          uriTopicDataSearch(
            selectedCluster,
            selectedTopic,
            filters,
            changePage ? nextPage : undefined
          ),
          localStorage.getItem('jwtToken')
            ? {
                headers: {
                  Authorization: 'Bearer ' + localStorage.getItem('jwtToken')
                }
              }
            : {}
        );

        this.eventSource.addEventListener('searchBody', function (e) {
          const res = JSON.parse(e.data);
          const records = res.records || [];

          const percentDiff = res.percent - lastPercentVal;

          // to avoid UI slowdowns, only update the percentage in fixed increments
          if (percentDiff >= percentUpdateDelta) {
            lastPercentVal = res.percent;
            self.setState({
              recordCount: self.state.recordCount + records.length,
              percent: res.percent.toFixed(2)
            });
          }

          if (records.length) {
            const tableMessages = self._handleMessages(
              records,
              true,
              self.state.sortBy === 'Oldest'
            );
            self.setState({ messages: tableMessages });
          }
        });

        this.eventSource.addEventListener('searchEnd', function (e) {
          const res = JSON.parse(e.data);
          const nextPage = res.after ? res.after : self.state.nextPage;
          self.setState({ percent: 100, nextPage, isSearching: false, loading: false });

          self.eventSource.close();
        });
      }
    );
  };

  _stopEventSource = () => {
    if (this.eventSource) {
      this.eventSource.close();
    }
    this.setState({ isSearching: false });
  };

  _clearSearch = () => {
    this.setState(
      {
        search: {
          key: { text: '', type: 'C' },
          value: { text: '', type: 'C' },
          headerKey: { text: '', type: 'C' },
          headerValue: { text: '', type: 'C' },
          keySubject: { text: '', type: 'C' },
          valueSubject: { text: '', type: 'C' }
        }
      },
      () => {
        this._searchMessages();
      }
    );
  };

  _hasAnyFilterFilled() {
    const { search } = this.state;
    return Object.keys(search).find(value => search[value].text.length > 0) !== undefined;
  }

  _buildFilters() {
    const { sortBy, partition, datetime, endDatetime, offsetsSearch, search } = this.state;

    const filters = [];

    if (sortBy) filters.push(`sort=${sortBy}`);
    if (offsetsSearch) filters.push(`after=${offsetsSearch}`);
    if (partition) filters.push(`partition=${partition}`);

    if (datetime) {
      filters.push(`timestamp=${this._buildTimestampFilter(datetime)}`);
    }

    if (endDatetime) {
      filters.push(`endTimestamp=${this._buildTimestampFilter(endDatetime)}`);
    }

    Object.keys(search)
      .filter(value => search[value].text.length > 0)
      .forEach(value => {
        filters.push(
          `searchBy${capitalizeTxt(value)}=${encodeURIComponent(search[value].text)}_${
            search[value].type
          }`
        );
      });
    return filters.join('&');
  }

  _buildTimestampFilter(datetime) {
    let timestamp = datetime.toString().length > 0 ? moment(datetime) : '';
    return (
      formatDateTime(
        {
          year: timestamp.year(),
          monthValue: timestamp.month(),
          dayOfMonth: timestamp.date(),
          hour: timestamp.hour(),
          minute: timestamp.minute(),
          second: timestamp.second(),
          milli: timestamp.millisecond()
        },
        'YYYY-MM-DDTHH:mm:ss.SSS'
      ) + 'Z'
    );
  }

  _searchMessages(changePage = false) {
    this._stopEventSource();
    if (this._hasAnyFilterFilled()) {
      this._startEventSource(changePage);
    } else {
      this._getMessages(changePage);
    }
  }

  _getSingleMessage(partition, offset) {
    const { selectedCluster, selectedTopic } = this.state;

    const requests = [
      this.getApi(uriTopicDataSingleRecord(selectedCluster, selectedTopic, partition, offset)),
      this.getApi(uriTopicsPartitions(selectedCluster, selectedTopic))
    ];

    this._fetchMessages(requests);
  }

  _getMessages = changePage => {
    const { selectedCluster, selectedTopic, nextPage } = this.state;

    const filters = this._buildFilters();
    const requests = [
      this.getApi(
        uriTopicData(selectedCluster, selectedTopic, filters, changePage ? nextPage : undefined)
      ),
      this.getApi(uriTopicsPartitions(selectedCluster, selectedTopic))
    ];

    this._fetchMessages(requests, changePage);

    if (changePage) {
      this._setUrlHistory(nextPage.substring(nextPage.indexOf('?') + 1, nextPage.length));
    } else {
      this._setUrlHistory(filters);
    }
  };

  _fetchMessages(requests, changePage = false) {
    const { nextPage, pageNumber, partitionCount, recordCount, offsets } = this.state;

    Promise.all(requests).then(data => {
      let tableMessages = [],
        pageNumberTemp,
        offsetsTemp,
        partitionCountTemp,
        nextPageTemp,
        recordCountTemp;

      const messagesData = data[0].data;
      const partitionData = data[1].data;

      if (messagesData.results) {
        tableMessages = this._handleMessages(messagesData.results);
      } else {
        pageNumberTemp = 1;
      }
      if (partitionData) {
        if (changePage) {
          offsetsTemp = this._getNextPageOffsets(nextPage);
        }
        partitionCountTemp = partitionData.length;
        nextPageTemp = messagesData.after;
        recordCountTemp = messagesData.size;
      }
      this.setState({
        messages: tableMessages,
        canDeleteRecords: messagesData.canDeleteRecords,
        pageNumber: pageNumberTemp ? pageNumberTemp : pageNumber,
        partitionCount: partitionCountTemp ? partitionCountTemp : partitionCount,
        nextPage: nextPageTemp ? nextPageTemp : nextPage,
        recordCount: recordCountTemp ? recordCountTemp : recordCount,
        offsets: offsetsTemp ? offsetsTemp : offsets,
        loading: false
      });
    });
  }

  _handleOnDelete(message) {
    this.setState({ compactMessageToDelete: message }, () => {
      this._showDeleteModal(
        <React.Fragment>
          Do you want to delete message: {<code>{message.key}</code>} ?
        </React.Fragment>
      );
    });
  }

  _copyToClipboard(code) {
    const textField = document.createElement('textarea');
    textField.innerText = code;
    document.body.appendChild(textField);
    textField.select();
    document.execCommand('copy');
    textField.remove();
  }

  _handleOnShare(row) {
    const { selectedCluster, selectedTopic } = this.state;

    const pathToShare = `${basePath}/ui/${selectedCluster}/topic/${selectedTopic}/data?single=true&partition=${row.partition}&offset=${row.offset}`; // eslint-disable-line max-len

    try {
      this._copyToClipboard(`${window.location.host}${pathToShare}`);
      toast.info('Message url is copied to your clipboard!');
    } catch (err) {
      console.error('Failed to copy: ', err);
    }

    this.setState({ canDownload: true });

    this.props.history.push(pathToShare);
    this._getSingleMessage(row.partition, row.offset);
  }

  _handleDownload({ key, value: data }) {
    const hasKey = key && key !== null && key !== 'null';

    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([data], { type: 'text/json' }));
    a.download = `${hasKey ? key : 'file'}.json`;

    a.click();
    a.remove();
  }

  _handleCheckbox(e) {
    this.props.onSelectAllCheckboxChange(e.target.checked, this.state.messages);
  }

  async _handleOnDateTimeFormatChanged(newDateTimeFormat) {
    const { clusterId } = this.props.match.params;
    this.setState({
      dateTimeFormat: newDateTimeFormat
    });
    const currentUiOptions = await getClusterUIOptions(clusterId);
    const newUiOptions = {
      ...currentUiOptions,
      topicData: {
        ...currentUiOptions.topicData,
        dateTimeFormat: newDateTimeFormat
      }
    };
    setUIOptions(clusterId, newUiOptions);
  }

  _handleCopy(row) {
    const data = {
      partition: row.partition,
      key: row.key,
      headers: row.headers,
      keySchema: row.schema.key,
      valueSchema: row.schema.value,
      value: row.value
    };
    setProduceToTopicValues(data);

    const { clusterId, topicId } = this.props.match.params;
    this.props.router.navigate(`/ui/${clusterId}/topic/${topicId}/produce`);
  }

  _showDeleteModal = deleteMessage => {
    this.setState({ showDeleteModal: true, deleteMessage });
  };

  _closeDeleteModal = () => {
    this.setState({ showDeleteModal: false, deleteMessage: '' });
  };

  _deleteCompactMessage = () => {
    const { selectedCluster, selectedTopic, compactMessageToDelete: message } = this.state;

    const encodedkey = Buffer.from(message.key).toString('base64');
    const deleteData = { partition: parseInt(message.partition), key: encodedkey };
    this.removeApi(
      uriTopicDataDelete(selectedCluster, selectedTopic, parseInt(message.partition), encodedkey),
      deleteData
    )
      .then(() => {
        toast.success(
          `Record '${message.partition}-${message.offset}' will be deleted on compaction`
        );
        this.setState({ showDeleteModal: false, compactMessageToDelete: '' }, () => {
          this._getMessages();
        });
      })
      .catch(() => {
        this.setState({ showDeleteModal: false, messageToDelete: {} });
      });
  };

  _handleMessages = (messages, append = false, insertAtEnd = true) => {
    let tableMessages = append ? [...this.state.messages] : [];
    messages.forEach(message => {
      let messageToPush = {
        key: message.key || '',
        value: message.truncated
          ? message.value + '...\nToo large message. Full body in share button.' || ''
          : message.value || '',
        timestamp: message.timestamp,
        partition: JSON.stringify(message.partition) || '',
        offset: JSON.stringify(message.offset) || '',
        headers: message.headers || [],
        schema: {
          key: message.keySchemaId,
          value: message.valueSchemaId,
          registryType: this.state.registryType
        },
        exceptions: message.exceptions || []
      };

      if (insertAtEnd) {
        tableMessages.push(messageToPush);
      } else {
        tableMessages.unshift(messageToPush);
      }
    });
    return tableMessages;
  };

  _downloadAllMatchingFilters = () => {
    const { selectedCluster, selectedTopic } = this.state;

    const filters = this._buildFilters();

    this.getApi(uriTopicDataDownload(selectedCluster, selectedTopic, filters)).then(response => {
      const a = document.createElement('a');
      const type = 'application/json';
      a.href = URL.createObjectURL(
        new Blob([JSON.stringify(response.data, null, 2)], { type: type, endings: 'native' })
      );
      a.download = `${selectedTopic}.json`;

      a.click();
      a.remove();

      this.setState({
        showDownloadModal: false
      });
    });
  };

  _getNextPageOffsets = nextPage => {
    let aux = nextPage.substring(nextPage.indexOf('after=') + 6);
    let afterString = aux.substring(0, aux.indexOf('&'));
    return this._getOffsetsByAfterString(afterString);
  };

  _getOffsetsByAfterString = afterString => {
    let offsets = [];
    const offsetsByPartition = afterString.split('_');

    offsetsByPartition.forEach(offsetByPartition => {
      const offset = offsetByPartition.split('-');
      offsets[`partition${offset[0]}`] = offset[1];
    });
    return offsets;
  };

  _getOffsetsByOffset = (partition, offset) => {
    let offsets = [];
    offsets[`partition${partition}`] = offset;
    return offsets;
  };

  _createPartitionOptions = () => {
    const { partitionCount } = this.state;
    let partitionOptions = ['All'];
    for (let i = 0; i < partitionCount; i++) {
      partitionOptions.push(`${i}`);
    }
    return partitionOptions;
  };

  _createOffsetsOptions = () => {
    const { partitionCount } = this.state;
    let offsetsOptions = [];
    for (let i = 0; i < partitionCount; i++) {
      offsetsOptions.push(`Partition ${i}`);
    }
    return offsetsOptions;
  };

  _setUrlHistory(filters) {
    const { selectedCluster, selectedTopic } = this.state;

    this.props.router.navigate({
      pathname: `/ui/${selectedCluster}/topic/${selectedTopic}/data`,
      search: filters
    });
  }

  _redirectToSchema(id) {
    const { selectedCluster, selectedTopic } = this.state;

    this.getApi(uriSchemaId(selectedCluster, id, selectedTopic)).then(response => {
      if (response.data) {
        this.props.router.navigate({
          pathname: `/ui/${selectedCluster}/schema/details/${response.data.subject}`,
          schemaId: response.data.subject
        });
      } else {
        toast.warn(`Unable to find the registry schema with id  ${id} !`);
      }
    });
  }

  _renderSortOptions() {
    const { sortOptions } = this.state;

    let renderedOptions = [];
    for (let option of sortOptions) {
      renderedOptions.push(
        <Dropdown.Item
          key={option}
          onClick={() =>
            this.setState({ sortBy: option }, () => {
              this._searchMessages();
            })
          }
        >
          <i className="fa fa-fw fa-sort-numeric-desc pull-left" aria-hidden="true" /> {option}
        </Dropdown.Item>
      );
    }
    return renderedOptions;
  }

  _renderPartitionOptions = () => {
    const partitionOptions = this._createPartitionOptions();

    let renderedOptions = [];
    for (let option of partitionOptions) {
      renderedOptions.push(
        <Dropdown.Item
          key={option}
          onClick={() =>
            this.setState({ partition: option }, () => {
              this._searchMessages();
            })
          }
        >
          <i className="fa fa-fw pull-left" aria-hidden="true" /> {option}
        </Dropdown.Item>
      );
    }
    return renderedOptions;
  };

  _renderOffsetsOptions = () => {
    const offsetsOptions = this._createOffsetsOptions();

    let renderedOptions = [];
    let i;
    let offsets = this.state.offsets;
    for (i = 0; i < offsetsOptions.length; i++) {
      const option = offsetsOptions[i];
      const camelCaseOption = camelCase(option);

      if (offsets[camelCaseOption] === undefined) {
        offsets[camelCaseOption] = '';
        this.setState({ offsets });
      }

      renderedOptions.push(
        <tr key={option}>
          <td className="offset-navbar-partition-label offset-navbar-partition-td">{option} : </td>
          <td className="offset-navbar-partition-td">
            <input
              className="form-control"
              type="number"
              min="0"
              name={`${i}`}
              value={offsets[camelCaseOption]}
              onChange={({ currentTarget: input }) => {
                let { offsets } = this.state;
                offsets[camelCaseOption] = input.value;
                this.setState(offsets);
              }}
            />
          </td>
        </tr>
      );
    }
    return renderedOptions;
  };

  _openAndCloseFilters() {
    let { showFilters } = this.state;
    if (showFilters === 'show') {
      this.setState({ showFilters: '' });
    } else {
      this.setState({ showFilters: 'show' });
    }
  }

  _renderSearchFilter(name, label) {
    const { search } = this.state;

    return (
      <div className="search-input-fields">
        <label>{label}</label>
        <input
          className="form-control"
          name={`${name}_text`}
          type="text"
          value={search[name].text}
          onChange={({ currentTarget: input }) => {
            search[name].text = input.value;
            this.setState({ search });
          }}
        />
        <Select
          name={`${name}_select`}
          selectClass={'col-xs-2'}
          value={search[name].type}
          label=""
          items={this.searchFilterTypes}
          onChange={value => {
            search[name].type = value.target.value;
            this.setState({ search });
          }}
        />
      </div>
    );
  }

  _renderSearchGroup() {
    const { isSearching } = this.state;

    return (
      <div style={{ minWidth: '400px' }} className="input-group">
        {this._renderSearchFilter('key', 'Key')}
        {this._renderSearchFilter('value', 'Value')}
        {this._renderSearchFilter('headerKey', 'Header Key')}
        {this._renderSearchFilter('headerValue', 'Header Value')}
        {this._renderSearchFilter('keySubject', 'Key Subject')}
        {this._renderSearchFilter('valueSubject', 'Value Subject')}

        <p style={{ fontStyle: 'italic' }}>
          * Whitespaces in search string are considered as separators for search patterns unless
          enclosed with double quotes
          <br />
          In case of multiple patterns, OR operator is applied for Contains / Equals. AND is applied
          for Not contains
        </p>

        <div style={{ display: 'flex' }}>
          <button
            className="btn btn-primary inline-block search"
            type="button"
            onClick={() => this._searchMessages()}
          >
            {isSearching ? (
              <i className="fa fa-spinner fa-spin"></i>
            ) : (
              <i className="fa fa-search"></i>
            )}
          </button>
          <button
            className="btn btn-primary btn-border inline-block"
            type="button"
            disabled={!isSearching}
            onClick={() => this._stopEventSource()}
          >
            Stop
          </button>
          <button
            className="btn btn-primary btn-border inline-block"
            type="button"
            disabled={isSearching}
            onClick={() => this._clearSearch()}
          >
            Clear
          </button>
        </div>
      </div>
    );
  }

  _renderCurrentSearchText() {
    const { search } = this.state;
    const filterKey = Object.keys(search).find(value => search[value].text.length > 0);
    return filterKey !== undefined ? search[filterKey].text : '';
  }

  render() {
    const {
      sortBy,
      partition,
      offsets,
      messages,
      pageNumber,
      recordCount,
      showFilters,
      datetime,
      endDatetime,
      isSearching,
      canDeleteRecords,
      canDownload,
      percent,
      loading,
      roles
    } = this.state;

    let actions = [constants.TABLE_SHARE, constants.TABLE_COPY];
    if (canDeleteRecords && roles.TOPIC_DATA && roles.TOPIC_DATA.includes('DELETE'))
      actions.push(constants.TABLE_DELETE);
    if (canDownload) actions.push(constants.TABLE_DOWNLOAD);

    let date = moment(datetime);
    let endDate = moment(endDatetime);
    const { history } = this.props;
    const firstColumns = [
      { colName: 'Key', colSpan: 1 },
      { colName: 'Value', colSpan: 1 },
      { colName: 'Date', colSpan: 1 },
      { colName: 'Partition', colSpan: 1 },
      { colName: 'Offset', colSpan: 1 },
      { colName: 'Headers', colSpan: 1 },
      { colName: 'Schema', colSpan: 1 }
    ];
    return (
      <React.Fragment>
        <nav className="navbar navbar-expand-lg mr-auto khq-data-filter khq-sticky khq-nav">
          <button
            className="navbar-toggler"
            type="button"
            data-toggle="collapse"
            data-target="#topic-data"
            aria-controls="topic-data"
            aria-expanded="false"
            aria-label="Toggle navigation"
            onClick={() => {
              this._openAndCloseFilters();
            }}
          >
            <span className="navbar-toggler-icon" />
          </button>

          <div className={`collapse navbar-collapse ${showFilters}`} id="topic-data">
            <ul className="navbar-nav mr-auto">
              <li className="nav-item dropdown">
                <Dropdown>
                  <Dropdown.Toggle className="nav-link dropdown-toggle">
                    Sort: ({sortBy})
                  </Dropdown.Toggle>
                  {!loading && <Dropdown.Menu>{this._renderSortOptions()}</Dropdown.Menu>}
                </Dropdown>
              </li>
              <li className="nav-item dropdown">
                <Dropdown>
                  <Dropdown.Toggle className="nav-link dropdown-toggle">
                    Partition: ({partition})
                  </Dropdown.Toggle>
                  {!loading && (
                    <Dropdown.Menu>
                      <div style={{ minWidth: '300px' }} className="khq-offset-navbar">
                        {this._renderPartitionOptions()}
                      </div>
                    </Dropdown.Menu>
                  )}
                </Dropdown>
              </li>
              <li className="nav-item dropdown">
                <Dropdown>
                  <Dropdown.Toggle className="nav-link dropdown-toggle">
                    Timestamp UTC:
                    {datetime !== '' &&
                      ' From ' +
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
                        )}
                    {endDatetime !== '' &&
                      ' To ' +
                        formatDateTime(
                          {
                            year: endDate.year(),
                            monthValue: endDate.month(),
                            dayOfMonth: endDate.date(),
                            hour: endDate.hour(),
                            minute: endDate.minute(),
                            second: endDate.second()
                          },
                          'DD-MM-YYYY HH:mm'
                        )}
                  </Dropdown.Toggle>
                  {!loading && (
                    <Dropdown.Menu>
                      <div style={{ display: 'flex' }}>
                        <div>
                          <DatePicker
                            label="Start"
                            onClear={() => {
                              this.setState({ datetime: '' }, () => {
                                this._searchMessages();
                              });
                            }}
                            showDateTimeInput
                            showTimeSelect
                            value={datetime}
                            onChange={value => {
                              this.setState({ datetime: value }, () => {
                                this._searchMessages();
                              });
                            }}
                          />
                        </div>
                        <div>
                          <DatePicker
                            label="End"
                            onClear={() => {
                              this.setState({ endDatetime: '' }, () => {
                                this._searchMessages();
                              });
                            }}
                            showDateTimeInput
                            showTimeSelect
                            value={endDatetime}
                            onChange={value => {
                              this.setState({ endDatetime: value }, () => {
                                this._searchMessages();
                              });
                            }}
                          />
                        </div>
                      </div>
                    </Dropdown.Menu>
                  )}
                </Dropdown>
              </li>
              <li className="nav-item dropdown">
                <Dropdown>
                  <Dropdown.Toggle className="nav-link dropdown-toggle">
                    Search: {this._renderCurrentSearchText()}
                  </Dropdown.Toggle>
                  {!loading && <Dropdown.Menu>{this._renderSearchGroup()}</Dropdown.Menu>}
                </Dropdown>
              </li>
              <li className="nav-item dropdown">
                <Dropdown>
                  <Dropdown.Toggle className="nav-link dropdown-toggle">Offsets:</Dropdown.Toggle>
                  {!loading && (
                    <Dropdown.Menu>
                      <div style={{ minWidth: '300px' }} className="khq-offset-navbar">
                        <div className="input-group">
                          <table>{this._renderOffsetsOptions()}</table>
                          <div className="input-group-append">
                            <button
                              className="btn btn-primary offsets-ok"
                              type="button"
                              onClick={() => {
                                let offsetsSearch = '';
                                for (let i = 0; i < Object.keys(offsets).length; i++) {
                                  if (Object.values(offsets)[i] !== '') {
                                    if (offsetsSearch !== '') {
                                      offsetsSearch += '_';
                                    }
                                    offsetsSearch += `${i}-${Object.values(offsets)[i]}`;
                                  }
                                }
                                this.setState({ offsetsSearch }, () => {
                                  this._searchMessages();
                                });
                              }}
                            >
                              OK
                            </button>
                          </div>
                        </div>
                      </div>
                    </Dropdown.Menu>
                  )}
                </Dropdown>
              </li>
              <li>
                <Dropdown>
                  <Dropdown.Toggle className="nav-link dropdown-toggle">
                    Time Format: ({this.state.dateTimeFormat})
                  </Dropdown.Toggle>
                  <Dropdown.Menu>
                    <Dropdown.Item
                      onClick={() =>
                        this._handleOnDateTimeFormatChanged(
                          constants.SETTINGS_VALUES.TOPIC_DATA.DATE_TIME_FORMAT.RELATIVE
                        )
                      }
                    >
                      Show relative time
                    </Dropdown.Item>
                    <Dropdown.Item
                      onClick={() =>
                        this._handleOnDateTimeFormatChanged(
                          constants.SETTINGS_VALUES.TOPIC_DATA.DATE_TIME_FORMAT.ISO
                        )
                      }
                    >
                      Show ISO timestamp
                    </Dropdown.Item>
                  </Dropdown.Menu>
                </Dropdown>
              </li>
              <div
                style={{ borderLeft: '1px solid ', padding: 0, margin: '0 10px' }}
                className="nav-link"
              ></div>
              <li>
                <button
                  type="button"
                  aria-label="Toggle navigation"
                  onClick={() => {
                    this.setState({
                      showDownloadModal: true
                    });
                  }}
                  className="nav-link"
                  style={{ backgroundColor: 'transparent', borderColor: 'transparent' }}
                >
                  <i className="fa fa-fw fa fa-download" aria-hidden="true" />
                  Download query result
                </button>
              </li>
            </ul>
          </div>

          <nav className="pagination-data">
            <div>
              <Pagination
                pageNumber={pageNumber}
                totalRecords={recordCount}
                totalPageNumber={messages.length === 0 ? pageNumber : undefined}
                onChange={({ currentTarget: input }) => {
                  this.setState({ pageNumber: input.value });
                }}
                onSubmit={() => {
                  this.setState(
                    {
                      pageNumber: pageNumber + 1
                    },
                    () => {
                      this._searchMessages(true);
                    }
                  );
                }}
                editPageNumber={false}
                showTotalPageNumber={false}
              />
            </div>
          </nav>
        </nav>
        {isSearching && <ProgressBar style={{ height: '0.3rem' }} animated now={percent} />}
        <div className="table-responsive">
          <Table
            loading={loading}
            history={history}
            reduce={true}
            firstHeader={firstColumns}
            isChecked={this.props.isAllTopicDataSelected}
            columns={[
              {
                id: 'checkboxes',
                accessor: 'checkboxes',
                colName: 'Download all',
                type: 'checkbox',
                expand: true,
                cell: () => {
                  return (
                    <input type="checkbox" checked={this.props.isAllTopicDataSelected} readOnly />
                  );
                },
                readOnly: true
              },
              {
                id: 'key',
                accessor: 'key',
                colName: 'Key',
                type: 'text',
                cell: (obj, col) => {
                  let value = obj[col.accessor] === '' ? 'null' : obj[col.accessor];
                  return (
                    <span>
                      <code className="key">{value}</code>
                    </span>
                  );
                }
              },
              {
                id: 'value',
                accessor: 'value',
                colName: 'Value',
                type: 'text',
                extraRow: true,
                extraRowContent: (obj, index) => {
                  let value = obj.value;
                  try {
                    let json = LosslessJson.parse(obj.value);
                    value = LosslessJson.stringify(json, undefined, '  ');
                    // eslint-disable-next-line no-empty
                  } catch (e) {}

                  return (
                    <AceEditor
                      mode="json"
                      id={'value' + index}
                      theme="merbivore_soft"
                      value={value || 'null'}
                      readOnly
                      name="UNIQUE_ID_OF_DIV"
                      editorProps={{ $blockScrolling: true }}
                      style={{ width: '100%', minHeight: '25vh' }}
                    />
                  );
                },
                cell: obj => {
                  return (
                    <div>
                      {obj.exceptions.length > 0 && (
                        <div
                          className="alert alert-warning"
                          role="alert"
                          dangerouslySetInnerHTML={{ __html: obj.exceptions.join('<br /><br />') }}
                        ></div>
                      )}
                      <pre className="mb-0 khq-data-highlight">
                        <code>{obj.value || 'null'}</code>
                      </pre>
                    </div>
                  );
                }
              },
              {
                id: 'timestamp',
                accessor: 'timestamp',
                colName: 'Timestamp',
                type: 'text',
                cell: (obj, col) => {
                  return (
                    <DateTime
                      isoDateTimeString={obj[col.accessor]}
                      dateTimeFormat={this.state.dateTimeFormat}
                    />
                  );
                }
              },
              {
                id: 'partition',
                accessor: 'partition',
                colName: 'Partition',
                type: 'text',
                cell: (obj, col) => {
                  return obj[col.accessor];
                }
              },
              {
                id: 'offset',
                accessor: 'offset',
                colName: 'Offset',
                type: 'text',
                cell: (obj, col) => {
                  return obj[col.accessor];
                }
              },
              {
                id: 'headers',
                accessor: 'headers',
                colName: 'Headers',
                type: 'text',
                expand: true,
                cell: obj => {
                  return <div className="tail-headers">{obj.headers.length}</div>;
                }
              },
              {
                id: 'schema',
                accessor: 'schema',
                colName: 'Schema',
                type: 'text',
                cell: (obj, col) => {
                  return (
                    <div className="justify-items">
                      {obj[col.accessor].key !== undefined && (
                        <Badge
                          bg="primary"
                          onClick={
                            obj[col.accessor].registryType !== 'GLUE'
                              ? () => {
                                  this._redirectToSchema(obj.schema.key);
                                }
                              : undefined
                          }
                        >
                          Key: {obj[col.accessor].key}
                        </Badge>
                      )}

                      {obj[col.accessor].value !== undefined && (
                        <Badge
                          bg="primary"
                          onClick={
                            obj[col.accessor].registryType !== 'GLUE'
                              ? () => {
                                  this._redirectToSchema(obj.schema.value);
                                }
                              : undefined
                          }
                        >
                          Value: {obj[col.accessor].value}
                        </Badge>
                      )}
                    </div>
                  );
                }
              }
            ]}
            extraRow
            noStripes
            data={messages}
            rowId={data => {
              return data.partition + '-' + data.offset;
            }}
            updateData={data => {
              this.setState({ messages: data });
            }}
            updateCheckbox={e => {
              this._handleCheckbox(e);
            }}
            onDelete={row => {
              this._handleOnDelete(row);
            }}
            onShare={row => {
              this._handleOnShare(row);
            }}
            onDownload={row => {
              this._handleDownload(row);
            }}
            onCopy={row => {
              this._handleCopy(row);
            }}
            actions={actions}
            onExpand={obj => {
              return obj.headers.map((header, i) => {
                return (
                  <tr
                    key={i}
                    className={'table-sm'}
                    style={{
                      display: 'flex',
                      flexDirection: 'row',
                      width: '100%'
                    }}
                  >
                    <td
                      style={{
                        width: '100%',
                        display: 'flex',
                        borderStyle: 'dashed',
                        borderWidth: '1px',
                        backgroundColor: '#171819'
                      }}
                    >
                      {header.key}
                    </td>
                    <td
                      style={{
                        width: '100%',
                        display: 'flex',
                        borderStyle: 'dashed',
                        borderWidth: '1px',
                        backgroundColor: '#171819'
                      }}
                    >
                      {header.value}
                    </td>
                  </tr>
                );
              });
            }}
          />
        </div>

        <ConfirmModal
          show={this.state.showDeleteModal}
          handleCancel={this._closeDeleteModal}
          handleConfirm={this._deleteCompactMessage}
          message={this.state.deleteMessage}
        />

        <ConfirmModal
          show={this.state.showDownloadModal}
          handleCancel={() =>
            this.setState({
              showDownloadModal: false
            })
          }
          handleConfirm={this._downloadAllMatchingFilters}
          message="Do you want to download all the messages matching your filters ? If you did not set any, it will
            download all the topic messages. For large topics, it can be slow and lead to high memory consumption."
        />
      </React.Fragment>
    );
  }
}

export default withRouter(TopicData);
