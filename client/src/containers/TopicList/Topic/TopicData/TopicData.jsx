import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import Dropdown from 'react-bootstrap/Dropdown';
import './styles.scss';
import Table from '../../../../components/Table/Table';
import { get } from '../../../../utils/api';
import { formatDateTime } from '../../../../utils/converters';
import { uriTopicData, uriTopicsPartitions } from '../../../../utils/endpoints';
import CodeViewModal from '../../../../components/Modal/CodeViewModal/CodeViewModal';
import Modal from '../../../../components/Modal/Modal';
import Pagination from '../../../../components/Pagination/Pagination';
import moment from 'moment';
import DatePicker from '../../../../components/DatePicker/DatePicker';
import Input from '../../../../components/Form/Input';
import _ from 'lodash';
import { checkPropTypes } from 'prop-types';
import AceEditor from 'react-ace';

// Adaptation of data.ftl

class TopicData extends Component {
  state = {
    showValueModal: false,
    valueModalBody: '',
    showHeadersModal: false,
    headersModalBody: '',
    sortBy: 'Oldest',
    sortOptions: ['Oldest', 'Newest'],
    partitionCount: 0,
    partition: 'All',
    partitionOptions: [],
    offsetsOptions: [],
    timestamp: '',
    currentSearch: '',
    search: '',
    offsets: {},
    offsetsSearch: '',
    openDateModal: false,
    messages: [],
    pageNumber: 1,
    nextPage: '',
    recordCount: 0,
    showFilters: ''
  };

  componentDidMount() {
    let { clusterId, topicId } = this.props.match.params;

    this.setState({ selectedCluster: clusterId, selectedTopic: topicId }, () => {
      this.getMessages();
    });
  }

  showValueModal = body => {
    this.setState({
      showValueModal: true,
      valueModalBody: body
    });
  };

  closeValueModal = () => {
    this.setState({ showValueModal: false, valueModalBody: '' });
  };

  showHeadersModal = headers => {
    this.setState({
      showHeadersModal: true,
      headersModalBody: Object.keys(headers).map(key => {
        return { key: key, value: headers[key] };
      })
    });
  };

  closeHeadersModal = () => {
    this.setState({ showHeadersModal: false, headersModalBody: '' });
  };

  async getMessages(changePage = false) {
    const { history } = this.props;
    const {
      selectedCluster,
      selectedTopic,
      sortBy,
      partition,
      timestamp,
      currentSearch,
      offsetsSearch,
      nextPage
    } = this.state;
    let data,
      partitionData = {};
    history.replace({
      loading: true
    });
    try {
      data = await get(
        uriTopicData(
          selectedCluster,
          selectedTopic,
          offsetsSearch !== '' ? offsetsSearch : undefined,
          partition,
          sortBy,
          timestamp !== ''
            ? formatDateTime(
                {
                  year: timestamp.year(),
                  monthValue: timestamp.month() + 1,
                  dayOfMonth: timestamp.date(),
                  hour: timestamp.hour(),
                  minute: timestamp.minute(),
                  second: timestamp.second(),
                  milli: timestamp.millisecond()
                },
                'YYYY-MM-DDThh:mm:ss.SSS',
                true
              ) + 'Z'
            : undefined,
          currentSearch !== '' ? currentSearch : undefined,
          changePage ? nextPage : undefined
        )
      );
      data = data.data;
      partitionData = await get(uriTopicsPartitions(selectedCluster, selectedTopic));
      partitionData = partitionData.data;
      if (data.results) {
        this.handleMessages(data.results);
      } else {
        this.setState({ messages: [], pageNumber: 1 });
      }
      if (partitionData) {
        if (changePage) {
          this.getNextPageOffsets();
        }
        this.setState({
          partitionCount: partitionData.length,
          nextPage: data.after,
          recordCount: data.size
        });
      }
    } catch (err) {
      if (err.response && err.response.status === 404) {
        history.replace('/page-not-found', { errorData: err });
      } else {
        history.replace('/error', { errorData: err });
      }
    } finally {
      history.replace({
        loading: false
      });
    }
  }

  handleMessages = messages => {
    let tableMessages = [];
    messages.map(message => {
      message.key = message.key ? message.key : 'null';
      message.value = message.value ? message.value : 'null';
      const date = moment(message.timestamp);
      message.timestamp = formatDateTime(
        {
          year: date.year(),
          monthValue: date.month() + 1,
          dayOfMonth: date.date(),
          hour: date.hour(),
          minute: date.minute(),
          second: date.second(),
          milli: date.millisecond()
        },
        'MMM DD, YYYY, hh:mm A'
      );
      message.partition = message.partition ? message.partition : '0';
      message.offset = message.offset ? message.offset : '0';
      message.headers = message.headers ? message.headers : {};
      message.schema = message.schema ? message.schema : '';

      tableMessages.push(message);
    });
    this.setState({ messages: tableMessages });
  };

