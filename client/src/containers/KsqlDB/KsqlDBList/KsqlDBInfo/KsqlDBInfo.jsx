import React from 'react';
import Table from '../../../../components/Table/Table';
import { uriKsqlDBInfo } from '../../../../utils/endpoints';
import 'react-toastify/dist/ReactToastify.css';
import Root from '../../../../components/Root';
import { withRouter } from '../../../../utils/withRouter';

class KsqlDBInfo extends Root {
  state = {
    clusterId: this.props.clusterId || this.props.params.clusterId,
    ksqlDBId: this.props.ksqlDBId || this.props.params.ksqlDBId,
    info: {},
    tableData: [],
    loading: true
  };

  componentDidMount() {
    this.getInfo();
  }

  handleInfo() {
    const info = this.state.info || {};
    let tableData = [
      {
        title: 'Server version',
        value: info['serverVersion']
      },
      {
        title: 'Kafka cluster id',
        value: info['kafkaClusterId']
      },
      {
        title: 'Ksql service id',
        value: info['ksqlServiceId']
      }
    ];
    this.setState({ tableData, loading: false });
  }

  async getInfo() {
    const { clusterId, ksqlDBId } = this.state;

    this.setState({ loading: true });
    const info = await this.getApi(uriKsqlDBInfo(clusterId, ksqlDBId));
    this.setState({ info: info.data }, () => this.handleInfo());
  }

  render() {
    const { tableData, loading } = this.state;
    return (
      <div className="tab-pane active" role="tabpanel">
        <div className="table-responsive">
          <Table
            loading={loading}
            columns={[
              {
                id: 'title',
                name: 'title',
                accessor: 'title',
                colName: 'Title',
                type: 'text',
                sortable: false
              },
              {
                id: 'value',
                name: 'value',
                accessor: 'value',
                colName: 'Value',
                type: 'text',
                sortable: false
              }
            ]}
            extraRow
            noStripes
            data={tableData}
          />
        </div>
      </div>
    );
  }
}

export default withRouter(KsqlDBInfo);
