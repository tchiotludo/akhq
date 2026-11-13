import React from 'react';
import Header from '../../Header';
import Table from '../../../components/Table';
import * as constants from '../../../utils/constants';
import { uriNodes, uriNodePartitions, uriNodeQuorum } from '../../../utils/endpoints';
import Root from '../../../components/Root';
import { withRouter } from '../../../utils/withRouter';
import MetadataQuorum from './MetadataQuorum';
import { NodeRoles, NodeRoleSummary } from './NodeRoles';

export class NodesList extends Root {
  requestGeneration = 0;

  state = {
    data: [],
    selectedCluster: '',
    loading: true,
    quorum: null,
    quorumLoading: true,
    nodeError: false
  };

  componentDidMount() {
    this.getNodes();
  }

  componentDidUpdate(prevProps) {
    if (prevProps.params.clusterId !== this.props.params.clusterId) this.getNodes();
  }

  componentWillUnmount() {
    this.requestGeneration += 1;
    super.componentWillUnmount();
  }

  async getNodes() {
    const { clusterId } = this.props.params;
    const generation = ++this.requestGeneration;
    this.setState({
      selectedCluster: clusterId,
      data: [],
      quorum: null,
      loading: true,
      quorumLoading: true,
      nodeError: false
    });

    // Quorum failures must not hide the broker list or its details links.
    const quorumRequest = this.getApi(uriNodeQuorum(clusterId))
      .then(result => {
        if (generation === this.requestGeneration)
          this.setState({ quorum: result.data, quorumLoading: false });
      })
      .catch(() => {
        if (generation === this.requestGeneration)
          this.setState({ quorum: { status: 'UNAVAILABLE' }, quorumLoading: false });
      });
    const nodesRequest = this.getApi(uriNodes(clusterId))
      .then(result => {
        if (generation === this.requestGeneration) return this.handleData(result.data, generation);
      })
      .catch(() => {
        if (generation === this.requestGeneration)
          this.setState({ loading: false, nodeError: true });
      });
    await Promise.all([quorumRequest, nodesRequest]);
  }

  async handleData(nodes, generation = this.requestGeneration) {
    const { clusterId } = this.props.params;
    let tableNodes = {};
    const setState = () => {
      if (generation === this.requestGeneration)
        this.setState({ data: Object.values(tableNodes), loading: false });
    };

    nodes.nodes.forEach(node => {
      tableNodes[node.id] = {
        id: node.id,
        host: `${node.host}:${node.port}`,
        rack: node.rack || '',
        partition: undefined
      };
    });

    setState();

    await this.getApi(uriNodePartitions(clusterId))
      .then(value => {
        Object.values(tableNodes).forEach(node => {
          node.partition = '0 (0.00%)';
        });
        for (let node of value.data) {
          const topicNode = tableNodes[node.id];
          if (!topicNode) continue;
          topicNode.partition =
            node.totalPartitions > 0
              ? node.countLeader +
                ' (' +
                ((node.countLeader / node.totalPartitions) * 100).toFixed(2) +
                '%)'
              : '0 (0.00%)';
        }

        setState();
      })
      .catch(() => {
        // Partition statistics are optional; keep the broker and quorum information visible.
        Object.values(tableNodes).forEach(node => {
          node.partition = 'Unavailable';
        });
        setState();
      });

    return Object.values(tableNodes);
  }

  render() {
    const { data, selectedCluster, loading, quorum, quorumLoading, nodeError } = this.state;
    const brokerIds = loading || nodeError ? null : new Set(data.map(node => node.id));
    const controllerIds =
      quorum?.status === 'AVAILABLE' ? new Set(quorum.voters.map(voter => voter.id)) : null;
    return (
      <div>
        <Header title="Nodes" />
        <button
          type="button"
          className="btn btn-theme-secondary mb-3"
          onClick={() => this.getNodes()}
          disabled={loading || quorumLoading}
        >
          Refresh nodes
        </button>
        {nodeError && <p role="alert">Broker information could not be loaded.</p>}
        <NodeRoleSummary brokerIds={brokerIds} quorum={quorum} loading={loading || quorumLoading} />
        <h2>Brokers</h2>
        <p className="small text-body-secondary">
          Partition leaders are elected per topic partition, independently of the active controller.
        </p>
        <Table
          loading={loading}
          columns={[
            {
              id: 'id',
              accessor: 'id',
              colName: 'Id',
              type: 'text',
              sortable: true,
              cell: (obj, col) => {
                return <span className="badge bg-info">{obj[col.accessor] ?? ''}</span>;
              }
            },
            {
              id: 'roles',
              colName: 'Observed roles',
              type: 'text',
              sortable: false,
              cell: obj => <NodeRoles broker={true} controller={controllerIds?.has(obj.id)} />
            },
            {
              id: 'host',
              accessor: 'host',
              colName: 'Broker endpoint',
              type: 'text',
              sortable: true
            },
            {
              id: 'partition',
              accessor: 'partition',
              colName: 'Partition leaders (% of total)',
              type: 'text',
              sortable: false
            },
            {
              id: 'rack',
              accessor: 'rack',
              colName: 'Rack',
              type: 'text',
              sortable: true
            }
          ]}
          data={data}
          updateData={data => {
            this.setState({ data });
          }}
          actions={[constants.TABLE_DETAILS]}
          detailsHref={id => `/ui/${selectedCluster}/node/${id}`}
        />
        <MetadataQuorum quorum={quorum} loading={quorumLoading} brokerIds={brokerIds} />
      </div>
    );
  }
}

export default withRouter(NodesList);
