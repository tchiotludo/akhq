import React, { Component } from 'react';
import Table from '../../components/Table';
import { uriConsumerGroups, uriConsumerGroupDelete } from '../../utils/endpoints';
import constants from '../../utils/constants';
import { calculateTopicOffsetLag } from '../../utils/converters';
import history from '../../utils/history';
import { Link } from 'react-router-dom';
import ConsumerGroup from './ConsumerGroup/ConsumerGroup';
import Header from '../Header';
import SearchBar from '../../components/SearchBar';
import Pagination from '../../components/Pagination';
import ConfirmModal from '../../components/Modal/ConfirmModal';
import api, { remove } from '../../utils/api';

class ConsumerGroupList extends Component {
  state = {
    consumerGroups: [],
    showDeleteModal: false,
    selectedCluster: '',
    deleteMessage: '',
    groupToDelete: {},
    deleteData: {},
    pageNumber: 1,
    totalPageNumber: 1,
    history: this.props,
    search: ''
  };

  componentDidMount() {
    let { clusterId } = this.props.match.params;
    this.setState({ selectedCluster: clusterId }, () => {
      this.getConsumerGroup();
    });
  }

  handleSearch = data => {
    this.setState({ pageNumber: 1, search: data.searchData.search }, () => {
      this.getConsumerGroup();
    });
  };

  handlePageChangeSubmission = value => {
    const { totalPageNumber } = this.state;
    if (value <= 0) {
      value = 1;
    } else if (value > totalPageNumber) {
      value = totalPageNumber;
    }
    this.setState({ pageNumber: value }, () => {
      this.getConsumerGroup();
    });
  };

  handlePageChange = ({ currentTarget: input }) => {
    const { value } = input;
    this.setState({ pageNumber: value });
  };

  async getConsumerGroup() {
    const { history } = this.props;
    const { selectedCluster, pageNumber, search } = this.state;

    history.push({
      loading: true
    });

    try {
      let response = await api.get(uriConsumerGroups(selectedCluster, search, pageNumber));
      response = response.data;
      if (response.results) {
        this.handleConsumerGroup(response.results);
        this.setState({ selectedCluster, totalPageNumber: response.total });
      } else {
        this.setState({ selectedCluster, consumerGroups: [], totalPageNumber: 0 });
      }
    } catch (err) {
      history.replace('/error', { errorData: err });
    } finally {
      history.push({
        loading: false
      });
    }
  }

  handleConsumerGroup(consumerGroup) {
    let tableConsumerGroup = [];
    consumerGroup.map(consumerGroup => {
      tableConsumerGroup.push({
        id: consumerGroup.id,
        state: consumerGroup.state,
        coordinator: consumerGroup.coordinator.id,
        members: consumerGroup.members.length,
        topics: consumerGroup.groupedTopicOffset
      });
    });

    this.setState({ consumerGroups: tableConsumerGroup });
  }

  handleState(state) {
    return (
      <span className={state === 'STABLE' ? 'badge badge-success' : 'badge badge-warning'}>
        {state}
      </span>
    );
  }

  handleCoordinator(coordinator) {
    return <span className="badge badge-primary"> {coordinator}</span>;
  }

  handleTopics(groupedTopicOffset) {
    const { history } = this.props;
    return Object.keys(groupedTopicOffset).map(topicId => {
      const topicOffsets = groupedTopicOffset[topicId];
      const offsetLag = calculateTopicOffsetLag(topicOffsets);

      return (
        <div
          onClick={() => {
            history.push({
              pathname: `/${this.state.selectedCluster}/topic/${topicId}`,
              tab: constants.TOPIC
            });
          }}
        >
          <Link
            to={`/${this.state.selectedCluster}/topic/${topicId}`}
            key="lagTopic.topicId"
            className="btn btn-dark btn-sm mb-1"
          >
            {topicId + ' '}

            <a href="#" className="badge badge-secondary">
              Lag:{offsetLag}
            </a>
          </Link>
        </div>
      );
    });
  }

