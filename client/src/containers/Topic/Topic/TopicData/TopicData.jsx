import React from 'react';
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
  uriTopicsPartitions
} from '../../../../utils/endpoints';
import Pagination from '../../../../components/Pagination/Pagination';
import PageSize from '../../../../components/PageSize';
import ReactDatePicker from 'react-datepicker';
import camelCase from 'lodash/camelCase';
import constants, { SETTINGS_VALUES } from '../../../../utils/constants';
import AceEditor from '../../../../components/AceEditor/AceEditor';
import ConfirmModal from '../../../../components/Modal/ConfirmModal';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Root from '../../../../components/Root';
import DateTime from '../../../../components/DateTime';
import { getClusterUIOptions, isSameLocation } from '../../../../utils/functions';
import { setProduceToTopicValues, setUIOptions } from '../../../../utils/localstorage';
import * as LosslessJson from 'lossless-json';
import { Buffer } from 'buffer';
import { streamSearch, SearchRequestError } from '../../../../utils/sseClient';
import SearchPanel from './Search/SearchPanel';
import { emptyDraft, expressionToDraft } from './Search/searchModel';
import { readSearchFromQuery, sameExpression, writeSearchToParams } from './Search/searchUrl';
import { withRouter } from '../../../../utils/withRouter';
import { format } from 'date-fns';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faDownload } from '@fortawesome/free-solid-svg-icons';

class TopicData extends Root {
  state = {
    sortBy: 'Oldest',
    openFilter: null,
    sortOptions: ['Oldest', 'Newest'],
    partitionCount: 0,
    partition: 'All',
    partitionOptions: [],
    offsetsOptions: [],
    timestamp: '',
    endTimestamp: '',
    draft: emptyDraft(),
    applied: null,
    searchNotShareable: false,
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
    dateTimeFormat: constants.SETTINGS_VALUES.TOPIC_DATA.DATE_TIME_FORMAT.RELATIVE,
    size: 50,
    checkboxes: {},
    messagesToExport: []
  };

  searchAbort;
  searchRunId = 0;

  constructor(props) {
    super(props);
    this._handleCheckbox = this._handleCheckbox.bind(this);
  }

  componentDidMount = () => {
    this._syncStateFromLocationAndLoad();
  };

  componentDidUpdate(prevProps, prevState, snapshot) {
    const pathnameChanged = this.props.location.pathname !== prevProps.location.pathname;
    const searchChanged = this.props.location.search !== prevProps.location.search;

    if (pathnameChanged || searchChanged) {
      if (!this.props.location.pathname.endsWith('/data')) {
        return;
      }

      this._stopEventSource(false);
      this._syncStateFromLocationAndLoad();
    }
  }

  componentWillUnmount = () => {
    super.componentWillUnmount();
    this._stopEventSource();
  };

