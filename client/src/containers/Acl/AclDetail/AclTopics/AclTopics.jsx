import React, { Component } from 'react';

import Table from '../../../../components/Table/Table';
import { get } from '../../../../utils/api';
import { uriAclsByPrincipal } from '../../../../utils/endpoints';

class AclTopics extends Component {
  state = {
    selectedCluster: this.props.clusterId,
    principalEncoded: this.props.principalEncoded,
    tableData: [],
    loading: true
  };

  componentDidMount() {
    this.getAcls();
  }

  async getAcls() {
    const { selectedCluster, principalEncoded } = this.state;

    const response = await get(uriAclsByPrincipal(selectedCluster, principalEncoded, 'TOPIC'));
    if (response.data.acls) {
      const acls = response.data || [];
      this.handleAcls(acls);
    } else {
      this.setState({ tableData: [], loading: false });
    }
  }

  handleAcls = data => {
    const tableData = data.acls.map(acl => {
      return {
        topic: acl.resource.name,
        host: acl.host,
        permission: acl.operation
      };
    });

    this.setState({ tableData, loading: false });
  };

  handlePermission = permission => {
    return (
      <React.Fragment>
        <span className="badge badge-secondary">{permission.permissionType}</span>{' '}
        {permission.operation}
      </React.Fragment>
    );
  };

  render() {
    const { loading } = this.state;
    return (
      <Table
        loading={loading}
        columns={[
          {
            id: 'topic',
            accessor: 'topic',
            colName: 'Topic',
            type: 'text',
            sortable: true
          },
          {
            id: 'host',
            accessor: 'host',
            colName: 'Host',
            type: 'text',
            sortable: true
          },
          {
            id: 'permission',
            accessor: 'permission',
            colName: 'Permission',
            type: 'text',
            cell: obj => {
              if (obj.permission) {
                return <div>{this.handlePermission(obj.permission)}</div>;
              }
            }
          }
        ]}
        data={this.state.tableData}
        updateData={data => {
          this.setState({ tableData: data });
        }}
        noContent={
          'No ACLS found, or the "authorizer.class.name" parameter is not configured on the cluster.'
        }
      />
    );
  }
}

export default AclTopics;
