import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import Dropdown from 'react-bootstrap/Dropdown';
import './styles.scss';
import Table from '../../../../components/Table/Table';
import { get } from '../../../../utils/api';
import { formatDateTime } from '../../../../utils/converters';
import { uriTopicData } from '../../../../utils/endpoints';
import CodeViewModal from '../../../../components/Modal/CodeViewModal/CodeViewModal';
import Modal from '../../../../components/Modal/Modal';
import Pagination from '../../../../components/Pagination/Pagination';

// Adaptation of data.ftl

class TopicData extends Component {
  state = {
    showValueModal: false,
    valueModalBody: '',
    showHeadersModal: false,
    headersModalBody: '',
    sortBy: 'Oldest',
    sortOptions: ['Oldest', 'Newest'],
    partition: 'All',
    partitionOptions: [],
    timestamp: '',
    messages: []
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
    console.log('close');
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

  async getMessages() {
    const { history } = this.props;
    const { selectedCluster, selectedTopic } = this.state;

    let data = {};
    history.push({
      loading: true
    });
    try {
      data = await get(uriTopicData(selectedCluster, selectedTopic));
      data = data.data;
      if (data) {
        this.handleMessages(data);
      } else {
        this.setState({ messages: [] });
      }
    } catch (err) {
      history.replace('/error', { errorData: err });
    } finally {
      history.push({
        loading: false
      });
    }
  }

  handleMessages = messages => {
    let tableMessages = [];
    console.log(messages);
    messages.map(message => {
      message.key = message.key ? message.key : 'null';
      message.date = formatDateTime(message.date, 'MMM DD, YYYY, hh:mm A');
      message.headers = message.headers ? message.headers : {};
      message.schema = message.schema ? message.schema : '';

      tableMessages.push(message);
    });
    this.setState({ messages: tableMessages });
  };

  createPartitionOptions = partitions => {
    let partitionOptions = ['All'];
    for (let i = 0; i < partitions; i++) {
      partitionOptions.push(`${i}`);
    }
    return partitionOptions;
  };

  onTimestampChange = timestamp => {
    this.setState({ timestamp });
  };

  renderSortOptions() {
    const { sortOptions } = this.state;

    let renderedOptions = [];
    for (let option of sortOptions) {
      renderedOptions.push(
        <Dropdown.Item key={option}>
          <i className="fa fa-fw fa-sort-numeric-desc pull-left" aria-hidden="true" /> {option}
        </Dropdown.Item>
      );
    }
    return renderedOptions;
  }

  renderPartitionOptions = partitions => {
    const partitionOptions = this.createPartitionOptions(partitions);

    let renderedOptions = [];
    for (let option of partitionOptions) {
      renderedOptions.push(
        <Dropdown.Item key={option}>
          <i className="fa fa-fw pull-left" aria-hidden="true" /> {option}
        </Dropdown.Item>
      );
    }
    return renderedOptions;
  };

  render() {
    const {
      sortBy,
      partition,
      timestamp,
      messages,
      showHeadersModal,
      showValueModal,
      valueModalBody,
      headersModalBody
    } = this.state;
    const firstColumns = [
      { colName: 'Key', colSpan: 1 },
      { colName: 'Value', colSpan: 1 },
      { colName: 'Date', colSpan: 1 },
      { colName: 'Partition', colSpan: 1 },
      { colName: 'Offset', colSpan: 1 },
      { colName: 'Headers', colSpan: 1 },
      { colname: 'Schema', colSpan: 1 }
    ];

    console.log(messages);

    return (
      <React.Fragment>
        <nav className="navbar navbar-expand-lg navbar-light bg-light mr-auto khq-data-filter khq-sticky khq-nav">
          <button
            className="navbar-toggler"
            type="button"
            data-toggle="collapse"
            data-target="#topic-data"
            aria-controls="topic-data"
            aria-expanded="false"
            aria-label="Toggle navigation"
          >
            <span className="navbar-toggler-icon" />
          </button>

          <nav className={'pagination-data'}>
            <Pagination />
          </nav>

          <div className="collapse navbar-collapse" id="topic-data">
            <ul className="navbar-nav mr-auto">
              <li className="nav-item dropdown">
                <Dropdown>
                  <Dropdown.Toggle className="nav-link dropdown-toggle">
                    <strong>Sort:</strong> ({sortBy})
                  </Dropdown.Toggle>
                  <Dropdown.Menu>{this.renderSortOptions()}</Dropdown.Menu>
                </Dropdown>
              </li>
              <li className="nav-item dropdown">
                <Dropdown>
                  <Dropdown.Toggle className="nav-link dropdown-toggle">
                    <strong>Partition:</strong> ({partition})
                  </Dropdown.Toggle>
                  <Dropdown.Menu>{this.renderPartitionOptions(1)}</Dropdown.Menu>
                </Dropdown>
              </li>
              <li className="nav-item dropdown">
                <Dropdown>
                  <Dropdown.Toggle className="nav-link dropdown-toggle">
                    <strong>Timestamp:</strong> {timestamp !== '' ? { timestamp } : ''}
                  </Dropdown.Toggle>
                  <Dropdown.Menu>
                    <div class="khq-data-datetime">
                      <div class="input-group mb-2">
                        <input
                          class="form-control"
                          name="timestamp"
                          type="text"
                          value={timestamp}
                        />
                        <div class="input-group-append">
                          <button class="btn btn-primary" type="button">
                            OK
                          </button>
                        </div>
                      </div>
                      <div class="datetime-container"></div>
                    </div>
                  </Dropdown.Menu>
                </Dropdown>
              </li>
              <li className="nav-item dropdown">
                <Link
                  className="nav-link dropdown-toggle"
                  to="#"
                  role="button"
                  data-toggle="dropdown"
                  aria-haspopup="true"
                  aria-expanded="false"
                >
                  <strong>Search:</strong>
                </Link>
                <div className="dropdown-menu khq-search-navbar">
                  <div className="input-group">
                    <input className="form-control" name="search" type="text" />
                    <div className="input-group-append">
                      <button className="btn btn-primary" type="button">
                        OK
                      </button>
                    </div>
                  </div>
                </div>
              </li>
              <li className="nav-item dropdown">
                <Link
                  className="nav-link dropdown-toggle"
                  to="#"
                  role="button"
                  data-toggle="dropdown"
                  aria-haspopup="true"
                  aria-expanded="false"
                >
                  <strong>Offsets:</strong>
                </Link>
                <div className="dropdown-menu khq-offset-navbar">
                  <div className="input-group">
                    <table></table>
                    <div className="input-group-append">
                      <button className="btn btn-primary" type="button">
                        OK
                      </button>
                    </div>
                  </div>
                </div>
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
                  return <code className="key">{obj[col.accessor]}</code>;
                }
              },
              {
                id: 'value',
                accessor: 'value',
                colName: 'Value',
                type: 'text',
                cell: (obj, col) => {
                  return (
                    <div className="value">
                      {obj[col.accessor] ? obj[col.accessor].substring(0, 150) : 'N/A'}
                      {obj[col.accessor] && obj[col.accessor].length > 150 && '(...)'}{' '}
                      <button
                        className="btn btn-secondary headers pull-right"
                        onClick={() => this.showValueModal(obj[col.accessor])}
                      >
                        Details
                      </button>
                    </div>
                  );
                }
              },
              {
                id: 'date',
                accessor: 'date',
                colName: 'Date',
                type: 'text'
              },
              {
                id: 'partition',
                accessor: 'partition',
                colName: 'Partition',
                type: 'text',
                cell: (obj, col) => {
                  return <div className="text-right">{obj[col.accessor]}</div>;
                }
              },
              {
                id: 'headers',
                accessor: 'headers',
                colName: 'Headers',
                type: 'text',
                cell: (obj, col) => {
                  return (
                    <div class="text-right">
                      {Object.keys(obj[col.accessor]).length}
                      {Object.keys(obj[col.accessor]).length > 0 && (
                        <React.Fragment>
                          {' '}
                          ⤑{' '}
                          <button
                            className="btn btn-secondary headers"
                            onClick={() => this.showHeadersModal(obj[col.accessor])}
                          >
                            Details
                          </button>
                        </React.Fragment>
                      )}
                    </div>
                  );
                }
              },
              {
                id: 'schema',
                accessor: 'schema',
                colName: 'Schema',
                type: 'text',
                cell: (obj, col) => {
                  return <div className="text-right">{obj[col.accessor]}</div>;
                }
              }
            ]}
            data={messages}
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
                  type: 'text'
                },
                {
                  id: 'headerValue',
                  accessor: 'value',
                  colName: 'Value',
                  type: 'text',
                  cell: (obj, col) => {
                    return (
                      <div className="value">
                        <div className="value-text">
                          {obj[col.accessor] ? obj[col.accessor].substring(0, 50) : 'N/A'}
                          {obj[col.accessor] && obj[col.accessor].length > 50 && '(...)'}{' '}
                        </div>
                        <button
                          className="btn btn-secondary headers pull-right"
                          onClick={() => this.showValueModal(obj[col.accessor])}
                        >
                          Details
                        </button>
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
