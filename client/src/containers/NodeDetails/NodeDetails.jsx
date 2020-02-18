import React, { Component } from 'react';
import Header from '../Header';

class NodeDetails extends Component {
  state = {
    host: '',
    port: ''
  };

  componentDidMount() {}

  render() {
    const { host, port } = this.state;
    const { nodeId } = this.props.match.params;
    return (
      <div>
        <div id="content" style={{ height: '100%' }}>
          <Header title={`Node: ${nodeId}`} />
        </div>

        <div class="tabs-container">
          <ul class="nav nav-tabs" role="tablist">
            <li class="nav-item">
              <a class="nav-link active" href="/my-cluster-plain-text/node/1002" role="tab">
                Configs
              </a>
            </li>
            <li class="nav-item">
              <a class="nav-link " href="/my-cluster-plain-text/node/1002/logs" role="tab">
                Logs
              </a>
            </li>
          </ul>

          <div class="tab-content">
            <div class="tab-pane active" role="tabpanel">
              <form enctype="multipart/form-data" method="post" class="khq-form mb-0">
                <div class="table-responsive">
                  <table
                    class="table table-bordered table-striped table-hover 
        mb-0 khq-form-config"
                  >
                    <thead class="thead-dark">
                      <tr>
                        <th>Name</th>
                        <th>Value</th>
                        <th>Type</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td>
                          {/* hardcorded value  */}
                          advertised.listeners
                        </td>
                        <td>
                          <input
                            type="text"
                            class="form-control"
                            autocomplete="off"
                            name="configs[advertised.listeners]"
                            value="PLAINTEXT://kafka:9092"
                          />
                          <small class="humanize form-text text-muted"></small>
                        </td>
                        <td>
                          <span class="badge badge-warning">
                            {/* hardcoded value */}
                            STATIC_BROKER_CONFIG
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default NodeDetails;
