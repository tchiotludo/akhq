import React, { Component } from 'react';
import Joi from 'joi-browser';
import PropTypes from 'prop-types';
import Pagination from '../Pagination';
import constants from '../../utils/constants';
import Form from '../Form/Form';

class SearchBar extends Form {
  static propTypes = {
    showPagination: PropTypes.bool,
    showTopicListView: PropTypes.bool,
    showSearch: PropTypes.bool
  };
  state = {
    formData: {},
    errors: {},
    topicListViewOptions: [
      {
        _id: 'ALL',
        name: 'ALL'
      },
      {
        _id: 'HIDE_INTERNAL',
        name: 'HIDE_INTERNAL'
      },
      {
        _id: 'HIDE_INTERNAL_STREAM',
        name: 'HIDE_INTERNAL_STREAM'
      },
      {
        _id: 'HIDE_STREAM',
        name: 'HIDE_STREAM'
      }
    ]
  };

  schema = {};

  componentDidMount() {
    const { showSearch, showPagination, showTopicListView } = this.props;
    const { formData, errors } = this.state;
    if (showSearch) {
      const { search } = this.props;
      formData['search'] = search;
      this.schema['search'] = Joi.string().allow('');
    }

    if (showPagination) {
      const { pagination } = this.props;
      formData['pagination'] = pagination;
      this.schema['pagination'] = Joi.number()
        .min(0)
        .required();
    }

    if (showTopicListView) {
      const { topicListView } = this.props;
      formData['topicListView'] = topicListView;
      this.schema['topicListView'] = Joi.string().required();
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

  render() {
    const { showSearch, showPagination, showTopicListView } = this.props;
    const { topicListViewOptions } = this.state;
    return (
      <nav className="navbar navbar-expand-lg navbar-light bg-light mr-auto khq-data-filter khq-sticky khq-nav">
        <button
          className="navbar-toggler"
          type="button"
          data-toggle="collapse"
          data-target="#navbar-search"
          aria-controls="navbar-search"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span className="navbar-toggler-icon" />
        </button>

        {showPagination ? (
          <nav>
            <Pagination />
          </nav>
        ) : (
          <div></div>
        )}

        <div className="collapse navbar-collapse" id="navbar-search">
          <form className="form-inline mr-auto khq-form-get" method="get">
            {showSearch &&
              this.renderInput('search', '', 'Search', 'text', { autoComplete: 'off' })}
            {showTopicListView &&
              // <select
              //   name="show"
              //   value={formData.topicListView}
              //   className="khq-select form-control"
              //   data-style="btn-white"
              //   onChange={topic => this.setTopic(topic.target.formData.topicListView)}
              // >
              //   <option value="ALL">Show all topics</option>
              //   <option value="HIDE_INTERNAL">Hide internal topics</option>
              //   <option value="HIDE_INTERNAL_STREAM">Hide internal & stream topics</option>
              //   <option value="HIDE_STREAM">Hide stream topics</option>
              // </select>´
              this.renderSelect('topicListView', '', topicListViewOptions)}

            <button className="btn btn-primary" type="submit">
              <span className="d-md-none">Search </span>
              <i className="fa fa-search" />
            </button>
          </form>
        </div>
      </nav>
    );
  }
}

export default SearchBar;
