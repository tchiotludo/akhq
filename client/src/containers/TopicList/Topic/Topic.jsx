import React, { Component } from 'react';
import { Link } from 'react-router-dom';

import Header from '../../Header';
import TopicData from './TopicData';
import TopicPartitions from './TopicPartitions';
import TopicGroups from './TopicGroups';
import TopicConfigs from './TopicConfigs';
import TopicAcls from './TopicAcls';
import TopicLogs from './TopicLogs';

// Adaptation of topic.ftl

class Topic extends Component {
  state = {
    clusterId: '',

    topic: {
      _id: Date.now(),
      name: 'test',
      partition: 1,
      replication: 1,
      cleanup: 'delete',
      retention: 50
    },

    selectedTab: 'data'
  };

  componentDidMount() {
    const { clusterId, topicId } = this.props.match.params;

    this.setState({ clusterId });
  }

  selectTab = tab => {
    this.setState({ selectedTab: tab });
  };

  tabClassName = tab => {
    const { selectedTab } = this.state;
    return selectedTab === tab ? 'nav-link active' : 'nav-link';
  };

  renderSelectedTab() {
    const { selectedTab, topic } = this.state;

    switch (selectedTab) {
      case 'data':
        return <TopicData topic={topic} />;
      case 'partitions':
        return <TopicPartitions />;
      case 'groups':
        return <TopicGroups />;
      case 'configs':
        return <TopicConfigs />;
      case 'acls':
        return <TopicAcls />;
      case 'logs':
        return <TopicLogs />;
      default:
        return <TopicData />;
    }
  }

  render() {
    const { topic } = this.state;
    return (
      <div id="content">
        <Header title={`Topic ${topic.name}`} />

        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            {/*#if roles?seq_contains("topic/data/read")*/}
            <li className="nav-item">
              {/*<a className="nav-link ${(tab == "data") ? then("active", "")}"*/}
              <Link
                className={this.tabClassName('data')}
                onClick={() => this.selectTab('data')}
                to="#"
                role="tab"
              >
                Data
              </Link>
            </li>
            {/*</#if>*/}
            <li className="nav-item">
              {/*<a className="nav-link ${(tab == " partitions") ? then(" active", "")}"*/}
              <Link
                className={this.tabClassName('partitions')}
                onClick={() => this.selectTab('partitions')}
                to="#"
                role="tab"
              >
                Partitions
              </Link>
            </li>
            <li className="nav-item">
              {/*<a className="nav-link ${(tab == " groups") ? then(" active", "")}"*/}
              <Link
                className={this.tabClassName('groups')}
                onClick={() => this.selectTab('groups')}
                to="#"
                role="tab"
              >
                Consumer Groups
              </Link>
            </li>
            <li className="nav-item">
              {/*<a className="nav-link ${(tab == " configs") ? then(" active", "")}"*/}
              <Link
                className={this.tabClassName('configs')}
                onClick={() => this.selectTab('configs')}
                to="#"
                role="tab"
              >
                Configs
              </Link>
            </li>
            {/*#if roles?seq_contains(" acls") == true*/}
            <li className="nav-item">
              {/*<a className="nav-link ${(tab == " acls") ? then(" active", "")}"*/}
              <Link
                className={this.tabClassName('acls')}
                onClick={() => this.selectTab('acls')}
                to="#"
                role="tab"
              >
                ACLS
              </Link>
            </li>
            {/*</#if>*/}
            <li className="nav-item">
              {/*<a className="nav-link ${(tab == " logs") ? then(" active", "")}"*/}
              <Link
                className={this.tabClassName('logs')}
                onClick={() => this.selectTab('logs')}
                to="#"
                role="tab"
              >
                Logs
              </Link>
            </li>
          </ul>

          <div className="tab-content">
            <div className="tab-pane active" role="tabpanel">
              {this.renderSelectedTab()}
            </div>
            {/*/!*<#if tab == " data">*!/*/}
            {/*<div className="tab-pane active" role=" tabpanel">*/}
            {/*    /!*<#include " blocks/topic/data.ftl" />*!/*/}
            {/*</div>*/}
            {/*/!*</#if>*!/*/}

            {/*/!*<#if tab == " partitions">*!/*/}
            {/*<div className="tab-pane active" role=" tabpanel">*/}
            {/*    /!*<#include " blocks/topic/partitions.ftl" />*!/*/}
            {/*</div>*/}
            {/*/!*</#if>*!/*/}

            {/*/!*<#if tab == " groups">*!/*/}
            {/*<div className="tab-pane active" role=" tabpanel">*/}
            {/*    /!*<@groupTemplate.table topic.getConsumerGroups() />*!/*/}
            {/*</div>*/}
            {/*/!*</#if>*!/*/}

            {/*/!*<#if tab == " configs">*!/*/}
            {/*<div className="tab-pane active" role=" tabpanel">*/}
            {/*    /!*<#include " blocks/configs.ftl" />*!/*/}
            {/*</div>*/}
            {/*/!*</#if>*!/*/}

            {/*/!*#if tab == " acls" && roles?seq_contains(" acls") == true*!/*/}
            {/*<div className="tab-pane active" role=" tabpanel">*/}
            {/*    /!*<#assign resourceType=" topic"/>*!/*/}
            {/*    /!*<#include " blocks/resourceTypeAcls.ftl" />*!/*/}
            {/*</div>*/}
            {/*/!*</#if>*!/*/}

            {/*/!*<#if tab == " logs">*!/*/}
            {/*<div className="tab-pane active" role=" tabpanel">*/}
            {/*    /!*<@logTemplate.table topic.getLogDir() />*!/*/}
            {/*</div>*/}
            {/*/!*</#if>*!/*/}
          </div>
        </div>

        <aside>
          <Link to="#" className="btn btn-secondary mr-2">
            <i className="fa fa-fw fa-level-down" aria-hidden={true} /> Live Tail
          </Link>
          <Link to="#" className="btn btn-primary">
            <i className="fa fa-plus" aria-hidden={true} /> Produce to topic
          </Link>
        </aside>
      </div>
    );
  }
}

// <#if tab != " configs" && roles?seq_contains(" topic/data/insert")>
//     <@template.bottom>
//         <a href=" ${basePath}/${clusterId}/tail/?topics= ${topic.getName()}" className="btn btn-secondary mr-2">
//             <i className="fa fa-fw fa-level-down" aria-hidden=" true"></i> Live Tail
//         </a>
//
//         <a href=" ${basePath}/${clusterId}/topic/${topic.getName()}/produce" className="btn btn-primary">
//             <i className="fa fa-plus" aria-hidden=" true"></i> Produce to topic
//         </a>
//     </@template.bottom>
// </#if>
//         );
//     }
// }

export default Topic;
