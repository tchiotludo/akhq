import React from 'react';
import PropTypes from 'prop-types';
import Sidebar from '../../containers/SideBar';
import constants from '../../utils/constants';
import Loading from '../../containers/Loading';
import Root from '../../components/Root';
import { withRouter } from '../../utils/withRouter';

class Base extends Root {
  state = {
    clusterId: '',
    topicId: '',
    selectedTab: constants.CLUSTER, //cluster | node | topic | tail | group | acls | schema | connect
    action: '',
    loading: false,
    expanded: !!localStorage.getItem('expanded')
  };

  static getDerivedStateFromProps(nextProps) {
    const clusterId = nextProps ? nextProps.params.clusterId : '';
    const topicId = nextProps ? nextProps.params.topicId : '';
    const action = nextProps ? nextProps.params.action : '';
    const loading = nextProps.location ? nextProps.location.loading : false;
    const tab = nextProps.location ? nextProps.location.tab : constants.CLUSTER;
    return {
      topicId: topicId,
      clusterId: clusterId,
      selectedTab: tab,
      action: action,
      loading
    };
  }

  getTitleContext(pathname) {
    const segments = pathname.split('/').filter(Boolean);
    const section = segments[2] || '';
    const decodeSegment = value => {
      if (!value) return '';
      try {
        return decodeURIComponent(value);
      } catch (e) {
        return value;
      }
    };
    switch (section) {
      case 'topic':
        return { label: 'Topics', id: decodeSegment(segments[3]) };
      case 'node':
        return { label: 'Nodes', id: decodeSegment(segments[3]) };
      case 'tail':
        return { label: 'Live Tail', id: '' };
      case 'group':
        return { label: 'Consumer Groups', id: decodeSegment(segments[3]) };
      case 'acls':
        return { label: 'Acls', id: decodeSegment(segments[3]) };
      case 'schema': {
        const schemaId = segments[3] === 'details' ? segments[4] : '';
        return { label: 'Schema Registry', id: decodeSegment(schemaId) };
      }
      case 'connect': {
        const connectId = segments[3];
        const definitionId = segments[4] === 'definition' ? segments[5] : '';
        return { label: 'Connect', id: decodeSegment(definitionId || connectId) };
      }
      case 'ksqldb':
        return { label: 'KsqlDB', id: decodeSegment(segments[3]) };
      default:
        return { label: '', id: '' };
    }
  }

  handleTitle() {
    const page = window.location.pathname;
    const { label, id } = this.getTitleContext(page);
    const title = label ? (id ? `${id} | ${label} |` : `${label} |`) : '';
    return title + ' akhq.io';
  }

  componentWillUnmount() {
    clearTimeout(this.interval);
  }

  render() {
    const { children, clusters } = this.props;
    const { loading, selectedTab, expanded } = this.state;
    return (
      <>
        <title>{this.handleTitle()}</title>
        <Loading show={loading} />
        {this.props.location.pathname !== '/ui/login' &&
          this.props.location.pathname !== '/ui/page-not-found' && (
            <Sidebar
              clusters={clusters}
              expanded={expanded}
              toggleSidebar={newExpanded => {
                newExpanded
                  ? localStorage.setItem('expanded', newExpanded)
                  : localStorage.removeItem('expanded');
                this.setState({ expanded: newExpanded });
              }}
              selectedTab={selectedTab}
            />
          )}
        <div id="content" className={expanded ? 'expanded' : 'collapsed'}>
          {children}
        </div>
      </>
    );
  }
}

Base.propTypes = {
  location: PropTypes.object,
  clusters: PropTypes.array,
  children: PropTypes.any
};

export default withRouter(Base);
