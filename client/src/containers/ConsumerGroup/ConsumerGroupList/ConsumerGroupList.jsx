import React, { Component } from 'react';
import Table from '../../../components/Table';
import { uriConsumerGroups, uriConsumerGroupDelete } from '../../../utils/endpoints';
import constants from '../../../utils/constants';
import { calculateTopicOffsetLag } from '../../../utils/converters';
import Header from '../../Header';
import SearchBar from '../../../components/SearchBar';
import Pagination from '../../../components/Pagination';
import ConfirmModal from '../../../components/Modal/ConfirmModal';
import api, { remove } from '../../../utils/api';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
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
    search: '',
    roles: JSON.parse(sessionStorage.getItem('roles'))
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
    const { selectedCluster, pageNumber, search } = this.state;

    let response = await api.get(uriConsumerGroups(selectedCluster, search, pageNumber));
    response = response.data;
    if (response.results) {
      this.handleConsumerGroup(response.results);
      this.setState({ selectedCluster, totalPageNumber: response.page });
    } else {
      this.setState({ selectedCluster, consumerGroups: [], totalPageNumber: 0 });
    }
  }

  handleConsumerGroup(consumerGroup) {
    let tableConsumerGroup = [];
    consumerGroup.forEach(consumerGroup => {
      tableConsumerGroup.push({
        id: consumerGroup.id,
        state: consumerGroup.state,
        coordinator: consumerGroup.coordinator.id,
        members: consumerGroup.members ? consumerGroup.members.length : 0,
        topics: consumerGroup.groupedTopicOffset ? consumerGroup.groupedTopicOffset : {}
      });
    });

    this.setState({ consumerGroups: tableConsumerGroup });
  }

  handleState(state) {
    let className = '';

    switch (state) {
      case 'STABLE':
        className = 'badge badge-success';
        break;
      case 'PREPARING_REBALANCE':
        className = 'badge badge-primary';
        break;
      default:
        className = 'badge badge-warning';
        break;
    }

    return <span className={className}>{state.replace('_', ' ')}</span>;
  }

  handleCoordinator(coordinator) {
    return <span className="badge badge-primary"> {coordinator}</span>;
  }

  handleTopics(group, groupedTopicOffset) {
    const noPropagation = e => e.stopPropagation();
    return Object.keys(groupedTopicOffset).map(topicId => {
      const topicOffsets = groupedTopicOffset[topicId];
      const offsetLag = calculateTopicOffsetLag(topicOffsets, topicId);

      return (
        <a
          href={`/ui/${this.state.selectedCluster}/topic/${topicId}`}
          key={group + '-' + topicId}
          className="btn btn-dark btn-sm mb-1 mr-1"
          onClick={noPropagation}
        >
          {topicId + ' '}

          <div className="badge badge-secondary">Lag: {offsetLag}</div>
        </a>
      );
    });
  }

  handleOnDelete(group) {
    this.setState({ groupToDelete: group }, () => {
      this.showDeleteModal(
        <React.Fragment>
          Do you want to delete consumer group: {<code>{group.id}</code>} ?
        </React.Fragment>
      );
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

    remove(uriConsumerGroupDelete(selectedCluster, groupToDelete.id))
      .then(() => {

        toast.success(`Consumer Group '${groupToDelete.id}' is deleted`);
        this.setState({ showDeleteModal: false, groupToDelete: {} }, () => this.getConsumerGroup());
      })
      .catch(() => {
        this.setState({ showDeleteModal: false, groupToDelete: {} });
      });
  };
  render() {
    const { selectedCluster, search, pageNumber, totalPageNumber } = this.state;
    const roles = this.state.roles || {};
    const { history } = this.props;

    return (
      <div>
        <Header title="Consumer Groups" history={history} />
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
                  return this.handleTopics(obj.id, obj.topics);
                }
              }
            }
          ]}
          data={this.state.consumerGroups}
          updateData={data => {
            this.setState({ consumerGroups: data });
          }}
          noContent={'No consumer group available'}
          onDelete={group => {
            this.handleOnDelete(group);
          }}
          onDetails={id => {
            history.push(`/ui/${selectedCluster}/group/${id}`);
          }}
          actions={
            roles.group && roles.group['group/delete']
              ? [constants.TABLE_DELETE, constants.TABLE_DETAILS]
              : [constants.TABLE_DETAILS]
          }
        />

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
