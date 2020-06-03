import React, { Component } from 'react';
import Header from '../Header';
import Table from '../../components/Table';
import * as constants from '../../utils/constants';
import { get } from '../../utils/api';
import { uriAclsList } from '../../utils/endpoints';
import SearchBar from '../../components/SearchBar';

class Acls extends Component {
  state = {
    data: [],
    selectedCluster: '',
    searchData: {
      search: ''
    }
  };

  componentDidMount() {
    this.getAcls();
  }

  async getAcls() {
    let acls = [];
    const { clusterId } = this.props.match.params;
    const { history } = this.props;
    history.replace({
      loading: true
    });
    try {
      acls = await get(uriAclsList(clusterId, this.state.searchData.search));
      this.handleData(acls.data);
    } catch (err) {
      if (err.response && err.response.status === 404) {
        history.replace('/ui/page-not-found', { errorData: err });
      } else {
        history.replace('/ui/error', { errorData: err });
      }
    } finally {
      history.replace({
        loading: false
      });
    }
  }

  handleData(acls) {
    let tableAcls = acls.map(acl => {
      acl.principalEncoded = btoa(acl.principal);
      return {
        id: acl,
        user: acl.principal || ''
      };
    });
    this.setState({ data: tableAcls });
    return tableAcls;
  }

  handleSearch = data => {
    const { searchData } = data;
    this.setState({ searchData }, () => {
      this.getAcls();
    });
  };

  render() {
    const { history } = this.props;
    const { data, searchData } = this.state;
    const { clusterId } = this.props.match.params;
    return (
      <div>
        <Header title="Acls" history={this.props.history} />
        <nav
          className="navbar navbar-expand-lg navbar-light bg-light mr-auto
         khq-data-filter khq-sticky khq-nav"
        >
          <SearchBar
            showSearch={true}
            search={searchData.search}
            showPagination={false}
            showTopicListView={false}
            showConsumerGroup
            groupListView={'ALL'}
            doSubmit={this.handleSearch}
          />
        </nav>
        <Table
          columns={[
            {
              id: 'user',
              accessor: 'user',
              colName: 'Principals',
              type: 'text'
            }
          ]}
          actions={[constants.TABLE_DETAILS]}
          data={data}
          noContent={
            <tr>
              <td colSpan={3}>
                <div className="alert alert-warning mb-0" role="alert">
                  No ACLS found, or the "authorizer.class.name" parameter is not configured on the
                  cluster.
                </div>
              </td>
            </tr>
          }
          onDetails={acl => {
            this.props.history.push({
              pathname: `/ui/${clusterId}/acls/${acl.principalEncoded}`,
              principal: acl.user
            });
          }}
        />
      </div>
    );
  }
}

export default Acls;
