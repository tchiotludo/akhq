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

  handleData(data) {
    let tableAcls = [];
    data.map(principal =>
      principal.acls.map((acl, index) => {
        tableAcls.push({
          id: index,
          topic: acl.resource.name || '',
          host: acl.host || '',
          permission: acl.operation || ''
        });
      })
    );
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
              id: 'permission',
              accessor: 'permission',
              colName: 'Permissions',
              type: 'text',
              cell: (obj, col) => {
                return (
                  <React.Fragment>
                    <span className="badge badge-secondary">
                      {obj[col.accessor].permissionType}
                    </span>{' '}
                    {obj[col.accessor].operation}
                  </React.Fragment>
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
