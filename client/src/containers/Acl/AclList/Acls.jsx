import React from 'react';
import Header from '../../Header';
import Table from '../../../components/Table';
import * as constants from '../../../utils/constants';
import { uriAclsList } from '../../../utils/endpoints';
import SearchBar from '../../../components/SearchBar';
import Root from "../../../components/Root";

class Acls extends Root {
  state = {
    data: [],
    selectedCluster: '',
    searchData: {
      search: ''
    },
    loading: true
  };

  componentDidMount() {
    this.getAcls();
  }

  async getAcls() {
    let acls = [];
    const { clusterId } = this.props.match.params;

    acls = await this.getApi(uriAclsList(clusterId, this.state.searchData.search));
    this.handleData(acls.data);
  }

  handleData(acls) {
    let tableAcls = acls.map(acl => {
      acl.principalEncoded = btoa(acl.principal);
      return {
        id: acl,
        user: acl.principal || ''
      };
    });
    this.setState({ data: tableAcls, loading: false });
    return tableAcls;
  }

  handleSearch = data => {
    const { searchData } = data;
    this.setState({ searchData, loading: true }, () => {
      this.getAcls();
    });
  };

  render() {
    const { data, searchData, loading } = this.state;
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
          loading={loading}
          history={this.props.history}
          columns={[
            {
              id: 'user',
              accessor: 'user',
              colName: 'Principals',
              type: 'text',
              sortable: true
            }
          ]}
          actions={[constants.TABLE_DETAILS]}
          data={data}
          updateData={data => {
            this.setState({ data });
          }}
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
          onDetails={acl => `/ui/${clusterId}/acls/${acl.principalEncoded}`}
        />
      </div>
    );
  }
}

export default Acls;