  handleOnDelete(group) {
    this.setState({ groupToDelete: group }, () => {
      this.showDeleteModal(`Delete ConsumerGroup ${group.id}?`);
    });
  }

  showDeleteModal = deleteMessage => {
    this.setState({ showDeleteModal: true, deleteMessage });
  };

  closeDeleteModal = () => {
    this.setState({ showDeleteModal: false, deleteMessage: '' });
  };

  deleteConsumerGroup = () => {
    const { selectedCluster, groupToDelete } = this.state;
    const { history } = this.props;

    history.push({ loading: true });
    remove(uriConsumerGroupDelete(selectedCluster, groupToDelete.id))
      .then(res => {
        this.props.history.push({
          showSuccessToast: true,
          successToastMessage: `Consumer Group '${groupToDelete.id}' is deleted`,
          loading: false
        });
        this.setState({ showDeleteModal: false, groupToDelete: {} }, () => this.getConsumerGroup());
      })
      .catch(err => {
        this.props.history.push({
          showErrorToast: true,
          errorToastTitle: `Failed to delete '${groupToDelete.id}'`,
          errorToastMessage: err.response.data.message,
          loading: false
        });
        this.setState({ showDeleteModal: false, groupToDelete: {} });
      });
  };
  render() {
    const { consumerGroup, selectedCluster, search, pageNumber, totalPageNumber } = this.state;
    const { history } = this.props;
    const { clusterId } = this.props.match.params;

    return (
      <div id="content">
        <Header title="Consumer Groups" />
        <nav
          className="navbar navbar-expand-lg navbar-light bg-light mr-auto
         khq-data-filter khq-sticky khq-nav"
        >
          <SearchBar
            showSearch={true}
            search={search}
            showPagination={true}
            pagination={pageNumber}
            showTopicListView={false}
            showConsumerGroup
            groupListView={'ALL'}
            doSubmit={this.handleSearch}
          />

          <Pagination
            pageNumber={pageNumber}
            totalPageNumber={totalPageNumber}
            onChange={this.handlePageChange}
            onSubmit={this.handlePageChangeSubmission}
          />
        </nav>

        <Table
          columns={[
            {
              id: 'id',
              accessor: 'id',
              colName: 'Id'
            },
            {
              id: 'state',
              accessor: 'state',
              colName: 'State',
              cell: obj => {
                return this.handleState(obj.state);
              }
            },
            {
              id: 'coordinator',
              accessor: 'coordinator',
              colName: 'Coordinator',
              cell: obj => {
                return this.handleCoordinator(obj.coordinator);
              }
            },
            {
              id: 'members',
              accessor: 'members',
              colName: 'Members'
            },
            {
              id: 'topics',
              accessor: 'topics',
              colName: 'Topics',
              cell: obj => {
                if (obj.topics) {
                  return this.handleTopics(obj.topics);
                }
              }
            }
          ]}
          data={this.state.consumerGroups}
          noContent={'No consumer group available'}
          onDelete={group => {
            this.handleOnDelete(group);
          }}
          onDetails={id => {
            history.push(`/${selectedCluster}/group/${id}`);
          }}
          actions={[constants.TABLE_DELETE, constants.TABLE_DETAILS]}
        />

        <div
          className="navbar navbar-expand-lg navbar-light mr-auto
         khq-data-filter khq-sticky khq-nav"
        >
          <div className="collapse navbar-collapse" />
          <Pagination
            pageNumber={pageNumber}
            totalPageNumber={totalPageNumber}
            onChange={this.handlePageChange}
            onSubmit={this.handlePageChangeSubmission}
          />
        </div>
        <ConfirmModal
          show={this.state.showDeleteModal}
          handleCancel={this.closeDeleteModal}
          handleConfirm={this.deleteConsumerGroup}
          message={this.state.deleteMessage}
        />
      </div>
    );
  }
}
export default ConsumerGroupList;
