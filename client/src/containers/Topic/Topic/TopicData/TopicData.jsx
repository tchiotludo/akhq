import React from 'react';
import Dropdown from 'react-bootstrap/Dropdown';
import ProgressBar from 'react-bootstrap/ProgressBar';
import './styles.scss';
import Table from '../../../../components/Table/Table';
import { formatDateTime } from '../../../../utils/converters';
import {
  uriSchemaId,
  uriTopicData,
  uriTopicDataDelete,
  uriTopicDataSearch, uriTopicDataSingleRecord,
  uriTopicsPartitions
} from '../../../../utils/endpoints';
import Pagination from '../../../../components/Pagination/Pagination';
import moment from 'moment';
import DatePicker from '../../../../components/DatePicker';
import _ from 'lodash';
import constants from '../../../../utils/constants';
import AceEditor from 'react-ace';
import ConfirmModal from '../../../../components/Modal/ConfirmModal';

import 'ace-builds/webpack-resolver';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-dracula';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Root from '../../../../components/Root';
import { basePath } from '../../../../utils/endpoints';
import {capitalizeTxt, getClusterUIOptions} from "../../../../utils/functions";
import Select from "../../../../components/Form/Select";

class TopicData extends Root {
  state = {
    sortBy: 'Oldest',
    sortOptions: ['Oldest', 'Newest'],
    partitionCount: 0,
    partition: 'All',
    partitionOptions: [],
    offsetsOptions: [],
    timestamp: '',
    search: {
      key: { text: '', type: 'C'},
      value: { text: '', type: 'C'},
      headerKey: { text: '', type: 'C'},
      headerValue: { text: '', type: 'C'}
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
    deleteMessage: '',
    compactMessageToDelete: '',
    selectedCluster: this.props.clusterId,
    selectedTopic: this.props.topicId,
    cleanupPolicy: '',
    datetime: '',
    roles: JSON.parse(sessionStorage.getItem('roles')),
    canDeleteRecords: false,
    percent: 0,
    loading: true
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
    }];

  eventSource;

  componentDidMount = () => {
    this._checkProps();
  };

  componentWillUnmount = () => {
    super.componentWillUnmount();
    this._stopEventSource();
  };

  async _checkProps() {
    const { clusterId, topicId } = this.props.match.params;
    const query =  new URLSearchParams(this.props.location.search);
    const uiOptions = await getClusterUIOptions(clusterId);

    this.setState(
        {
          selectedCluster: clusterId,
          selectedTopic: topicId,
          sortBy: (query.get('sort'))? query.get('sort') : (uiOptions && uiOptions.topicData && uiOptions.topicData.sort)?
              uiOptions.topicData.sort : this.state.sortBy,
          partition: (query.get('partition'))? query.get('partition') : this.state.partition,
          datetime: (query.get('timestamp'))? new Date(query.get('timestamp')) : this.state.datetime,
          offsetsSearch: (query.get('after'))? query.get('after') : this.state.offsetsSearch,
          search: this._buildSearchFromQueryString(query),
          offsets: (query.get('offset'))? this._getOffsetsByOffset(query.get('partition'), query.get('offset')) :
              ((query.get('after'))? this._getOffsetsByAfterString(query.get('after')): this.state.offsets),
        },
        () => {
            if(query.get('single') !== null) {
              this._getSingleMessage(query.get('partition'), query.get('offset'));
            } else {
              this._getMessages();
            }
        }
    );
  };

  _buildSearchFromQueryString(query) {
    const { search } = this.state;

    Object.keys(search).forEach(value => {
      const searchFilter = query.get(`searchBy${capitalizeTxt(value)}`);
      if(searchFilter) {
        const pos = searchFilter.lastIndexOf('_');
        search[value].text = searchFilter.substr(0, pos);
        search[value].type = searchFilter.substr(pos + 1);
      }
    });
    return search;
  }

  _startEventSource = (changePage) => {
    let { selectedCluster, selectedTopic, nextPage } = this.state;

    let self = this;
    this.setState({ sortBy: 'Oldest', messages: [], pageNumber: 1, percent: 0, isSearching: true, recordCount: 0 }, () => {
      const filters = this._buildFilters();
      if (changePage) {
        this._setUrlHistory(filters + '&after=' + nextPage );
      } else {
        this._setUrlHistory(filters);
      }
      this.eventSource = new EventSource(uriTopicDataSearch(selectedCluster, selectedTopic, filters, (changePage)? nextPage: undefined));

      this.eventSource.addEventListener('searchBody', function(e) {
        const res = JSON.parse(e.data);
        const records = res.records || [];
        const nextPage = (res.after) ? res.after : self.state.nextPage;
        self.setState({ nextPage, recordCount: self.state.recordCount + records.length , percent: res.percent.toFixed(2) }, () => {
          self._handleMessages(records, true);
        });
      });

      this.eventSource.addEventListener('searchEnd', function(e) {
        self.eventSource.close();
        self.setState({ percent: 100, isSearching: false, loading: false });
      });
    });
  };

