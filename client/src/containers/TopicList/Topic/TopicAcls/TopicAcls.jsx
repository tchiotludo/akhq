import React, { Component } from 'react';
import Table from '../../../../components/Table';
import * as constants from '../../../../utils/constants';
import { get } from '../../../../utils/api';
import { uriTopicsAcls } from '../../../../utils/endpoints';

class TopicAcls extends Component {
  state = {
    data: [],
    selectedCluster: ''
  };

  componentDidMount() {
    this.getAcls();
  }

  async getAcls() {
    let acls = [];
    const { clusterId, topicId } = this.props;
    const { history } = this.props;
    history.replace({
      loading: true
    });
    try {
      acls = await get(uriTopicsAcls(clusterId, topicId));
      this.handleData(acls.data);
    } catch (err) {
      history.replace('/error', { errorData: err });
    } finally {
      history.replace({
        loading: false
      });
    }
  }

  handleData(data) {
    let tableAcls = data.map((acl, index) => {
      return {
        id: index,
        user: acl.principal || '',
        host: acl.acls[0].host || '',
        permission: acl.acls[0].operation.operation || ''
      };
    });
    this.setState({ data: tableAcls });
    return tableAcls;
  }

  render() {
    const { history } = this.props;
    const { data, selectedCluster } = this.state;
    return (
      <div>
        <Table
          columns={[
            {
              id: 'user',
              accessor: 'user',
              colName: 'User',
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
              colName: 'Permissions',
              type: 'text',
              cell: (obj, col) => {
                return (
                  <div>
                    <span class="badge badge-secondary">{obj[col.accessor]}</span>
                  </div>
                );
              }
            }
          ]}
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
        />
      </div>
    );
  }
}

export default TopicAcls;
