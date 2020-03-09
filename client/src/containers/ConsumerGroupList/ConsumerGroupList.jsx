import React, { Component } from 'react';

import Table from '../../components/Table';
import Header from '../Header';
import SearchBar from '../../components/SearchBar';
import Pagination from '../../components/Pagination';
import api from '../../utils/api';
import endpoints from '../../utils/endpoints';
import constants from '../../utils/constants';
import history from '../../utils/history';

class ConsumerGroupList extends Component {
  state = {
    consumerGroups: [
      {
        id: '1',
        state: 'Active',
        coordinator: '10',
        members: '2',
        topics: [
          {
            _id: Date.now(),
            name: 'test',
            partition: 1,
            replication: 1,
            cleanup: 'delete',
            retention: 50
          }
        ]
      }
    ],
    showDeleteModal: false,
    selectedCluster: '',
    deleteMessage: '',
    pageNumber: 1,
    totalPageNumber: 1,
    searchData: {
      search: ''
    }
  };

  componentDidMount() {
    let { clusterId } = this.props.match.params;
    /*  this.setState({ selectedCluster: clusterId }, () => {
      this.getConsumerGroup();
    });*/
  }

  handleSearch = data => {
    const { searchData } = data;
    this.setState({ pageNumber: 1, searchData }, () => {
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
    const { selectedCluster, pageNumber } = this.state;
    const { search, consumerGroupListView } = this.state.searchData;
    let data = {};
    history.push({
      loading: true
    });
    try {
      data = await api.get(
        endpoints.uriConsumerGroups(selectedCluster, consumerGroupListView, search, pageNumber)
      );
      data = data.data;
      if (data) {
        if (data.consumerGroup) {
          this.handleConsumerGroup(data.consumerGroup);
        } else {
          this.setState({ consumerGroup: [] });
        }
        this.setState({ selectedCluster, totalPageNumber: data.totalPageNumber });
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
      consumerGroup.size = 0;
      consumerGroup.logDirSize = 0;
      tableConsumerGroup.push({
        id: consumerGroup.id,
        state: consumerGroup.state,
        size: consumerGroup.size,
        coordinator: consumerGroup.coordinator,
        members: consumerGroup.members,
        topics: consumerGroup.topics
      });
    });
    this.setState({ consumerGroups: tableConsumerGroup });
  }

  render() {
    const { consumerGroup, selectedCluster, searchData, pageNumber, totalPageNumber } = this.state;
    const { history } = this.props;
    const { clusterId } = this.props.match.params;

    return (
      <div id="content">
        <Header title="Consumer Groups" />
        <nav className="navbar navbar-expand-lg navbar-light bg-light mr-auto khq-data-filter khq-sticky khq-nav">
          <SearchBar
            showSearch={true}
            search={searchData.search}
            showPagination={true}
            pagination={pageNumber}
            showTopicListView={false}
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
              colName: 'State'
            },
            {
              id: 'coordinator',
              accessor: 'coordinator',
              colName: 'Coordinator'
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
                console.log(obj);
                return obj.topics.map(topic => {
                  return <span>{topic.name}</span>;
                });
              }
            }
          ]}
          data={this.state.consumerGroups}
          onDetails={id => {
            history.push(`/${selectedCluster}/consumer-group/${id}`);
          }}
          actions={[constants.TABLE_DETAILS]}
        />

        <div className="navbar navbar-expand-lg navbar-light mr-auto khq-data-filter khq-sticky khq-nav">
          <div className="collapse navbar-collapse" />
          <Pagination
            pageNumber={pageNumber}
            totalPageNumber={totalPageNumber}
            onChange={this.handlePageChange}
            onSubmit={this.handlePageChangeSubmission}
          />
        </div>
      </div>
    );
  }
}
export default ConsumerGroupList;
