import React from 'react';
import Table from '../../../../components/Table/Table';
import 'ace-builds/webpack-resolver';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-merbivore_soft';
import 'react-toastify/dist/ReactToastify.css';
import Root from '../../../../components/Root';
import SearchBar from '../../../../components/SearchBar';
import Pagination from '../../../../components/Pagination';
import { handlePageChange, getPageNumber } from '../../../../utils/pagination';
import { uriKsqlDBQueries } from '../../../../utils/endpoints';
import AceEditor from 'react-ace';
import { Link } from 'react-router-dom';
import { withRouter } from '../../../../utils/withRouter';

class KsqlDBQueries extends Root {
  state = {
    clusterId: '',
    ksqlDBId: '',
    tableData: [],
    loading: true,
    pageNumber: 1,
    totalPageNumber: 1,
    history: this.props,
    searchData: {
      search: ''
    }
  };

  static getDerivedStateFromProps(nextProps) {
    const clusterId = nextProps.params.clusterId;
    const ksqlDBId = nextProps.params.ksqlDBId;

    return {
      clusterId: clusterId,
      ksqlDBId: ksqlDBId
    };
  }

  componentDidMount() {
    const { searchData, pageNumber } = this.state;
    const query = new URLSearchParams(this.props.location.search);
    this.setState(
      {
        searchData: { search: query.get('search') ? query.get('search') : searchData.search },
        pageNumber: query.get('page') ? parseInt(query.get('page')) : parseInt(pageNumber)
      },
      () => {
        this.getKsqlDBQueries();
      }
    );
  }

  componentDidUpdate(prevProps) {
    if (this.props.location.pathname !== prevProps.location.pathname) {
      this.cancelAxiosRequests();
      this.renewCancelToken();

      this.setState({ pageNumber: 1 }, () => {
        this.componentDidMount();
      });
    }
  }

  async getKsqlDBQueries() {
    const { clusterId, ksqlDBId, pageNumber } = this.state;
    const { search } = this.state.searchData;

    this.setState({ loading: true });

    let response = await this.getApi(uriKsqlDBQueries(clusterId, ksqlDBId, search, pageNumber));
    let data = response.data;
    if (data.results) {
      this.handleData(data);
      this.setState({ selectedCluster: clusterId, totalPageNumber: data.page }, () => {
        this.props.history.push({
          pathname: `/ui/${this.state.clusterId}/ksqldb/${this.state.ksqlDBId}/queries`,
          search: `search=${this.state.searchData.search}&page=${pageNumber}`
        });
      });
    } else {
      this.setState({ clusterId, tableData: [], totalPageNumber: 0, loading: false });
    }
  }

  handleData = data => {
    const tableData = data.results.map(query => {
      return {
        id: query.id || '',
        queryType: query.queryType || '',
        sink: query.sink || '',
        sinkTopic: query.sinkTopic || '',
        sql: query.sql || ''
      };
    });

    this.setState({ tableData, loading: false, totalPageNumber: data.page });
  };

  handleSearch = data => {
    const { searchData } = data;
    this.setState({ pageNumber: 1, searchData }, () => {
      this.getKsqlDBQueries();
    });
  };

  handlePageChangeSubmission = value => {
    let pageNumber = getPageNumber(value, this.state.totalPageNumber);
    this.setState({ pageNumber: pageNumber }, () => {
      this.getKsqlDBQueries();
    });
  };

  render() {
    const { clusterId, tableData, loading, searchData, pageNumber, totalPageNumber } = this.state;
    const { history } = this.props;
    return (
      <div>
        <nav className="navbar navbar-expand-l mr-auto khq-data-filter khq-sticky khq-nav">
          <SearchBar
            showSearch={true}
            search={searchData.search}
            showPagination={true}
            pagination={pageNumber}
            doSubmit={this.handleSearch}
          />

          <Pagination
            pageNumber={pageNumber}
            totalPageNumber={totalPageNumber}
            onChange={handlePageChange}
            onSubmit={this.handlePageChangeSubmission}
          />
        </nav>

        <Table
          loading={loading}
          history={history}
          columns={[
            {
              id: 'id',
              name: 'id',
              accessor: 'id',
              colName: 'Id',
              type: 'text',
              sortable: true
            },
            {
              id: 'queryType',
              name: 'queryType',
              accessor: 'queryType',
              colName: 'Query type',
              type: 'text',
              sortable: true
            },
            {
              id: 'sink',
              name: 'sink',
              accessor: 'sink',
              colName: 'Sink',
              type: 'text',
              sortable: true
            },
            {
              id: 'sinkTopic',
              name: 'sinkTopic',
              accessor: 'sinkTopic',
              colName: 'Sink topic',
              type: 'text',
              sortable: true,
              cell: (obj, col) => {
                return obj[col.accessor] ? (
                  <Link to={`/ui/${clusterId}/topic/${obj[col.accessor]}`}>
                    {obj[col.accessor]}
                  </Link>
                ) : null;
              }
            },
            {
              id: 'sql',
              name: 'sql',
              accessor: 'sql',
              colName: 'SQL',
              type: 'text',
              extraRow: true,
              extraRowContent: (obj, col, index) => {
                return (
                  <AceEditor
                    mode="sql"
                    id={'value' + index}
                    theme="merbivore_soft"
                    value={obj[col.accessor]}
                    readOnly
                    name="UNIQUE_ID_OF_DIV"
                    editorProps={{ $blockScrolling: true }}
                    style={{ width: '100%', minHeight: '25vh' }}
                  />
                );
              }
            }
          ]}
          data={tableData}
          updateData={data => {
            this.setState({ tableData: data });
          }}
          extraRow
          noStripes
          noContent={'No queries available'}
        />
      </div>
    );
  }
}

export default withRouter(KsqlDBQueries);
