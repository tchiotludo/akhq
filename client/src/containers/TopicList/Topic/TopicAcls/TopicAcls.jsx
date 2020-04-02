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
    this.getNodes();
  }

  async getNodes() {
    let acls = [];
    const { clusterId, topicId } = this.props;
    const { history } = this.props;
    history.push({
      loading: true
    });
    try {
      //acls = await get(uriTopicsAcls(clusterId, topicId));
      //this.handleData(acls.data);
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
      let hostProp = Object.keys(acl.permissions.topic)[0];
      return {
        id: index,
        topic: acl.principal || ''
        //port: node.port || '',
        //rack: node.rack || ''
      };
    });
    //this.setState({ data: tableAcls });
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
              id: 'id',
              accessor: 'id',
              colName: 'Id',
              type: 'text',
              cell: (obj, col) => {
                return <span className="badge badge-info">{obj[col.accessor] || ''}</span>;
              }
            },
            {
              id: 'host',
              accessor: 'host',
              colName: 'Host',
              type: 'text'
            },
            {
              id: 'racks',
              accessor: 'rack',
              colName: 'Racks',
              type: 'text'
            }
          ]}
          data={data}
          actions={[constants.TABLE_DETAILS]}
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
