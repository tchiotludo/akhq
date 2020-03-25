import React, { Component } from 'react';
import Header from '../Header/Header';
import { Link } from 'react-router-dom';
import './styles.scss';

class SchemaList extends Component {
  state = {};
  render() {
    const { clusterId } = this.props.match.params;

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

export default SchemaList;
