import React, { Component } from 'react';
import Joi from 'joi-browser';
import SearchBar from '../../components/SearchBar';
import Input from '../../components/Form/Input';
import Header from '../Header';

class Tail extends Component {
  state = {
    formData: {
      search: ''
    },
    errors: {}
  };

  handleSubmit = e => {
    console.log('here', e);
  };
  handleChange = e => {
    console.log('change', e);
  };
  render() {
    const { search } = this.state;
    return (
      <div>
        <Header title="Live Tail" history={this.props.history} />
        <nav
          className="navbar navbar-expand-lg navbar-light 
        bg-light mr-auto khq-data-filter khq-sticky khq-nav"
        >
          <Input
            type="text"
            name="search"
            id="search"
            value={search}
            label={'Search'}
            placeholder={'search'}
            onChange={this.handleChange}
            wrapperClass={'tail-search-wrapper'}
            inputClass={'tail-search-input'}
          />

          <div class="dropdown bootstrap-select show-tick khq-select show">
            <select
              name="topics"
              class="khq-select"
              multiple=""
              data-selected-text-format="count"
              data-style="btn-white"
              data-actions-box="true"
              data-live-search="true"
              title="Topics"
              tabindex="-98"
            >
              <option value="__connect-1-config">__connect-1-config</option>
              <option value="__connect-1-offsets">__connect-1-offsets</option>
              <option value="__connect-1-status">__connect-1-status</option>
              <option value="__consumer_offsets">__consumer_offsets</option>
              <option value="_schemas">_schemas</option>
              <option value="test">test</option>
            </select>
            <button
              type="button"
              class="btn dropdown-toggle btn-white bs-placeholder"
              data-toggle="dropdown"
              role="combobox"
              aria-owns="bs-select-1"
              aria-haspopup="listbox"
              aria-expanded="true"
              title="Topics"
            >
              <div class="filter-option">
                <div class="filter-option-inner">
                  <div class="filter-option-inner-inner">Topics</div>
                </div>
              </div>
            </button>
            {/* <div
              class="dropdown-menu show"
              style="max-height: 771px; overflow: hidden; min-height: 182px;"
            >
              <div class="bs-searchbox">
                <input
                  type="search"
                  class="form-control"
                  autocomplete="off"
                  role="combobox"
                  aria-label="Search"
                  aria-controls="bs-select-1"
                  aria-autocomplete="list"
                />
              </div>
              <div class="bs-actionsbox">
                <div class="btn-group btn-group-sm btn-block">
                  <button type="button" class="actions-btn bs-select-all btn btn-light">
                    Select All
                  </button>
                  <button type="button" class="actions-btn bs-deselect-all btn btn-light">
                    Deselect All
                  </button>
                </div>
              </div>
              <div
                class="inner show"
                role="listbox"
                id="bs-select-1"
                tabindex="-1"
                aria-multiselectable="true"
                style="max-height: 678px; overflow-y: auto; min-height: 89px;"
              >
                <ul
                  class="dropdown-menu inner show"
                  role="presentation"
                  style="margin-top: 0px; margin-bottom: 0px;"
                >
                  <li>
                    <a
                      role="option"
                      class="dropdown-item"
                      id="bs-select-1-0"
                      tabindex="0"
                      aria-selected="false"
                    >
                      <span class="fa fa-check check-mark"></span>
                      <span class="text">__connect-1-config</span>
                    </a>
                  </li>
                  <li>
                    <a
                      role="option"
                      class="dropdown-item"
                      id="bs-select-1-1"
                      tabindex="0"
                      aria-selected="false"
                    >
                      <span class="fa fa-check check-mark"></span>
                      <span class="text">__connect-1-offsets</span>
                    </a>
                  </li>
                  <li>
                    <a
                      role="option"
                      class="dropdown-item"
                      id="bs-select-1-2"
                      tabindex="0"
                      aria-selected="false"
                    >
                      <span class="fa fa-check check-mark"></span>
                      <span class="text">__connect-1-status</span>
                    </a>
                  </li>
                  <li>
                    <a
                      role="option"
                      class="dropdown-item"
                      id="bs-select-1-3"
                      tabindex="0"
                      aria-selected="false"
                    >
                      <span class="fa fa-check check-mark"></span>
                      <span class="text">__consumer_offsets</span>
                    </a>
                  </li>
                  <li>
                    <a
                      role="option"
                      class="dropdown-item"
                      id="bs-select-1-4"
                      tabindex="0"
                      aria-selected="false"
                    >
                      <span class="fa fa-check check-mark"></span>
                      <span class="text">_schemas</span>
                    </a>
                  </li>
                  <li>
                    <a
                      role="option"
                      class="dropdown-item"
                      id="bs-select-1-5"
                      tabindex="0"
                      aria-selected="false"
                    >
                      <span class="fa fa-check check-mark"></span>
                      <span class="text">test</span>
                    </a>
                  </li>
                </ul>
              </div>
            </div> */}
          </div>
          <button className="btn btn-primary" type="submit">
            <span className="d-md-none">Search </span>
            <i className="fa fa-search" />
          </button>
        </nav>
      </div>
    );
  }
}

export default Tail;
