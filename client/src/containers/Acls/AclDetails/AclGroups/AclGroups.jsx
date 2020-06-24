import React, { Component } from 'react';
import './styles.scss';

import Table from '../../../../components/Table/Table';
import { get } from '../../../../utils/api';
import { uriAclsByPrincipal } from '../../../../utils/endpoints';

class AclTopics extends Component {
  state = {
    selectedCluster: this.props.clusterId,
    principalEncoded: this.props.principalEncoded,
    tableData: []
  };

  componentDidMount() {
    this.getAcls();
  }

  async getAcls() {
    const { history } = this.props;
    const { selectedCluster, principalEncoded } = this.state;

    history.replace({
      loading: true
    });

    try {
      const response = await get(uriAclsByPrincipal(selectedCluster, principalEncoded, 'GROUP'));
      if (response.data.acls) {
        const acls = response.data || [];
        this.handleAcls(acls);
      }
    } catch (err) {
      if (err.status === 404) {
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

  handleAcls = data => {
    const tableData = data.acls.map(acl => {
      return {
        group: acl.resource.name,
        host: acl.host,
        permission: acl.operation
      };
    });

    this.setState({ tableData });
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
    return (
      <Table
        columns={[
          {
            id: 'group',
            accessor: 'group',
            colName: 'Group',
            type: 'text'
          },
          {
            id: 'host',
            accessor: 'host',
            colName: 'Host',
            type: 'text'
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
        noContent={
          'No ACLS found, or the "authorizer.class.name" parameter is not configured on the cluster.'
        }
      />
    );
  }
}

export default AclTopics;
