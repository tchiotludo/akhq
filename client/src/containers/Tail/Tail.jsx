import React from 'react';
import Dropdown from 'react-bootstrap/Dropdown';
import remove from 'lodash/remove';
import Input from '../../components/Form/Input';
import Header from '../Header';
import { SETTINGS_VALUES } from '../../utils/constants';
import { getClusterUIOptions } from '../../utils/functions';
import { uriLiveTail, uriTopicsName } from '../../utils/endpoints';
import Table from '../../components/Table';
import AceEditor from 'react-ace';
import 'ace-builds/webpack-resolver';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-merbivore_soft';
import Root from '../../components/Root';
import DateTime from '../../components/DateTime';
import { EventSourcePolyfill } from 'event-source-polyfill';
import * as LosslessJson from 'lossless-json';

const STATUS = {
  STOPPED: 'STOPPED',
  STARTED: 'STARTED',
  PAUSED: 'PAUSED'
};

const MAX_RECORDS = [50, 100, 250, 500, 1000, 2500];

class Tail extends Root {
  state = {
    search: '',
    dropdownSearch: '',
    topics: [],
    showDropdown: false,
    selectedTopics: [],
    selectedStatus: 'STOPPED',
    maxRecords: 50,
    data: [],
    showFilters: '',
    dateTimeFormat: SETTINGS_VALUES.TOPIC_DATA.DATE_TIME_FORMAT.RELATIVE
  };
  eventSource;

  componentDidMount = async () => {
    const { clusterId } = this.props.match.params;
    const query = new URLSearchParams(this.props.location.search);

    let data = await this.getApi(uriTopicsName(clusterId));
    data = data.data;
    let topics = [];
    if (query && query.get('topicId')) {
      topics = [query.get('topicId')];
    }

    if (data) {
      this.setState({ topics: data, selectedTopics: topics }, () => {
        if (query && query.get('topicId')) {
          this.setState({ selectedStatus: STATUS.STARTED });
          this.startEventSource();
        }
      });
    } else {
      this.setState({ topics: [], selectedTopics: topics }, () => {
        if (query && query.get('topicId')) {
          this.setState({ selectedStatus: STATUS.STARTED });
          this.startEventSource();
        }
      });
    }

    this.initDateTimeFormat();
  };

  initDateTimeFormat = async () => {
    const { clusterId } = this.props.match.params;
    const uiOptions = await getClusterUIOptions(clusterId);
    if (uiOptions.topicData && uiOptions.topicData.dateTimeFormat) {
      this.setState({
        dateTimeFormat: uiOptions.topicData.dateTimeFormat
      });
    }
  };

  componentWillUnmount = () => {
    super.componentWillUnmount();
    this.onStop();
  };

  startEventSource = () => {
    const { clusterId } = this.props.match.params;
    const { search, selectedTopics, maxRecords } = this.state;
    this.eventSource = new EventSourcePolyfill(
      uriLiveTail(clusterId, search, selectedTopics, JSON.stringify(maxRecords)),
      localStorage.getItem('jwtToken')
        ? {
            headers: {
              Authorization: 'Bearer ' + localStorage.getItem('jwtToken')
            }
          }
        : {}
    );
    let self = this;
    this.eventSource.addEventListener('tailBody', function (e) {
      let res = JSON.parse(e.data);
      let { data } = self.state;

      if (res.records) {
        data = data.concat(res.records);
        if (data.length > maxRecords) {
          data = data.slice(data.length - maxRecords);
        }
      }

      self.setState({ data: data });
      self.scrollToBottom();
    });

    this.eventSource.onerror = () => {
      this.setState({ selectedStatus: STATUS.STOPPED });
    };
  };

  onStop = () => {
    if (this.eventSource) this.eventSource.close();
  };

  onStart = () => {
    this.startEventSource();
  };

  scrollToBottom = () => {
    const followScroll =
      (document.body.scrollTop || document.documentElement.scrollTop) + window.innerHeight >=
      document.documentElement.scrollHeight - document.documentElement.clientHeight;

    if (followScroll) {
      this.messagesEnd.scrollIntoView({ behavior: 'smooth' });
    }
  };

  handleChange = e => {
    this.setState({ [e.target.name]: [e.target.value] });
  };

  handleSelectedTopics = topic => {
    let selectedTopics = this.state.selectedTopics;
    if (selectedTopics.find(el => el === topic)) {
      let updatedSelected = remove(selectedTopics, el => el !== topic);

      this.setState({ selectedTopics: updatedSelected });
    } else {
      selectedTopics.push(topic);
      this.setState({ selectedTopics: selectedTopics });
    }
  };

