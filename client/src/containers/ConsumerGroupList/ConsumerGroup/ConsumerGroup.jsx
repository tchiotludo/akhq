import React, { Component } from 'react';
import Header from '../../Header';
import SearchBar from '../../../components/SearchBar';
import Pagination from '../../../components/Pagination';
import Table from '../../../components/Table/Table';
import ConsumerGroupTopics from './ConsumerGroupTopics/ConsumerGroupTopics';
import ConsumerGroupMembers from './ConsumerGroupMembers/ConsumerGroupMembers';
import history from '../../../utils/history';
import { Link } from 'react-router-dom';

class ConsumerGroup extends Component {
  state = {
    clusterId: '',
    consumerGroupId: '',
    consumerGroup: {},
    selectedTab: 'topics'
  };

  componentDidMount() {
    const { clusterId, consumerGroupId } = this.props.match.params;

    this.setState({ clusterId, consumerGroupId });
  }

  selectTab = tab => {
    this.setState({ selectedTab: tab });
  };

  tabClassName = tab => {
    const { selectedTab } = this.state;
    return selectedTab === tab ? 'nav-link active' : 'nav-link';
  };

  renderSelectedTab() {
    const { selectedTab } = this.state;
    const { history } = this.props;
    const { clusterId, consumerGroupId } = this.props.match.params;
    console.log('antes do switch', clusterId, consumerGroupId);
    switch (selectedTab) {
      case 'topics':
        return (
          <ConsumerGroupTopics
            clusterId={clusterId}
            consumerGroupId={consumerGroupId}
            history={history}
          />
        );
      case 'members':
        return (
          <ConsumerGroupMembers
            clusterId={clusterId}
            consumerGroupId={consumerGroupId}
            history={history}
          />
        );

      default:
        return (
          <ConsumerGroupTopics
            clusterId={clusterId}
            consumerGroupId={consumerGroupId}
            history={history}
          />
        );
    }
  }

  render() {
    const { consumerGroupId } = this.state;
    return (
      <div id="content">
        <Header title={`Topic ${consumerGroupId}`} />
        <div className="tabs-container">
          <ul className="nav nav-tabs" role="tablist">
            <li className="nav-item">
              <Link
                className={this.tabClassName('topics')}
                onClick={() => this.selectTab('topics')}
                to="#"
                role="tab"
              >
                Topics
              </Link>
            </li>
            <li className="nav-item">
              <Link
                className={this.tabClassName('members')}
                onClick={() => this.selectTab('members')}
                to="#"
                role="tab"
              >
                Members
              </Link>
            </li>
          </ul>

          <div className="tab-content">
            <div className="tab-pane active" role="tabpanel">
              {this.renderSelectedTab()}
            </div>
          </div>
        </div>

        <aside>
          <Link to="#" className="btn btn-primary">
            <i className="fa fa-plus" aria-hidden={true} /> Update Offsets
          </Link>
        </aside>
      </div>
    );
  }
}

export default ConsumerGroup;
