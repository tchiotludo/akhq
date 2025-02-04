import React from 'react';
import Table from '../../../../components/Table';
import { uriTopicsGroups } from '../../../../utils/endpoints';
import constants, { SETTINGS_VALUES } from '../../../../utils/constants';
import Root from '../../../../components/Root';
import { Link } from 'react-router-dom';
import { withRouter } from '../../../../utils/withRouter.jsx';
import SearchBar from '../../../../components/SearchBar/index.jsx';
import { getClusterUIOptions } from '../../../../utils/functions.jsx';

class TopicGroups extends Root {
  state = {
    consumerGroups: [],
    topicId: this.props.topicId,
    showDeleteModal: false,
    selectedCluster: this.props.clusterId,
    deleteMessage: '',
    loading: true,
    groupsListView: 'ALL'
  };

  componentDidMount() {
    this._initializeVars(() => this.getConsumerGroup());
  }

  async _initializeVars(callBackFunction) {
    const { clusterId } = this.props.params;
    const uiOptions = await getClusterUIOptions(clusterId);
    const query = new URLSearchParams(this.props.location.search);

    this.setState(
      {
        groupsListView: query.get('groupsListView')
          ? query.get('groupsListView')
          : uiOptions && uiOptions.topic && uiOptions.topic.groupsDefaultView
            ? uiOptions.topic.groupsDefaultView
            : SETTINGS_VALUES.TOPIC.CONSUMER_GROUP_DEFAULT_VIEW.ALL
      },
      callBackFunction
    );
  }

  async getConsumerGroup() {
    const { selectedCluster, topicId, groupsListView } = this.state;
    this.setState({ loading: true });

    let data = await this.getApi(uriTopicsGroups(selectedCluster, topicId, groupsListView));
    if (data && data.data) {
      this.handleGroups(data.data);
    } else {
      this.setState({ consumerGroup: [], loading: false });
    }
  }

  handleSearch = () => {
    // Cancel previous requests if there are some to prevent UI issues
    this.cancelAxiosRequests();
    this.renewCancelToken();

    this.getConsumerGroup();

    this.props.router.navigate({
      pathname: `/ui/${this.state.selectedCluster}/topic/${this.state.topicId}/groups`,
      search: `groupsListView=${this.state.groupsListView}`
    });
  };

  handleGroups(consumerGroups) {
    let tableConsumerGroups =
      consumerGroups.map(consumerGroup => {
        return {
          id: consumerGroup.id,
          state: consumerGroup.state,
          coordinator: consumerGroup.coordinator.id,
          members: consumerGroup.members ? consumerGroup.members.length : 0,
          topics: this.groupTopics(consumerGroup.offsets)
        };
      }) || [];
    this.setState({ consumerGroups: tableConsumerGroups, loading: false });
  }

  groupTopics(topics) {
    if (!topics) return {};
    return topics.reduce(function (a, e) {
      let key = e.topic;
      a[key] ? (a[key] = a[key] + e.offsetLag) : (a[key] = e.offsetLag || 0);
      return a;
    }, {});
  }

  handleState(state) {
    return (
      <span className={state === 'STABLE' ? 'badge bg-success' : 'badge bg-warning'}>{state}</span>
    );
  }

  handleCoordinator(coordinator) {
    return <span className="badge bg-primary"> {coordinator}</span>;
  }

  handleTopics(topics) {
    const noPropagation = e => e.stopPropagation();
    return Object.keys(topics).map(topic => {
      return (
        <Link
          to={`/ui/${this.state.selectedCluster}/topic/${topic}`}
          key={`lagTopic.${topic}`}
          className="btn btn-dark btn-sm mb-1 me-1"
          onClick={noPropagation}
        >
          {topic}{' '}
          <div className="badge bg-secondary">Lag: {Number(topics[topic]).toLocaleString()}</div>
        </Link>
      );
    });
  }

  render() {
    const { selectedCluster, loading, groupsListView } = this.state;

    return (
      <div>
        <nav className="navbar navbar-expand-lg navbar-light bg-light me-auto khq-data-filter khq-sticky khq-nav">
          <SearchBar
            showSearch={false}
            showPagination={true}
            showTopicListView={false}
            showGroupsListView={true}
            groupsListView={groupsListView}
            ongroupsListViewChange={value => {
              this.setState({ groupsListView: value });
            }}
            doSubmit={this.handleSearch}
          />
        </nav>
        <Table
          loading={loading}
          columns={[
            {
              id: 'id',
              accessor: 'id',
              colName: 'Id',
              sortable: true
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
              colName: 'Members',
              sortable: true
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
          updateData={data => {
            this.setState({ consumerGroups: data });
          }}
          onDetails={id => {
            this.props.router.navigate(
              {
                pathname: `/ui/${selectedCluster}/group/${id}`
              },
              { replace: true }
            );
          }}
          actions={[constants.TABLE_DETAILS]}
        />
      </div>
    );
  }
}

export default withRouter(TopicGroups);