  getNextPageOffsets = () => {
    const { nextPage } = this.state;
    let { offsets } = this.state;

    let aux = nextPage.substring(nextPage.indexOf('after=') + 6);
    let afterString = aux.substring(0, aux.indexOf('&'));
    const offsetsByPartition = afterString.split('_');

    offsetsByPartition.map(offsetByPartition => {
      const offset = offsetByPartition.split('-');
      offsets[`partition${offset[0]}`] = offset[1];
    });

    this.setState({ offsets });
  };

  createPartitionOptions = () => {
    const { partitionCount } = this.state;
    let partitionOptions = ['All'];
    for (let i = 0; i < partitionCount; i++) {
      partitionOptions.push(`${i}`);
    }
    return partitionOptions;
  };

  createOffsetsOptions = () => {
    const { partitionCount } = this.state;
    let offsetsOptions = [];
    for (let i = 0; i < partitionCount; i++) {
      offsetsOptions.push(`Partition ${i}`);
    }
    return offsetsOptions;
  };

  renderSortOptions() {
    const { sortOptions } = this.state;

    let renderedOptions = [];
    for (let option of sortOptions) {
      renderedOptions.push(
        <Dropdown.Item
          key={option}
          onClick={() => this.setState({ sortBy: option }, () => this.getMessages())}
        >
          <i className="fa fa-fw fa-sort-numeric-desc pull-left" aria-hidden="true" /> {option}
        </Dropdown.Item>
      );
    }
    return renderedOptions;
  }

  renderPartitionOptions = () => {
    const partitionOptions = this.createPartitionOptions();

    let renderedOptions = [];
    for (let option of partitionOptions) {
      renderedOptions.push(
        <Dropdown.Item
          key={option}
          onClick={() => this.setState({ partition: option }, () => this.getMessages())}
        >
          <i className="fa fa-fw pull-left" aria-hidden="true" /> {option}
        </Dropdown.Item>
      );
    }
    return renderedOptions;
  };

  renderOffsetsOptions = () => {
    const offsetsOptions = this.createOffsetsOptions();

    let renderedOptions = [];
    let i;
    for (i = 0; i < offsetsOptions.length; i++) {
      const option = offsetsOptions[i];
      const camelcaseOption = _.camelCase(option);
      let { offsets } = this.state;
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
      sortBy,
      partition,
      timestamp,
      currentSearch,
      search,
      offsets,
      offsetsSearch,
      messages,
      showHeadersModal,
      showValueModal,
      valueModalBody,
      headersModalBody,
      pageNumber,
      nextPage,
      recordCount,
      showFilters
    } = this.state;
    const { loading } = this.props.history.location;
    const firstColumns = [
      { colName: 'Key', colSpan: 1 },
      { colName: 'Value', colSpan: 1 },
      { colName: 'Date', colSpan: 1 },
      { colName: 'Partition', colSpan: 1 },
      { colName: 'Offset', colSpan: 1 },
      { colName: 'Headers', colSpan: 1 },
      { colname: 'Schema', colSpan: 1 }
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
              this.openAndCloseFilters();
            }}
          >
            <span className="navbar-toggler-icon" />
          </button>