  _stopEventSource = () => {
    if (this.eventSource) {
      this.eventSource.close();
    }
    this.setState({ isSearching: false });
  };

  _clearSearch = () => {
    this.setState({ search: {
        key: { text: '', type: 'C'},
        value: { text: '', type: 'C'},
        headerKey: { text: '', type: 'C'},
        headerValue: { text: '', type: 'C'}
      } }, () => {
        this._searchMessages();
    });
  }

  _hasAnyFilterFilled() {
    const { search } = this.state;
    return Object.keys(search).find(value => search[value].text.length > 0) !== undefined;
  }

  _buildFilters() {
    const {
      sortBy,
      partition,
      datetime,
      offsetsSearch,
      search
    } = this.state;

    const filters = [];

    if (sortBy) filters.push(`sort=${sortBy}`);
    if (offsetsSearch) filters.push(`after=${offsetsSearch}`);
    if (partition) filters.push(`partition=${partition}`);

    if (datetime) {
      let timestamp = datetime.toString().length > 0 ? moment(datetime) : '';
      timestamp = formatDateTime(
            {
              year: timestamp.year(),
              monthValue: timestamp.month(),
              dayOfMonth: timestamp.date(),
              hour: timestamp.hour(),
              minute: timestamp.minute(),
              second: timestamp.second(),
              milli: timestamp.millisecond()
            },
            'YYYY-MM-DDTHH:mm:ss.SSS',
            true
          ) + 'Z';
      filters.push(`timestamp=${timestamp}`);
    }

    Object.keys(search).filter(value => search[value].text.length > 0)
               .forEach(value => {
                    filters.push(`searchBy${capitalizeTxt(value)}=${encodeURIComponent(search[value].text)}_${search[value].type}`)
                })
    return filters.join('&');
  }

  _searchMessages(changePage = false){
    this._stopEventSource();
    if (this._hasAnyFilterFilled()) {
      this._startEventSource(changePage);
    } else {
      this._getMessages(changePage);
    }
  }

  _getSingleMessage(partition, offset) {
    const {
      selectedCluster,
      selectedTopic,
    } = this.state;

    const requests = [this.getApi(uriTopicDataSingleRecord(selectedCluster, selectedTopic, partition, offset)),
                      this.getApi(uriTopicsPartitions(selectedCluster, selectedTopic))];

    this._fetchMessages(requests);
  }

  _getMessages(changePage = false) {
    const {
      selectedCluster,
      selectedTopic,
      nextPage
    } = this.state;

    const filters = this._buildFilters();
    const requests = [
      this.getApi(uriTopicData(selectedCluster, selectedTopic, filters, changePage ? nextPage : undefined)),
      this.getApi(uriTopicsPartitions(selectedCluster, selectedTopic))
    ];

    this._fetchMessages(requests, changePage);

    if (changePage) {
      this._setUrlHistory(nextPage.substring(nextPage.indexOf('?') + 1, nextPage.length));
    } else {
      this._setUrlHistory(filters);
    }
  }

