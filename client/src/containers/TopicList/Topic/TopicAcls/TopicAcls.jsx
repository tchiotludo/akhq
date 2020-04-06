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
    history.push({
      loading: true
    });
    try {
      acls = await get(uriTopicsAcls(clusterId, topicId));
      this.handleData(acls.data);
    } catch (err) {
      history.replace('/error', { errorData: err });
    } finally {
      history.push({
        loading: false
      });
    }
  }

  handleData(acls) {
    let tableAcls = acls.map((acl, index) => {
      return {
        id: index,
        user: acl.user || '',
        host: acl.host || '',
        permissions: acl.permissions || ''
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
              id: 'permissions',
              accessor: 'permissions',
              colName: 'Permissions',
              type: 'text',
              cell: obj => {
                return (
                  <div>
                    {obj.permissions.map(el => {
                      return <span class="badge badge-secondary">{el}</span>;
                    })}
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
          onDetails={id => {
            history.push(`/${selectedCluster}/node/${id}`);
          }}
        />
      </div>
    );
  }
}

export default TopicAcls;
