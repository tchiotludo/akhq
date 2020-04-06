import React, { Component } from 'react';
import './styles.scss';

import Table from '../../../../components/Table/Table';
import { get } from '../../../../utils/api';
import { uriAclsByPrincipal } from '../../../../utils/endpoints';

class AclGroups extends Component {
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
    const { selectedCluster, selectedConsumerGroup, principalEncoded } = this.state;

    history.push({
      loading: true
    });

    try {
      let response = await get(uriAclsByPrincipal(selectedCluster, principalEncoded, 'group'));
      if (response) {
        const acls = response.data || [];
        this.handleAcls(acls);
      }
    } catch (err) {
      history.replace('/error', { errorData: err });
    } finally {
      history.push({
        loading: false
      });
    }
  }

  handleAcls = acls => {
    const tableData = acls.map(acl => {
      return {
        group: acl.resource,
        host: acl.host,
        permissions: acl.permissions
      };
    });

    this.props.history.principal = acls[0].user;

    this.setState({ tableData });
  };

  handlePermissions = permissions => {
    return permissions.map(permission => {
      console.log(permission);
      return (
        <h5 key={permission}>
          <span className="badge badge-secondary">{permission}</span>
        </h5>
      );
    });
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
            id: 'permissions',
            accessor: 'permissions',
            colName: 'Permissions',
            type: 'text',
            cell: obj => {
              if (obj.permissions) {
                return <div>{this.handlePermissions(obj.permissions)}</div>;
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

export default AclGroups;
