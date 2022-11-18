import React from 'react';
import Joi from 'joi-browser';
import PropTypes from 'prop-types';
import Form from '../Form/Form';
import { SETTINGS_VALUES } from '../../utils/constants';
import './styles.scss';

class SearchBar extends Form {
  static propTypes = {
    showPagination: PropTypes.bool,
    showTopicListView: PropTypes.bool,
    showSearch: PropTypes.bool,
    showFilters: PropTypes.string,
    showKeepSearch: PropTypes.bool
  };
  state = {
    formData: {},
    errors: {},
    topicListViewOptions: [
      {
        _id: SETTINGS_VALUES.TOPIC.TOPIC_DEFAULT_VIEW.ALL,
        name: 'Show all topics'
      },
      {
        _id: SETTINGS_VALUES.TOPIC.TOPIC_DEFAULT_VIEW.HIDE_INTERNAL,
        name: 'Hide internal topics'
      },
      {
        _id: SETTINGS_VALUES.TOPIC.TOPIC_DEFAULT_VIEW.HIDE_INTERNAL_STREAM,
        name: 'Hide internal & stream topics'
      },
      {
        _id: SETTINGS_VALUES.TOPIC.TOPIC_DEFAULT_VIEW.HIDE_STREAM,
        name: 'Hide stream topics'
      }
    ]
  };

  schema = {};

  componentDidMount() {
    this.setState({ formData: this.setupProps() });
  }

  componentDidUpdate(prevProps) {
    const { search, topicListView, keepSearch } = this.props;

    if (
      search !== prevProps.search ||
      topicListView !== prevProps.topicListView ||
      keepSearch !== prevProps.keepSearch
    ) {
      this.setupProps();
    }
  }

  setupProps() {
    const { showSearch, showTopicListView, showKeepSearch } = this.props;
    const { formData } = this.state;
    if (showSearch) {
      const { search } = this.props;
      formData['search'] = search;
      this.schema['search'] = Joi.string().allow('');
    }
    if (showTopicListView) {
      const { topicListView } = this.props;
      formData['topicListView'] = topicListView;
      this.schema['topicListView'] = Joi.string().required();
    }
    if (showKeepSearch) {
      formData['keepSearch'] = this.props.keepSearch;
      this.schema['keepSearch'] = Joi.boolean();
    }
    return formData;
  }

  setValue(value) {
    this.setState({ value }, () => {
      this.props.onChangeValue(value);
    });
  }

  setTopic(topic) {
    this.setState({ topic }, () => {
      this.props.onChangeTopic(topic);
    });
  }

  doSubmit = () => {
    const { pagination, search, topicListView, keepSearch } = this.state.formData;
    const data = {
      pagination: pagination,
      searchData: {
        search: search,
        topicListView: topicListView
      },
      keepSearch: keepSearch
    };
    this.props.doSubmit(data);
  };
  openAndCloseFilters() {
    let { showFilters } = this.state;
    if (showFilters === 'show') {
      this.setState({ showFilters: '' });
    } else {
      this.setState({ showFilters: 'show' });
    }
  }

  render() {
    const { showSearch, showTopicListView, showKeepSearch } = this.props;
    const { topicListViewOptions, showFilters, formData } = this.state;

    return (
      <React.Fragment>
        <button
          className="navbar-toggler"
          type="button"
          data-toggle="collapse"
          data-target="#navbar-search"
          aria-controls="navbar-search"
          aria-expanded="false"
          aria-label="Toggle navigation"
          onClick={() => {
            this.openAndCloseFilters();
          }}
        >
          <span className="navbar-toggler-icon" />
        </button>
        <div className={`collapse navbar-collapse ${showFilters}`} id="navbar-search">
          <form className="form-inline mr-auto khq-form-get" onSubmit={e => this.handleSubmit(e)}>
            {showSearch &&
              this.renderInput(
                'search',
                '',
                'Search',
                'text',
                { autoComplete: 'off' },
                '',
                'topic-search-wrapper',
                'topic-search-input'
              )}
            {showTopicListView &&
              this.renderSelect(
                'topicListView',
                '',
                topicListViewOptions,
                ({ currentTarget: input }) => {
                  let { formData } = this.state;
                  formData.topicListView = input.value;
                  this.setState();
                  this.props.onTopicListViewChange(input.value);
                },
                '',
                'select-wrapper',
                { className: 'form-control topic-select' }
              )}

            <button className="btn btn-primary" type="submit">
              <span className="d-md-none">Search </span>
              <i className="fa fa-search" />
            </button>
            {showKeepSearch && (
              <span>
                <input
                  type="checkbox"
                  name="keepSearch"
                  id="keepSearch"
                  onClick={event => this.props.onKeepSearchChange(event.target.checked)}
                  defaultChecked={formData['keepSearch']}
                />{' '}
                Keep search
              </span>
            )}
          </form>
        </div>
      </React.Fragment>
    );
  }
}

export default SearchBar;
