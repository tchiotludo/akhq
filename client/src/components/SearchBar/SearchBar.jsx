import React from 'react';
import Joi from 'joi-browser';
import PropTypes from 'prop-types';
import Form from '../Form/Form';
import './styles.scss';

class SearchBar extends Form {
  static propTypes = {
    showPagination: PropTypes.bool,
    showTopicListView: PropTypes.bool,
    showSearch: PropTypes.bool,
    showConsumerGroup: PropTypes.bool,
    showFilters: PropTypes.string
  };
  state = {
    formData: {},
    errors: {},
    topicListViewOptions: [
      {
        _id: 'ALL',
        name: 'Show all topics'
      },
      {
        _id: 'HIDE_INTERNAL',
        name: 'Hide internal topics'
      },
      {
        _id: 'HIDE_INTERNAL_STREAM',
        name: 'Hide internal & stream topics'
      },
      {
        _id: 'HIDE_STREAM',
        name: 'Hide stream topics'
      }
    ]
  };

  schema = {};

  componentDidMount() {
    const { showSearch, showTopicListView, showConsumerGroup } = this.props;
    const { formData, errors } = this.state;
    if (showSearch) {
      const { search } = this.props;
      formData['search'] = search;
      this.schema['search'] = Joi.string().allow('');
    }

    if (showTopicListView) {
      const { topicListView } = this.props;
      formData['topicListView'] = topicListView;
      this.schema['topicListView'] = Joi.string().required();
    } else if (showConsumerGroup) {
      const { groupListView } = this.props;
      formData['groupListView'] = groupListView;
      this.schema['groupListView'] = Joi.string().required();
    }

    this.setState({ formData, errors });
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
    const { pagination, search, topicListView } = this.state.formData;
    const data = {
      pagination: pagination,
      searchData: {
        search: search,
        topicListView: topicListView
      }
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
    const { showSearch, showTopicListView } = this.props;
    const { topicListViewOptions, showFilters } = this.state;

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
          </form>
        </div>
      </React.Fragment>
    );
  }
}

export default SearchBar;