  renderTopicList = () => {
    let { topics, dropdownSearch, selectedTopics } = this.state;

    return (
      <div style={{ maxHeight: '678px', overflowY: 'auto', minHeight: '89px' }}>
        <ul
          className="dropdown-menu inner show"
          role="presentation"
          style={{ marginTop: '0px', marginBottom: '0px' }}
        >
          {topics
            .filter(topic => {
              if (dropdownSearch.length > 0) {
                return topic.includes(dropdownSearch);
              }
              return topic;
            })
            .map((topic, index) => {
              let selected = selectedTopics.find(selected => selected === topic);
              return (
                <li key={`topic_${topic}_${index}`}>
                  <div
                    onClick={() => {
                      this.onStop();
                      this.setState({ data: [] });
                      this.handleSelectedTopics(topic);
                    }}
                    role="option"
                    className={`dropdown-item ${selected ? 'selected' : ''}`}
                    id={`bs-select-${index}-0`}
                    aria-selected="false"
                  >
                    <span className="text">{topic}</span>
                  </div>
                </li>
              );
            })}
        </ul>{' '}
      </div>
    );
  };

  openAndCloseFilters() {
    let { showFilters } = this.state;
    if (showFilters === 'show') {
      this.setState({ showFilters: '' });
    } else {
      this.setState({ showFilters: 'show' });
    }
  }

