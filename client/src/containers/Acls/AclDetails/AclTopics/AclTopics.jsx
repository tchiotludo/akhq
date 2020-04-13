import React, { Component } from 'react';
import './styles.scss';

import Table from '../../../../components/Table/Table';
import { get } from '../../../../utils/api';
import { uriAclsByPrincipal } from '../../../../utils/endpoints';
import _ from 'lodash';

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

    history.push({
      loading: true
    });

    try {
      const response = await get(uriAclsByPrincipal(selectedCluster, principalEncoded, 'TOPIC'));
      if (response.data.acls) {
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

  handleAcls = data => {
    const tableData = data.acls.map(acl => {
      return {
        topic: `${_.toLower(acl.resource.patternType)}:${acl.resource.name}`,
        host: acl.host,
        permission: acl.operation.operation
      };
    });

    this.setState({ tableData });
  };

  handlePermission = permission => {
    return (
      <h5 key={permission}>
        <span className="badge badge-secondary">{permission}</span>
      </h5>
    );
  };

  render() {
    return (
      <Table
        columns={[
          {
            id: 'topic',
            accessor: 'topic',
            colName: 'Topic',
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