  _fetchMessages(requests, changePage = false) {
    const {
      nextPage,
      pageNumber,
      partitionCount,
      recordCount,
      offsets
    } = this.state;

    Promise.all(requests)
      .then(data => {
        let tableMessages = [],
            pageNumberTemp, offsetsTemp, partitionCountTemp, nextPageTemp, recordCountTemp;

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
          pageNumber: (pageNumberTemp) ? pageNumberTemp : pageNumber,
          partitionCount: (partitionCountTemp) ? partitionCountTemp : partitionCount,
          nextPage: (nextPageTemp) ? nextPageTemp : nextPage,
          recordCount: (recordCountTemp) ? recordCountTemp : recordCount,
          offsets: (offsetsTemp) ? offsetsTemp : offsets,
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
    const textField = document.createElement('textarea')
    textField.innerText = code
    document.body.appendChild(textField)
    textField.select()
    document.execCommand('copy')
    textField.remove()
  }

  _handleOnShare(row) {
    const {
      selectedCluster,
      selectedTopic
    } = this.state;

    const pathToShare = `${basePath}/ui/${selectedCluster}/topic/${selectedTopic}/data?single=true&partition=${row.partition}&offset=${row.offset}`;

    try {
      this._copyToClipboard(`${window.location.host}${pathToShare}`)
      toast.info('Message url is copied to your clipboard!');
    } catch (err) {
      console.error('Failed to copy: ', err);
    }

    this.props.history.push(pathToShare)
    this._getSingleMessage(row.partition, row.offset);
  }

  _showDeleteModal = deleteMessage => {
    this.setState({ showDeleteModal: true, deleteMessage });
  };

  _closeDeleteModal = () => {
    this.setState({ showDeleteModal: false, deleteMessage: '' });
  };

  _deleteCompactMessage = () => {
    const { selectedCluster, selectedTopic, compactMessageToDelete: message } = this.state;

    const encodedkey = new Buffer(message.key).toString('base64');
    const deleteData = { partition: parseInt(message.partition), key: encodedkey };
    this.removeApi(
        uriTopicDataDelete(selectedCluster, selectedTopic, parseInt(message.partition), encodedkey),
        deleteData
    )
        .then(() => {
          toast.success(`Record '${message.partition}-${message.offset}' will be deleted on compaction`);
          this.setState({ showDeleteModal: false, compactMessageToDelete: '' }, () => {
            this._getMessages();
          });
        })
        .catch(() => {
          this.setState({ showDeleteModal: false, messageToDelete: {} });
        });
  };

  _handleMessages = (messages, append = false) => {
    let tableMessages = append ? this.state.messages : [];
    messages.forEach(message => {
      let messageToPush = {
        key: message.key || 'null',
        value: message.value || 'null',
        timestamp: message.timestamp,
        partition: JSON.stringify(message.partition) || '',
        offset: JSON.stringify(message.offset) || '',
        headers: message.headers || {},
        schema: { key: message.keySchemaId, value: message.valueSchemaId },
        exceptions: message.exceptions || []
      };
      tableMessages.push(messageToPush);
    });
    return tableMessages;
  };

  _getNextPageOffsets = (nextPage) => {
    let aux = nextPage.substring(nextPage.indexOf('after=') + 6);
    let afterString = aux.substring(0, aux.indexOf('&'));
    return this._getOffsetsByAfterString(afterString);
  };

  _getOffsetsByAfterString = (afterString) => {
    let offsets = [];
    const offsetsByPartition = afterString.split('_');

    offsetsByPartition.forEach(offsetByPartition => {
      const offset = offsetByPartition.split('-');
      offsets[`partition${offset[0]}`] = offset[1];
    });
    return offsets;
  }

  _getOffsetsByOffset = (partition, offset) => {
    let offsets = [];
    offsets[`partition${partition}`] = offset;
    return offsets;
  }

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

  _setUrlHistory(filters){
    const {
      selectedCluster,
      selectedTopic
    } = this.state;

    this.props.history.push({
      pathname: `/ui/${selectedCluster}/topic/${selectedTopic}/data`,
      search: filters
    });
  }

  _redirectToSchema(id) {
    const { selectedCluster } = this.state;

    this.getApi(uriSchemaId(selectedCluster, id))
      .then(response => {
        if (response.data) {
          this.props.history.push({
            pathname: `/ui/${selectedCluster}/schema/details/${response.data.subject}`,
            schemaId: response.data.subject
          });
        } else {
          toast.warn(`Unable to find the registry schema with id  ${id} !`);
        }
      })
  }

  _renderSortOptions() {
    const { sortOptions } = this.state;

    let renderedOptions = [];
    for (let option of sortOptions) {
      renderedOptions.push(
          <Dropdown.Item
              key={option}
              onClick={() => this.setState({ sortBy: option }, () => {
                this._clearSearch();
              })}
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
              onClick={() => this.setState({ partition: option }, () => {
                this._searchMessages();
              })}
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
      const camelcaseOption = _.camelCase(option);

      if (offsets[camelcaseOption] === undefined) {
        offsets[camelcaseOption] = '';
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
                  value={offsets[camelcaseOption]}
                  onChange={({ currentTarget: input }) => {
                    let { offsets } = this.state;
                    offsets[camelcaseOption] = input.value;
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
        {this._renderSearchFilter('value','Value')}
        {this._renderSearchFilter('headerKey', 'Header Key')}
        {this._renderSearchFilter('headerValue', 'Header Value')}

        <div style={{display: 'flex' }}>
          <button
              className="btn btn-primary inline-block search"
              type="button"
              onClick={() => this._searchMessages() }
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
    </div>);
  }

  _renderCurrentSearchText() {
    const { search } = this.state;
    const filterKey = Object.keys(search).find(value => search[value].text.length > 0);
    return (filterKey !== undefined)? search[filterKey].text : '';
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
      isSearching,
      canDeleteRecords,
      percent,
      loading
    } = this.state;
    let date = moment(datetime);
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
          <nav
              className="navbar navbar-expand-lg navbar-light bg-light
         mr-auto khq-data-filter khq-sticky khq-nav"
          >
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

            <div className={`collapse navbar-collapse ${showFilters}`} id="topic-data">
              <ul className="navbar-nav mr-auto">
                <li className="nav-item dropdown">
                  <Dropdown>
                    <Dropdown.Toggle className="nav-link dropdown-toggle">
                      <strong>Sort:</strong> ({sortBy})
                    </Dropdown.Toggle>
                    {!loading && <Dropdown.Menu>{this._renderSortOptions()}</Dropdown.Menu>}
                  </Dropdown>
                </li>
                <li className="nav-item dropdown">
                  <Dropdown>
                    <Dropdown.Toggle className="nav-link dropdown-toggle">
                      <strong>Partition:</strong> ({partition})
                    </Dropdown.Toggle>
                    {!loading && <Dropdown.Menu>
                      <div style={{ minWidth: '300px' }} className="khq-offset-navbar">
                      {this._renderPartitionOptions()}
                      </div>
                    </Dropdown.Menu>}
                  </Dropdown>
                </li>
                <li className="nav-item dropdown">
                  <Dropdown>
                    <Dropdown.Toggle className="nav-link dropdown-toggle">
                      <strong>Timestamp:</strong>
                      {(datetime !== '' &&
                      ' ' +
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
                      ))}
                    </Dropdown.Toggle>
                    {!loading && (
                        <Dropdown.Menu>
                          <div className="input-group">
                            <DatePicker
                                onClear={() => {
                                  this.setState({ datetime: '' });
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
                        </Dropdown.Menu>
                    )}
                  </Dropdown>
                </li>
                <li className="nav-item dropdown">
                  <Dropdown>
                    <Dropdown.Toggle className="nav-link dropdown-toggle">
                      <strong>Search:</strong> { this._renderCurrentSearchText() }
                    </Dropdown.Toggle>
                    {!loading && (
                        <Dropdown.Menu>
                          {this._renderSearchGroup()}
                        </Dropdown.Menu>
                    )}
                  </Dropdown>
                </li>
                <li className="nav-item dropdown">
                  <Dropdown>
                    <Dropdown.Toggle className="nav-link dropdown-toggle">
                      <strong>Offsets:</strong>
                    </Dropdown.Toggle>
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
              </ul>
            </div>
          </nav>
          {isSearching && <ProgressBar style={{ height: '0.3rem' }} animated now={percent} />}
          <div className="table-responsive">
            <Table
                loading={loading}
                history={history}
                reduce={true}
                firstHeader={firstColumns}
                columns={[
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
                        let json = JSON.parse(obj.value);
                        value = JSON.stringify(json, null, 2);
                        // eslint-disable-next-line no-empty
                      } catch (e) {}

                      return (
                          <AceEditor
                              mode="json"
                              id={'value' + index}
                              theme="merbivore_soft"
                              value={value}
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
                                    dangerouslySetInnerHTML={{ __html: obj.exceptions.join('<br /><br />')}}>
                                </div>
                            )}
                            <pre className="mb-0 khq-data-highlight">
                              <code>{obj.value}</code>
                            </pre>
                          </div>
                      );
                    }
                  },
                  {
                    id: 'timestamp',
                    accessor: 'timestamp',
                    colName: 'Date',
                    type: 'text',
                    cell: (obj, col) => {
                      return obj[col.accessor];
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
                      return <div className="tail-headers">{Object.keys(obj.headers).length}</div>;
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
                                    className="badge badge-primary clickable"
                                    onClick={() => {
                                      this._redirectToSchema(obj.schema.key);
                                    }}
                                >
                          Key: {obj[col.accessor].key}
                        </span>
                            )}

                            {obj[col.accessor].value !== undefined && (
                                <span
                                    className="badge badge-primary clickable schema-value"
                                    onClick={() => {
                                      this._redirectToSchema(obj.schema.value);
                                    }}
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
                updateData={data => {
                  this.setState({ messages: data });
                }}
                onDelete={row => {
                  this._handleOnDelete(row);
                }}
                onShare={row => {
                  this._handleOnShare(row);
                }}
                actions={canDeleteRecords ? [constants.TABLE_DELETE, constants.TABLE_SHARE] : [constants.TABLE_SHARE]}
                onExpand={obj => {
                  return Object.keys(obj.headers).map(header => {
                    return (
                        <tr
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
                            {header}
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
                            {obj.headers[header]}
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
        </React.Fragment>
    );
  }
}

export default TopicData;
