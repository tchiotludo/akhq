import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';
import * as constants from '../../utils/constants';
import './styles.scss';

class Table extends Component {
  state = {};

  componentDidMount() {}

  renderHeader() {
    const { has2Headers, firstHeader, colNames, actions, data } = this.props;

    return (
      <>
        {has2Headers && (
          <thead className="thead-dark">
            <tr key="firstHeader">
              {firstHeader.map((column, index) => {
                return (
                  <th
                    className="header-text"
                    key={`firstHead${column.colName}${index}`}
                    colSpan={column.colSpan}
                  >
                    {column.colName}
                  </th>
                );
              })}
              {actions && actions.length > 0 && data && data.length > 0 && (
                <th colSpan={actions.length} />
              )}
            </tr>
          </thead>
        )}
        <thead className="thead-dark">
          <tr key="secondHeader">
            {colNames.map((column, index) => {
              return (
                <th className="header-text" key={`secondHead${column.colName}${index}`}>
                  {column}
                </th>
              );
            })}
            {actions && actions.length > 0 && data && data.length > 0 && (
              <th colSpan={actions.length} />
            )}
          </tr>
        </thead>
      </>
    );
  }

  renderRow(row, index) {
    const { toPresent, actions } = this.props;
    return (
      <tr key={`tableRow${index}`}>
        {Object.keys(row).map(key => {
          if (toPresent.find(elem => elem === key)) {
            return <td key={key}>{row[key]}</td>;
          }
        })}
        {actions && actions.length > 0 && this.renderActions(row)}
      </tr>
    );
  }

  renderActions(row) {
    const { actions, onAdd, onDetails, onDelete } = this.props;

    return (
      <>
        {actions.find(el => el === constants.TABLE_ADD) && (
          <td className="khq-row-action khq-row-action-main action-hover">
            <span
              onClick={() => {
                onAdd && onAdd();
              }}
            >
              <i className="fa fa-search" />
            </span>
          </td>
        )}
        {actions.find(el => el === constants.TABLE_DETAILS) && (
          <td className="khq-row-action khq-row-action-main action-hover">
            <span
              onClick={() => {
                console.log('doRow', row);
                onDetails && onDetails(row.id);
              }}
            >
              <i className="fa fa-search" />
            </span>
          </td>
        )}
        {actions.find(el => el === constants.TABLE_DELETE) && (
          <td className="khq-row-action khq-row-action-main action-hover">
            <span
              onClick={() => {
                onDelete && onDelete();
              }}
            >
              <i className="fa fa-trash" />
            </span>
          </td>
        )}
        {actions.find(el => el === constants.TABLE_EDIT) && (
          <td className="khq-row-action khq-row-action-main action-hover">
            <Link to={row.url || '/'}>
              <i className="fa fa-search" />
            </Link>
          </td>
        )}
      </>
    );
  }

  render() {
    const { data, colNames } = this.props;

    return (
      <div className="table-responsive">
        <table className="table table-bordered table-striped table-hover mb-0">
          {this.renderHeader()}
          <tbody>
            {data && data.length > 0 ? (
              data.map((row, index) => {
                return this.renderRow(row, index);
              })
            ) : (
              <tr>
                <td colSpan={colNames.length}>
                  <div className="alert alert-info mb-0" role="alert">
                    No topic available
                  </div>
                </td>
              </tr>
            )}
          </tbody>
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
  onDetails: PropTypes.func,
  onDelete: PropTypes.func,
  toPresent: PropTypes.array
};

export default Table;