  async _syncStateFromLocationAndLoad() {
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
        ...this._searchStateFromQuery(query, prevState),
        offsets: query.get('offset')
          ? this._getOffsetsByOffset(query.get('partition'), query.get('offset'))
          : query.get('after')
            ? this._getOffsetsByAfterString(query.get('after'))
            : this.state.offsets,
        dateTimeFormat:
          uiOptions && uiOptions.topicData && uiOptions.topicData.dateTimeFormat
            ? uiOptions.topicData.dateTimeFormat
            : prevState.dateTimeFormat,
        size: query.get('size')
          ? parseInt(query.get('size'))
          : uiOptions && uiOptions.topicData && uiOptions.topicData.size
            ? uiOptions.topicData.size
            : prevState.size
      }),
      () => {
        this._loadFromCurrentLocation(query);
      }
    );
  }

  _loadFromCurrentLocation = query => {
    if (query.get('single') !== null) {
      this._getSingleMessage(query.get('partition'), query.get('offset'));
      this.setState({ canDownload: true });
    } else {
      this.setState(
        { canDownload: false, ...(this._forcesOldest() ? { sortBy: 'Oldest' } : {}) },
        () => {
          // Delegate mode selection to _searchMessages so active filters always use /data/search.
          this._searchMessages(false);
        }
      );
    }
  };

  _searchStateFromQuery(query, prevState) {
    const { applied, error } = readSearchFromQuery(query);
    if (error) {
      toast.warn(`The search in this link could not be restored: ${error}`);
    }

    // A search too long for the URL only lives in the state: keep it across our own navigations.
    if (!applied && !error && prevState.searchNotShareable && !query.get('searchQuery')) {
      return {};
    }

    return {
      applied,
      searchNotShareable: false,
      draft: sameExpression(applied, prevState.applied)
        ? prevState.draft
        : expressionToDraft(applied)
    };
  }

  _startEventSource = changePage => {
    const { selectedCluster, selectedTopic, nextPage, applied } = this.state;

    let lastPercentVal = 0.0;
    const percentUpdateDelta = 0.5;
    const results = [];
    const runId = ++this.searchRunId;
    const isCurrent = () => runId === this.searchRunId;

    this.setState(
      { messages: [], pageNumber: 1, percent: 0, isSearching: true, recordCount: 0 },
      () => {
        this.searchAbort = new AbortController();

        const onBody = data => {
          results.push(data);
          const lastResult = data;
          const percentDiff = lastResult.percent - lastPercentVal;

          // to avoid UI slowdowns, only update the percentage in fixed increments
          if (percentDiff >= percentUpdateDelta) {
            lastPercentVal = lastResult.percent;
            this.setState(prev => ({
              nextPage: lastResult.after ? lastResult.after : prev.nextPage,
              recordCount: prev.recordCount + (lastResult.records ? lastResult.records.length : 0),
              percent: lastResult.percent.toFixed(2)
            }));
          }

          const records = results
            .map(result => result.records)
            .filter(records => records?.length > 0)
            .reduce((acc, all) => [...acc, ...all], []);

          if (records.length) {
            const tableMessages = this._sortMessages(this._handleMessages(records));
            this.setState({
              recordCount: tableMessages.length,
              messages: tableMessages,
              loading: false
            });
          }
        };

        streamSearch({
          url: uriTopicDataSearch(
            selectedCluster,
            selectedTopic,
            this._buildScopeFilters(),
            changePage ? nextPage : undefined
          ),
          body: applied,
          signal: this.searchAbort.signal,
          onEvent: ({ event, data }) => {
            if (!isCurrent()) {
              return;
            }
            if (event === 'searchBody') {
              onBody(JSON.parse(data) || {});
            } else if (event === 'searchEnd') {
              const result = JSON.parse(data) || {};
              this.setState(prev => ({
                percent: 100,
                nextPage: result.after ? result.after : prev.nextPage,
                isSearching: false,
                loading: false
              }));
              this.searchAbort.abort();
            }
          }
        }).catch(error => {
          if (!isCurrent()) {
            return;
          }
          if (error instanceof SearchRequestError) {
            toast.warn(error.message);
          } else {
            toast.error('The search failed: ' + (error.message || 'unexpected error'));
          }
          this.setState({ isSearching: false, loading: false });
        });
      }
    );
  };

  _stopEventSource = (resetLoading = true) => {
    this.searchRunId++;
    if (this.searchAbort) {
      this.searchAbort.abort();
    }

    this.cancelAxiosRequests();
    this.renewCancelToken();

    this.setState({ isSearching: false, ...(resetLoading ? { loading: false } : {}) });
  };

  _clearSearch = () => {
    this.setState({ draft: emptyDraft(), applied: null, searchNotShareable: false }, () => {
      this._navigateWithCurrentFilters(false);
    });
  };

  _applySearch = expression => {
    const { notShareable } = writeSearchToParams(new URLSearchParams(), expression);
    this.setState(
      { applied: expression, searchNotShareable: notShareable, nextPage: '', sortBy: 'Oldest' },
      () => {
        this._navigateWithCurrentFilters(false);
      }
    );
  };

  // Scope filters only: these are the API query parameters; the search expression travels in the body.
  _buildScopeFilters() {
    const { sortBy, partition, datetime, endDatetime, offsetsSearch, size } = this.state;

    const filters = [];

    if (sortBy) filters.push(`sort=${sortBy}`);
    if (offsetsSearch) filters.push(`after=${offsetsSearch}`);
    if (partition) filters.push(`partition=${partition}`);
    if (size) filters.push(`size=${size}`);

    if (datetime) {
      filters.push(`timestamp=${encodeURIComponent(this._buildTimestampFilter(datetime))}`);
    }

    if (endDatetime) {
      filters.push(`endTimestamp=${encodeURIComponent(this._buildTimestampFilter(endDatetime))}`);
    }

    return filters.join('&');
  }

  // Page URL: scope filters plus the compact searchQuery (omitted when too long to share).
  _buildFilters() {
    const filters = this._buildScopeFilters();
    const { params } = writeSearchToParams(new URLSearchParams(), this.state.applied);
    const searchQuery = params.get('searchQuery');
    return searchQuery ? `${filters}&searchQuery=${searchQuery}` : filters;
  }

  async _handlePageSizeChange(newSize) {
    this.setState({ size: newSize, nextPage: '' }, () => {
      this._navigateWithCurrentFilters(false);
    });
  }

  _buildTimestampFilter(datetime) {
    if (datetime instanceof Date) {
      return formatDateTime(
        {
          year: datetime.getFullYear(),
          monthValue: datetime.getMonth(),
          dayOfMonth: datetime.getDate(),
          hour: datetime.getHours(),
          minute: datetime.getMinutes(),
          second: datetime.getSeconds(),
          milli: datetime.getMilliseconds()
        },
        "yyyy-MM-dd'T'HH:mm:ss.SSSxxx"
      );
    }
  }

  _searchMessages(changePage = false) {
    this._stopEventSource(false);
    this.setState({ loading: true });
    if (this.state.applied) {
      this._startEventSource(changePage);
    } else {
      this._getMessages(changePage);
    }
  }

  _getSingleMessage(partition, offset) {
    const { selectedCluster, selectedTopic } = this.state;

    this.setState({ loading: true });

    const requests = [
      this.getApi(uriTopicDataSingleRecord(selectedCluster, selectedTopic, partition, offset)),
      this.getApi(uriTopicsPartitions(selectedCluster, selectedTopic))
    ];

    this._fetchMessages(requests);
  }

  _getMessages = (changePage = false) => {
    const { selectedCluster, selectedTopic, nextPage } = this.state;

    this.setState({ loading: true });

    const filters = this._buildScopeFilters();
    const requests = [
      this.getApi(
        uriTopicData(selectedCluster, selectedTopic, filters, changePage ? nextPage : undefined)
      ),
      this.getApi(uriTopicsPartitions(selectedCluster, selectedTopic))
    ];

    this._fetchMessages(requests, changePage);
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

    const pathToShare = `/ui/${selectedCluster}/topic/${selectedTopic}/data?single=true&partition=${row.partition}&offset=${row.offset}`; // eslint-disable-line max-len

    try {
      this._copyToClipboard(`${window.location.host}${pathToShare}`);
      toast.info('Message url is copied to your clipboard!');
    } catch (err) {
      console.error('Failed to copy: ', err);
    }

    this.setState({ canDownload: true });
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
    const isAllSelected = e.target.checked;

    if (isAllSelected) {
      this.setState({ messagesToExport: this.state.messages }, () => {
        this.props.updateExportData(this.state.messagesToExport, true);
      });
    }

    this._updateCheckboxes(isAllSelected);
  }

  _updateCheckboxes(isAllSelected) {
    if (isAllSelected) {
      const checkboxes = {};
      this.state.messages.forEach((message, index) => {
        checkboxes[message.id] = true;
      });
      this.setState({ checkboxes }, () => {
        this.props.updateExportData(this.state.messagesToExport, true);
      });
    } else {
      this.setState(
        {
          checkboxes: {},
          messagesToExport: []
        },
        () => {
          this.props.updateExportData(this.state.messagesToExport, false);
        }
      );
    }
  }

  async _handleOnDateTimeFormatChanged(newDateTimeFormat) {
    const { clusterId } = this.props.params;
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

    const { clusterId, topicId } = this.props.params;
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

  _handleMessages = messages => {
    return messages.map(message => ({
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
    }));
  };

  // Only needed when the client accumulates records from several progressive batches (streaming
  // search): the union of already-sorted batches is not globally ordered. The paginated data endpoint
  // returns a fully-ordered page and does not go through this. Uses a proper 3-way comparator with an
  // offset tie-break so records sharing the same timestamp keep a stable, deterministic order (a simple
  // ternary never returns 0 for equal timestamps and makes the engine reverse those runs).
  _sortMessages = (messages, startWithOldest = true) => {
    const isBefore = startWithOldest ? -1 : 1;
    const isAfter = startWithOldest ? 1 : -1;

    return [...messages].sort((a, b) => {
      if (a.timestamp < b.timestamp) return isBefore;
      if (a.timestamp > b.timestamp) return isAfter;

      const partitionA = Number(a.partition);
      const partitionB = Number(b.partition);
      if (partitionA < partitionB) return isBefore;
      if (partitionA > partitionB) return isAfter;

      const offsetA = Number(a.offset);
      const offsetB = Number(b.offset);
      if (offsetA < offsetB) return isBefore;
      if (offsetA > offsetB) return isAfter;
      return 0;
    });
  };

  _downloadAllMatchingFilters = () => {
    const { selectedCluster, selectedTopic, applied } = this.state;

    const filters = this._buildScopeFilters();

    this.postApi(
      uriTopicDataDownload(selectedCluster, selectedTopic, filters),
      applied || { version: 1, groups: [] }
    ).then(response => {
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

  _navigateWithFilters(filters, replaceInNavigation = true) {
    const { selectedCluster, selectedTopic } = this.state;
    const pathname = `/ui/${selectedCluster}/topic/${selectedTopic}/data`;

    if (isSameLocation(this.props.location, pathname, filters)) {
      // Re-submitting the same filters would not change the URL: reload the data in place.
      this._stopEventSource(false);
      this._loadFromCurrentLocation(new URLSearchParams(this.props.location.search));
      return;
    }

    this.props.router.navigate(
      {
        pathname,
        search: filters
      },
      { replace: replaceInNavigation }
    );
  }

  _navigateWithCurrentFilters = (replaceInNavigation = false) => {
    this.setState({ loading: true }, () => {
      this._navigateWithFilters(this._buildFilters(), replaceInNavigation);
    });
  };

  _navigateToNextPage = (replaceInNavigation = false) => {
    const { nextPage } = this.state;

    if (!nextPage) {
      return;
    }

    this.setState({ loading: true }, () => {
      const currentParams = new URLSearchParams(this.props.location.search);

      if (nextPage.includes('?')) {
        const nextQuery = nextPage.substring(nextPage.indexOf('?') + 1);
        this._navigateWithFilters(nextQuery, replaceInNavigation);
      } else {
        // EventSource search returns token-only `after` values (e.g. 0-106_1-55).
        currentParams.set('after', nextPage);
        this._navigateWithFilters(currentParams.toString(), replaceInNavigation);
      }
    });
  };

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

  _applyOffsets = () => {
    const { offsets } = this.state;
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
      this._navigateWithCurrentFilters(false);
    });
  };

  _toggleFilter(name) {
    this.setState(prev => ({ openFilter: prev.openFilter === name ? null : name }));
  }

  _formatFilterDate(date) {
    return formatDateTime(
      {
        year: date.getFullYear(),
        monthValue: date.getMonth(),
        dayOfMonth: date.getDate(),
        hour: date.getHours(),
        minute: date.getMinutes(),
        second: date.getSeconds()
      },
      'dd-MM-yyyy HH:mm'
    );
  }

  _renderPill(name, label, summary) {
    const open = this.state.openFilter === name;
    return (
      <button
        type="button"
        className={`filter-pill${open ? ' open' : ''}`}
        aria-expanded={open}
        onClick={() => this._toggleFilter(name)}
      >
        <strong>{label}</strong> {summary}
      </button>
    );
  }

  _renderTogglePill(label, current, onToggle, disabledReason) {
    return (
      <button
        type="button"
        className="filter-pill"
        disabled={!!disabledReason}
        title={disabledReason || 'Click to switch'}
        onClick={() => {
          this.setState({ openFilter: null });
          onToggle();
        }}
      >
        <strong>{label}</strong> ({current})
      </button>
    );
  }

  // Searches and timestamp windows only scan forward, so they force the Oldest sort.
  _forcesOldest(state = this.state) {
    return !!state.applied || state.datetime !== '' || state.endDatetime !== '';
  }

  _toggleSort = () => {
    if (this._forcesOldest()) {
      return;
    }
    const { sortOptions, sortBy } = this.state;
    const next = sortOptions.find(o => o !== sortBy) || sortBy;
    this.setState({ sortBy: next }, () => this._navigateWithCurrentFilters(false));
  };

  _toggleTimeFormat = () => {
    const { DATE_TIME_FORMAT } = constants.SETTINGS_VALUES.TOPIC_DATA;
    this._handleOnDateTimeFormatChanged(
      this.state.dateTimeFormat === DATE_TIME_FORMAT.RELATIVE
        ? DATE_TIME_FORMAT.ISO
        : DATE_TIME_FORMAT.RELATIVE
    );
  };

  _renderOptionPanel(title, options, current, onSelect) {
    return (
      <div className="filter-panel">
        <strong>{title}</strong>
        {options.map(({ value, text }) => (
          <button
            key={value}
            type="button"
            aria-pressed={current === value}
            className={`btn ${current === value ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => {
              this.setState({ openFilter: null });
              if (current !== value) {
                onSelect(value);
              }
            }}
          >
            {text}
          </button>
        ))}
      </div>
    );
  }

  _renderDateFilter(label, value, onChange) {
    return (
      <ReactDatePicker
        selected={value === '' ? null : value}
        onChange={date => onChange(date || '')}
        showTimeSelect
        timeIntervals={15}
        dateFormat="dd-MM-yyyy HH:mm"
        placeholderText={label}
        isClearable
        className="form-control form-control-sm"
      />
    );
  }

  _applyTimestamps = () => {
    this.setState(
      {
        openFilter: null,
        ...(this.state.datetime !== '' || this.state.endDatetime !== '' ? { sortBy: 'Oldest' } : {})
      },
      () => this._navigateWithCurrentFilters(false)
    );
  };

  _offsetsSummary() {
    const { offsetsSearch } = this.state;
    if (!offsetsSearch) {
      return '(none)';
    }
    return offsetsSearch
      .split('_')
      .map(o => `P${o.replace('-', ':')}`)
      .join(' ');
  }

  _renderFilterPanel() {
    const { openFilter, partition, datetime, endDatetime } = this.state;

    switch (openFilter) {
      case 'partition':
        return this._renderOptionPanel(
          'Partition',
          this._createPartitionOptions().map(o => ({ value: o, text: o })),
          partition,
          option =>
            this.setState({ partition: option }, () => this._navigateWithCurrentFilters(false))
        );
      case 'timestamp':
        return (
          <div className="filter-panel">
            <span className="small">Start</span>
            {this._renderDateFilter('Start', datetime, value => this.setState({ datetime: value }))}
            <span className="small">End</span>
            {this._renderDateFilter('End', endDatetime, value =>
              this.setState({ endDatetime: value })
            )}
            <button type="button" className="btn btn-primary" onClick={this._applyTimestamps}>
              Apply
            </button>
          </div>
        );
      case 'offsets':
        return (
          <div className="filter-panel filter-offsets" data-testid="offsets-filter">
            <strong>Offsets</strong>
            <span className="small">Per-partition start offset</span>
            <table>
              <tbody>{this._renderOffsetsOptions()}</tbody>
            </table>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => {
                this.setState({ openFilter: null });
                this._applyOffsets();
              }}
            >
              Apply offsets
            </button>
          </div>
        );
      default:
        return null;
    }
  }

  _renderFilterBar() {
    const { sortBy, partition, datetime, endDatetime } = this.state;
    const timestampSummary =
      datetime === '' && endDatetime === ''
        ? '(none)'
        : `${datetime !== '' ? `From ${this._formatFilterDate(datetime)}` : ''}${
            endDatetime !== '' ? ` To ${this._formatFilterDate(endDatetime)}` : ''
          }`.trim();

    return (
      <div className="topic-filters" data-testid="topic-filters">
        <div className="filter-pills">
          {this._renderTogglePill(
            'Sort:',
            sortBy,
            this._toggleSort,
            this._forcesOldest() && 'Sorting is always Oldest when a search or timestamp is set'
          )}
          {this._renderPill('partition', 'Partition:', `(${partition})`)}
          {this._renderPill('timestamp', `Timestamp ${format(new Date(), 'z')}:`, timestampSummary)}
          {this._renderPill('offsets', 'Offsets:', this._offsetsSummary())}
          {this._renderTogglePill(
            'Time Format:',
            this.state.dateTimeFormat,
            this._toggleTimeFormat
          )}
          <button
            type="button"
            onClick={() => this.setState({ showDownloadModal: true })}
            className="btn btn-primary btn-sm ml-auto"
          >
            <FontAwesomeIcon icon={faDownload} aria-hidden={true} /> Download query result
          </button>
        </div>
        {this._renderFilterPanel()}
      </div>
    );
  }

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

  _renderSearchGroup() {
    const { draft, applied, isSearching, searchNotShareable } = this.state;

    return (
      <SearchPanel
        draft={draft}
        onDraftChange={next => this.setState({ draft: next })}
        applied={applied}
        onApply={this._applySearch}
        onClear={this._clearSearch}
        onStop={() => this._stopEventSource()}
        isSearching={!!isSearching}
        notShareable={searchNotShareable}
      />
    );
  }

  _handleSingleCheckboxChange = event => {
    const messageId = event.target.id;

    this.setState(
      prevState => {
        const newMessage = prevState.messages.find(message => message.id === messageId);

        return {
          checkboxes: {
            ...prevState.checkboxes,
            [messageId]: !prevState.checkboxes[messageId]
          },
          messagesToExport: prevState.checkboxes[messageId]
            ? prevState.messagesToExport.filter(message => message.id !== messageId)
            : [...prevState.messagesToExport, newMessage]
        };
      },
      () => {
        this.props.updateExportData(
          this.state.messagesToExport,
          this.state.messagesToExport.length > 0
        );
      }
    );
  };

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
      roles,
      size
    } = this.state;

    let actions = [constants.TABLE_SHARE, constants.TABLE_COPY];
    if (canDeleteRecords && roles.TOPIC_DATA && roles.TOPIC_DATA.includes('DELETE'))
      actions.push(constants.TABLE_DELETE);
    if (canDownload) actions.push(constants.TABLE_DOWNLOAD);

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
        <nav className="navbar navbar-expand-lg navbar-light bg-light me-auto khq-data-filter khq-sticky khq-nav">
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

          <nav className="pagination-data">
            <div>
              <PageSize
                currentPageSize={size}
                onChange={value => this._handlePageSizeChange(value)}
              />
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
                      this._navigateToNextPage(false);
                    }
                  );
                }}
                editPageNumber={false}
                showTotalPageNumber={false}
              />
            </div>
          </nav>

          <div className={`collapse navbar-collapse ${showFilters}`} id="topic-data">
            {this._renderFilterBar()}
          </div>
        </nav>
        {this._renderSearchGroup()}
        {isSearching && <ProgressBar style={{ height: '0.3rem' }} animated now={percent} />}
        <div className="table-responsive">
          <Table
            loading={loading}
            reduce={true}
            firstHeader={firstColumns}
            isChecked={messages?.length === this.state.messagesToExport?.length}
            columns={[
              {
                id: 'checkboxes',
                accessor: 'checkboxes',
                colName: 'Download all',
                type: 'checkbox',
                expand: true,
                cell: row => {
                  return (
                    <input
                      type="checkbox"
                      id={row.id}
                      checked={this.state.checkboxes[row.id] || false}
                      onChange={this._handleSingleCheckboxChange}
                    />
                  );
                }
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
                      value={value ?? 'null'}
                      readOnly
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
                        <code>{obj.value ?? 'null'}</code>
                      </pre>
                    </div>
                  );
                }
              },
              {
                id: 'timestamp',
                accessor: 'timestamp',
                colName:
                  this.state.dateTimeFormat === SETTINGS_VALUES.TOPIC_DATA.DATE_TIME_FORMAT.ISO
                    ? 'Timestamp ' + format(new Date(), 'z')
                    : 'Timestamp',
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
                        <span
                          className="badge bg-primary clickable"
                          onClick={
                            obj[col.accessor].registryType !== 'GLUE'
                              ? () => {
                                  this._redirectToSchema(obj.schema.key);
                                }
                              : undefined
                          }
                        >
                          Key: {obj[col.accessor].key}
                        </span>
                      )}

                      {obj[col.accessor].value !== undefined && (
                        <span
                          className="badge bg-primary clickable schema-value"
                          onClick={
                            obj[col.accessor].registryType !== 'GLUE'
                              ? () => {
                                  this._redirectToSchema(obj.schema.value);
                                }
                              : undefined
                          }
                        >
                          Value: {obj[col.accessor].value}
                        </span>
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
const withRouterInnerRef = WrappedComponent => {
  class InnerComponentWithRef extends React.Component {
    render() {
      const { forwardRef, ...rest } = this.props;
      return <WrappedComponent {...rest} ref={forwardRef} />;
    }
  }

  const ComponentWithRef = withRouter(InnerComponentWithRef, { withRef: true });

  return React.forwardRef((props, ref) => {
    return <ComponentWithRef {...props} forwardRef={ref} />;
  });
};

export { TopicData };
export default withRouterInnerRef(TopicData);
