import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import Header from '../Header';

class Schema extends Component {
  render() {
    const { clusterId, topicId } = this.props.match.params;
    return (
      <div id="content">
        <Header title="Schema Registry" />
        Schema
        <aside>
          <Link
            className="btn btn-primary"
            to={{
              pathname: `/${clusterId}/schema/create`
            }}
          >
            Create a Subject
          </Link>
        </aside>
      </div>
    );
  }
}

export default Schema;