          <nav className="pagination-data">
            <div style={{ paddingTop: '1rem' }}>
              <label>Total records: ≈{recordCount}</label>
            </div>
            <div>
              <Pagination
                pageNumber={pageNumber}
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
                      this.getMessages(true);
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
                  {!loading && <Dropdown.Menu>{this.renderSortOptions()}</Dropdown.Menu>}
                </Dropdown>
              </li>
              <li className="nav-item dropdown">
                <Dropdown>
                  <Dropdown.Toggle className="nav-link dropdown-toggle">
                    <strong>Partition:</strong> ({partition})
                  </Dropdown.Toggle>
                  {!loading && <Dropdown.Menu>{this.renderPartitionOptions()}</Dropdown.Menu>}
                </Dropdown>
              </li>
              <li className="nav-item dropdown">
                <Dropdown>
                  <Dropdown.Toggle className="nav-link dropdown-toggle">
                    <strong>Timestamp:</strong>{' '}
                    {timestamp !== '' &&
                      formatDateTime(
                        {
                          year: timestamp.year(),
                          monthValue: timestamp.month() + 1,
                          dayOfMonth: timestamp.date(),
                          hour: timestamp.hour(),
                          minute: timestamp.minute(),
                          second: timestamp.second(),
                          milli: timestamp.millisecond()
                        },
                        'MMM DD, YYYY, hh:mm A'
                      )}
                  </Dropdown.Toggle>
                  {!loading && (
                    <Dropdown.Menu>
                      <div className="input-group mb-2 datetime-picker-div">
                        <DatePicker
                          name={'datetime-picker'}
                          value={timestamp}
                          onChange={value => {
                            this.setState({ timestamp: moment(value) }, () => this.getMessages());
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
                    <strong>Search:</strong> {currentSearch !== '' ? `(${currentSearch})` : ''}
                  </Dropdown.Toggle>
                  {!loading && (
                    <Dropdown.Menu>
                      <div className="input-group">
                        <input
                          className="form-control"
                          name="search"
                          type="text"
                          value={search}
                          onChange={({ currentTarget: input }) => {
                            this.setState({ search: input.value });
                          }}
                        />
                        <div className="input-group-append">
                          <button
                            className="btn btn-primary"
                            type="button"
                            onClick={() =>
                              this.setState({ currentSearch: search, search: '' }, () =>
                                this.getMessages()
                              )
                            }
                          >
                            OK
                          </button>
                        </div>
                      </div>
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
                      <div className="khq-offset-navbar">
                        <div className="input-group">
                          <table>{this.renderOffsetsOptions()}</table>
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
                                this.setState({ offsetsSearch }, () => this.getMessages());
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
        <div className="table-responsive">
          <Table
            firstHeader={firstColumns}
            columns={[
              {
                id: 'key',
                accessor: 'key',
                colName: 'Key',
                type: 'text',
                cell: (obj, col) => {
                  return (
                    <div className="value cell-div">
                      <div className="align-cell">
                        <span>
                          <code className="key">{obj[col.accessor]}</code>
                        </span>
                      </div>
                    </div>
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
                  return (
                    <AceEditor
                      mode="json"
                      id={'value' + index}
                      theme="dracula"
                      value={obj.value}
                      readOnly
                      name="UNIQUE_ID_OF_DIV"
                      editorProps={{ $blockScrolling: true }}
                      style={{ width: '100%', minHeight: '25vh' }}
                    />
                  );
                },
                cell: (obj, index) => {
                  return (
                    <pre class="mb-0 khq-data-highlight">
                      <code>{obj.value}</code>
                    </pre>
                  );
                }
              },
              {
                id: 'timestamp',
                accessor: 'timestamp',
                colName: 'Date',
                type: 'text',
                cell: (obj, col) => {
                  return (
                    <div className="value cell-div">
                      <div className="align-cell">{obj[col.accessor]}</div>
                    </div>
                  );
                }
              },
              {
                id: 'partition',
                accessor: 'partition',
                colName: 'Partition',
                type: 'text',
                cell: (obj, col) => {
                  return (
                    <div className="value cell-div">
                      <div className="align-cell">{obj[col.accessor]}</div>
                    </div>
                  );
                }
              },
              {
                id: 'offset',
                accessor: 'offset',
                colName: 'Offset',
                type: 'text',
                cell: (obj, col) => {
                  return (
                    <div className="value cell-div">
                      <div className="align-cell">{obj[col.accessor]}</div>
                    </div>
                  );
                }
              },
              {
                id: 'headers',
                accessor: 'headers',
                colName: 'Headers',
                type: 'text',
                expand: true,
                cell: obj => {
                  return <a className="tail-headers">{Object.keys(obj.headers).length}</a>;
                }
              },
              {
                id: 'schema',
                accessor: 'schema',
                colName: 'Schema',
                type: 'text',
                cell: (obj, col) => {
                  return (
                    <div className="value cell-div">
                      <div className="align-cell">{obj[col.accessor]}</div>
                    </div>
                  );
                }
              }
            ]}
            extraRow
            noStripes
            data={messages}
            onExpand={obj => {
              return Object.keys(obj.headers).map(header => {
                return (
                  <tr
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
        <Modal show={showHeadersModal} handleClose={this.closeHeadersModal}>
          <div className="headers-modal">
            <button
              type="button"
              className="close pull-right"
              aria-label="Close"
              onClick={this.closeHeadersModal}
            >
              <span aria-hidden="true">&times;</span>
            </button>
            <Table
              firstHeader={[
                { colName: 'Key', colSpan: 1 },
                { colName: 'Value', colSpan: 1 }
              ]}
              columns={[
                {
                  id: 'headerKey',
                  accessor: 'key',
                  colName: 'Key',
                  type: 'text',
                  cell: (obj, col) => {
                    return <div className="align-cell">{obj[col.accessor]}</div>;
                  }
                },
                {
                  id: 'headerValue',
                  accessor: 'value',
                  colName: 'Value',
                  type: 'text',
                  cell: (obj, col) => {
                    return (
                      <div className="value">
                        <div className="align-cell value-text headers-detail-value">
                          {obj[col.accessor] ? obj[col.accessor].substring(0, 50) : 'N/A'}
                          {obj[col.accessor] && obj[col.accessor].length > 50 && '(...)'}{' '}
                        </div>
                        <div className="headers-detail-button">
                          <button
                            className="btn btn-secondary headers pull-right"
                            onClick={() => this.showValueModal(obj[col.accessor])}
                          >
                            Details
                          </button>
                        </div>
                      </div>
                    );
                  }
                }
              ]}
              data={headersModalBody}
            />
          </div>
        </Modal>
        <CodeViewModal
          show={showValueModal}
          body={valueModalBody}
          handleClose={this.closeValueModal}
        />
      </React.Fragment>
    );
  }
}

export default TopicData;