  render() {
    const {
      search,
      dropdownSearch,
      selectedTopics,
      topics,
      selectedStatus,
      maxRecords,
      data,
      showFilters
    } = this.state;

    return (
      <div>
        <Header title="Live Tail" history={this.props.history} />
        <nav
          className="navbar navbar-expand-lg navbar-light
        bg-light mr-auto khq-data-filter khq-sticky khq-nav"
        >
          <button
            className="navbar-toggler"
            type="button"
            data-toggle="collapse"
            data-target="#navbar-search"
            aria-controls="navbar-search"
            aria-expanded="false"
            aria-label="Toggle navigation"
            onClick={() => {
              this.openAndCloseFilters();
            }}
          >
            <span className="navbar-toggler-icon" />
          </button>
          <div className={`form-inline collapse navbar-collapse ${showFilters}`} id="navbar-search">
            <Input
              type="text"
              name="search"
              id="search"
              value={search}
              label={''}
              placeholder={'Search...'}
              onChange={e => {
                this.onStop();
                this.setState({ data: [] });
                this.handleChange(e);
              }}
              wrapperClass={'tail-search-wrapper'}
              inputClass={'tail-search-input'}
            />
            <Dropdown className="form-group dropdown bootstrap-select show-tick khq-select show">
              <Dropdown.Toggle className="btn dropdown-toggle btn-white">
                {selectedTopics.length === 0
                  ? 'Topics'
                  : selectedTopics.length === 1
                  ? selectedTopics[0]
                  : `${selectedTopics.length} Topics Selected`}
              </Dropdown.Toggle>
              <Dropdown.Menu style={{ maxHeight: '771px', overflow: 'hidden', minHeight: '182px' }}>
                <div className="bs-searchbox">
                  <input
                    type="text"
                    name="dropdownSearch"
                    id="dropdownSearch"
                    className="form-control"
                    autoComplete="off"
                    role="combobox"
                    aria-label="Search"
                    aria-controls="bs-select-1"
                    aria-autocomplete="list"
                    aria-expanded="false"
                    placeholder={'search'}
                    onChange={this.handleChange}
                    value={dropdownSearch}
                  />
                </div>
                <div className="bs-actionsbox">
                  <div className="btn-group btn-group-sm btn-block">
                    <button
                      onClick={() => {
                        this.onStop();

                        this.setState({
                          data: [],
                          selectedTopics: JSON.parse(JSON.stringify(topics)).filter(topic => {
                            if (dropdownSearch.length > 0) {
                              return topic.includes(dropdownSearch);
                            }
                            return topic;
                          })
                        });
                      }}
                      type="button"
                      className="actions-btn bs-select-all btn btn-light"
                    >
                      Select All
                    </button>
                    <button
                      onClick={() => {
                        this.onStop();
                        this.setState({ data: [], selectedTopics: [] });
                      }}
                      type="button"
                      className="actions-btn bs-deselect-all btn btn-light"
                    >
                      Deselect All
                    </button>
                  </div>
                </div>
                {this.renderTopicList()}
              </Dropdown.Menu>
            </Dropdown>

            <Dropdown className="form-group dropdown bootstrap-select show-tick khq-select show">
              <Dropdown.Toggle className="btn dropdown-toggle btn-white">
                Max Records: {maxRecords}
              </Dropdown.Toggle>
              <Dropdown.Menu style={{ maxHeight: '771px', overflow: 'hidden', minHeight: '182px' }}>
                {MAX_RECORDS.map(maxRecord => {
                  return (
                    <li key={`record_${maxRecord}`}>
                      <div
                        onClick={() => {
                          this.onStop();
                          this.setState({ maxRecords: maxRecord, data: [] });
                        }}
                        role="option"
                        className="dropdown-item"
                        aria-selected="false"
                      >
                        {maxRecord}
                      </div>
                    </li>
                  );
                })}
              </Dropdown.Menu>
            </Dropdown>
            <button
              onClick={() => {
                this.onStop();
                this.setState({ selectedStatus: STATUS.STARTED }, () => {
                  this.onStart();
                });
              }}
              className="btn btn-primary"
              type="submit"
            >
              <span className="d-md-none">Search </span>
              <i className="fa fa-search" />
            </button>
            <div className="btn-group actions" role="group">
              <button
                className={`btn btn-secondary pause ${
                  selectedStatus === STATUS.STARTED ? '' : 'd-none'
                }`}
                onClick={() => {
                  this.onStop();
                  this.setState({ selectedStatus: STATUS.PAUSED });
                }}
              >
                <i className={'fa fa-pause'} />
                <span> Pause</span>
              </button>
              <button
                className={`btn btn-secondary resume ${
                  selectedStatus === STATUS.PAUSED ? '' : 'd-none'
                }`}
                onClick={() => {
                  this.onStart();
                  this.setState({ selectedStatus: STATUS.STARTED });
                }}
              >
                <i className="fa fa-play" /> <span> Resume</span>
              </button>
              <button
                className={`btn btn-secondary empty ${
                  selectedStatus === STATUS.STARTED || selectedStatus === STATUS.PAUSED
                    ? ''
                    : 'd-none'
                }`}
                onClick={() => {
                  this.setState({ data: [] });
                }}
              >
                <i className="fa fa-remove" /> <span> Clear</span>
              </button>
            </div>
          </div>
        </nav>
        {selectedStatus !== STATUS.STOPPED && (
          <Table
            history={this.props.history}
            rowId={data => {
              return data.topic.name + '-' + data.partition + '-' + data.offset;
            }}
            columns={[
              {
                id: 'topic',
                accessor: 'topic',
                colName: 'Topic',
                type: 'text',
                cell: obj => {
                  return obj.topic.name;
                }
              },
              {
                id: 'key',
                accessor: 'key',
                colName: 'Key',
                type: 'text',
                cell: obj => {
                  return <span style={{ color: 'red' }}>{obj.key}</span>;
                }
              },
              {
                id: 'timestamp',
                accessor: 'timestamp',
                colName: 'Date',
                type: 'text',
                cell: obj => {
                  return (
                    <div className="tail-headers">
                      <DateTime
                        isoDateTimeString={obj.timestamp}
                        dateTimeFormat={this.state.dateTimeFormat}
                      />
                    </div>
                  );
                }
              },
              {
                id: 'partition',
                accessor: 'partition',
                colName: 'Partition',
                type: 'text'
              },
              {
                id: 'offset',
                accessor: 'offset',
                colName: 'Offset',
                type: 'text'
              },
              {
                id: 'headers',
                accessor: 'headers',
                colName: 'Headers',
                type: 'text',
                expand: true,
                cell: obj => {
                  return (
                    <div className="tail-headers">
                      {obj.headers ? Object.keys(obj.headers).length : 0}
                    </div>
                  );
                }
              },
              {
                id: 'value',
                accessor: 'value',
                colName: 'Schema',
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
                    <pre className="mb-0 khq-data-highlight">
                      <code>{obj.value}</code>
                    </pre>
                  );
                }
              }
            ]}
            extraRow
            noStripes
            data={data}
            updateData={data => {
              this.setState({ data: data });
            }}
            noContent={<tr />}
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
        )}
        <div
          ref={el => {
            this.messagesEnd = el;
          }}
        ></div>
      </div>
    );
  }
}

export default Tail;
