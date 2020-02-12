import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';
import * as constants from '../../utils/constants';

class Table extends Component {
  state = {};

  componentDidMount() {}

  renderHeader() {
    const { has2Headers, firstHeader, colNames } = this.props;

    return (
      <>
        {has2Headers && (
          <thead className="thead-dark">
            <tr>
              {firstHeader.map((column, index) => {
                return (
                  <th key={column.colName + index} colSpan={column.colSpan}>
                    {column.colName}
                  </th>
                );
              })}
            </tr>
          </thead>
        )}
        <thead className="thead-dark">
          <tr>
            {colNames.map((column, index) => {
              return <th key={column.colName + index}>{column}</th>;
            })}
          </tr>
        </thead>
      </>
    );
  }

  renderRow(row) {
    return <tr>{row.map(element => {})}</tr>;
  }

  renderActions(row) {
    const { actions } = this.props;

    return (
      <>
        {actions.find(el => el === constants.VIEW_TABLE) && (
          <td className="khq-row-action khq-row-action-main">
            <Link to={row.url || '/'}>
              <i className="fa fa-search" />
            </Link>
          </td>
        )}
        {actions.find(el => el === constants.EDIT_TABLE) && (
          <td className="khq-row-action khq-row-action-main">
            <Link to={row.url || '/'}>
              <i className="fa fa-search" />
            </Link>
          </td>
        )}
        {actions.find(el => el === constants.DELETE_TABLE) && (
          <td className="khq-row-action khq-row-action-main">
            <Link to={row.url || '/'}>
              <i className="fa fa-search" />
            </Link>
          </td>
        )}
        {actions.find(el => el === constants.ADD_TABLE) && (
          <td className="khq-row-action khq-row-action-main">
            <Link to={row.url || '/'}>
              <i className="fa fa-search" />
            </Link>
          </td>
        )}
      </>
    );
  }

  render() {
    const { data } = this.props;

    return (
      <div className="table-responsive">
        <table className="table table-bordered table-striped table-hover mb-0">
          {this.renderHeader()}
          {data.map(row => {
            this.renderRow(row);
          })}
        </table>
      </div>
    );
  }
}

Table.propTypes = {
  title: PropTypes.string,
  has2Headers: PropTypes.bool,
  firstHeader: PropTypes.arrayOf(
    PropTypes.shape({
      colName: PropTypes.string,
      colSpan: PropTypes.number
    })
  ),
  data: PropTypes.array,
  colNames: PropTypes.array,
  actions: PropTypes.array,
  onEdit: PropTypes.func,
  onDelete: PropTypes.func,
  toPresent: PropTypes.array
};

export default Table;
