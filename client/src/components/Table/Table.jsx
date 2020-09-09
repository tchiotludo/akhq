import React, { Component } from 'react';
import PropTypes from 'prop-types';
import * as constants from '../../utils/constants';
import './styles.scss';

class Table extends Component {
  state = {
    extraExpanded: [],
    expanded: [],
    sortingColumn: '',
    reverse: false
  };

  handleExpand = el => {
    const currentExpandedRows = this.state.expanded;
    const isRowCurrentlyExpanded = currentExpandedRows.includes(el.id);
    const newExpandedRows = isRowCurrentlyExpanded
      ? currentExpandedRows.filter(id => id !== el.id)
      : currentExpandedRows.concat(el.id);
    this.setState({ expanded: newExpandedRows });
  };

  handleExtraExpand = el => {
    const currentExpandedRows = this.state.extraExpanded;
    const isRowCurrentlyExpanded = currentExpandedRows.includes(el.id);

    const newExpandedRows = isRowCurrentlyExpanded
      ? currentExpandedRows
      : currentExpandedRows.concat(el.id);
    this.setState({ extraExpanded: newExpandedRows });
  };

  handleExtraCollapse = el => {
    const currentExpandedRows = this.state.extraExpanded;
    const isRowCurrentlyExpanded = currentExpandedRows.includes(el.id);

    const newExpandedRows = !isRowCurrentlyExpanded
      ? currentExpandedRows
      : currentExpandedRows.filter(id => id !== el.id);
    this.setState({ extraExpanded: newExpandedRows });
  };

  renderHeader() {
    const { has2Headers, firstHeader, columns, actions, data } = this.props;
    return (
      <>
        {has2Headers && (
          <thead id="firstHeader" className="thead-dark">
            <tr key="firstHeader">
              {firstHeader.map((column, index) => {
                return (
                  <th
                    id="headerColumn"
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
        <thead id="secondHeader" className="thead-dark">
          <tr key="secondHeader">
            {columns.map((column, index) => {
              if (!column.extraRow) {
                return (
                  <th className="header-text" key={`secondHead${column.colName}${index}`}>
                    <div className="header-content">
                      {column.colName}
                      {column.sortable && (
                        <i
                          className="fa fa-sort clickable"
                          onClick={() => {
                            let data = [];
                            this.setState(
                              {
                                sortingColumn:
                                  column.accessor !== this.state.sortingColumn
                                    ? column.accessor
                                    : this.state.sortingColumn,
                                reverse:
                                  column.accessor !== this.state.sortingColumn &&
                                  this.state.sortingColumn > 0
                                    ? false
                                    : !this.state.reverse
                              },
                              () => {
                                data = this.props.data.sort(
                                  constants.sortBy(this.state.sortingColumn, this.state.reverse)
                                );
                                this.props.updateData(data);
                              }
                            );
                          }}
                        />
                      )}
                    </div>
                  </th>
                );
              }
              return null;
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
    const { actions, columns, extraRow, onExpand, noRowBackgroundChange, onDetails, handleExtraExpand, handleExtraCollapse, reduce } = this.props;
    const { extraExpanded } = this.state;

    let extraRowColCollapsed;
    let extraRowColExpanded;
    const items = [
      <tr
        key={`tableRow${index}`}
        className={reduce ? 'reduce' : ''}
      >
        {columns.map((column, colIndex) => {
          let extraStyles = [];
          if (noRowBackgroundChange) {
            extraStyles.push({ backgroundColor: '#444' });
          }
          if (column.expand) {
            extraStyles.push({ cursor: 'pointer' });
          }
          if (column.extraRow) {
            extraRowColCollapsed = column.cell ? column.cell(row, column) : row[column.accessor];
            extraRowColExpanded = column.extraRowContent
              ? column.extraRowContent(row, column)
              : row[column.accessor];
            return null;
          }
          if (typeof column.cell === 'function') {
            return (
              <td
                key={`tableCol${index}${colIndex}`}
                style={column.expand ? { cursor: 'pointer' } : {}}
                onClick={() => {
                  if (
                    actions &&
                    actions.find(action => action === constants.TABLE_DETAILS) &&
                    !column.expand
                  ) {
                    onDetails && onDetails(row.id, row);
                  }

                  column.expand && this.handleExpand(row);
                }}
                id={`row_${column.id}_${colIndex}`}
              >
                {column.cell(row, column)}
              </td>
            );
          }
          return (
            <td
              key={`tableCol${index}${colIndex}`}
              style={column.expand ? { cursor: 'pointer' } : {}}
              onClick={() => {
                if (
                  actions &&
                  actions.find(action => action === constants.TABLE_DETAILS) &&
                  !column.expand
                ) {
                  onDetails && onDetails(row.id, row);
                }

                column.expand && this.handleExpand(row);
              }}
              id={`row_${column.id}_${colIndex}`}
            >
              {row[column.accessor]}
            </td>
          );
        })}
        {actions && actions.length > 0 && this.renderActions(row)}
      </tr>
    ];
    if (
      JSON.stringify(
        this.state.expanded.find(el => {
          return el === row.id;
        })
      )
    ) {
      items.push(
        <tr key={'row-expandable-' + row.id}>
          <td
            key={'col-expandable-' + row.id}
            colSpan={columns.length + (actions && actions.length ? actions.length : 0)}
            style={{ padding: 0 }}
          >
            {onExpand(row)}
          </td>
        </tr>
      );
    }

    if (extraRow && extraRowColCollapsed) {
      items.push(
        <tr
          onClick={() => {
            if (
              !extraExpanded ||
              !JSON.stringify(extraExpanded.find(expanded => expanded.subject ? expanded.subject === row.subject : expanded === row.id)) ||
              !JSON.stringify(extraExpanded.find(expanded => expanded.subject ? expanded.subject === row.subject : expanded === row.id)).length > 0) {
                typeof handleExtraExpand === 'function'
                  ? this.setState({ extraExpanded: handleExtraExpand(extraExpanded, row)})
                  : this.handleExtraExpand(row);
            }
          }}
          key={'row-expanded-' + row.id}
        >
          <td
            style={{ backgroundColor: '#171819' }}
            colSpan={columns.length + (actions && actions.length ? actions.length : 0)}
          >
            {' '}
            {extraExpanded &&
            JSON.stringify(extraExpanded.find(expanded => expanded.subject ? expanded.subject === row.subject : expanded === row.id)) &&
            JSON.stringify(extraExpanded.find(expanded => expanded.subject ? expanded.subject === row.subject : expanded === row.id)).length > 0 ? (
              <div className="close-container">
                <span
                  onClick={() => {
                    typeof handleExtraCollapse === 'function'
                        ? this.setState({ extraExpanded: handleExtraCollapse(extraExpanded, row)})
                        : this.handleExtraCollapse(row);
                  }}
                  aria-hidden="true"
                >
                  ×
                </span>
              </div>
            ) : null}
            <div
              className={
                extraExpanded &&
                JSON.stringify(extraExpanded.find(expanded => expanded.subject ? expanded.subject === row.subject : expanded === row.id)) &&
                JSON.stringify(extraExpanded.find(expanded => expanded.subject ? expanded.subject === row.subject : expanded === row.id)).length > 0
                  ? ''
                  : 'collapsed-extra-row'
              }
            >
              {extraExpanded &&
              JSON.stringify(extraExpanded.find(expanded => expanded.subject ? expanded.subject === row.subject : expanded === row.id)) &&
              JSON.stringify(extraExpanded.find(expanded => expanded.subject ? expanded.subject === row.subject : expanded === row.id)).length > 0
                ? extraRowColExpanded
                : extraRowColCollapsed}
            </div>
          </td>
        </tr>
      );
    }

    return items;
  }

  renderActions(row) {
    const { actions, onAdd, onDetails, onConfig, onDelete, onEdit, onRestart } = this.props;

    return (
      <>
        {actions.find(el => el === constants.TABLE_ADD) && (
          <td className="khq-row-action khq-row-action-main action-hover">
            <span
              id="add"
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
              id="details"
              onClick={() => {
                onDetails && onDetails(row.id, row);
              }}
            >
              <i className="fa fa-search" />
            </span>
          </td>
        )}
        {actions.find(el => el === constants.TABLE_CONFIG) && (
          <td className="khq-row-action khq-row-action-main action-hover">
            <span
              id="config"
              onClick={() => {
                onConfig && onConfig(row.id, row);
              }}
            >
              <i className="fa fa-gear" />
            </span>
          </td>
        )}
        {actions.find(el => el === constants.TABLE_DELETE) && (
          <td className="khq-row-action khq-row-action-main action-hover">
            <span
              id="delete"
              onClick={() => {
                onDelete && onDelete(row);
              }}
            >
              <i className="fa fa-trash" />
            </span>
          </td>
        )}
        {actions.find(el => el === constants.TABLE_EDIT) && (
          <td className="khq-row-action khq-row-action-main action-hover">
            <span
              id="edit"
              onClick={() => {
                onEdit && onEdit();
              }}
            >
              <i className="fa fa-search" />
            </span>
          </td>
        )}
        {actions.find(el => el === constants.TABLE_RESTART) && (
          <td className="khq-row-action khq-row-action-main action-hover">
            <span
              id="restart"
              onClick={() => {
                onRestart && onRestart(row);
              }}
            >
              <i className="fa fa-refresh" />
            </span>
          </td>
        )}
      </>
    );
  }

  renderNoContent() {
    const { noContent, columns } = this.props;
    if (noContent) {
      if (typeof noContent === 'string') {
        return (
          <tr>
            <td colSpan={columns.length}>
              <div className="alert alert-warning mb-0" role="alert">
                {noContent}
              </div>
            </td>
          </tr>
        );
      } else {
        return noContent;
      }
    }
    return (
      <tr>
        <td colSpan={columns.length}>
          <div className="alert alert-warning mb-0" role="alert">
            No data available
          </div>
        </td>
      </tr>
    );
  }

  render() {
    const { noStripes } = this.props;
    let allItemRows = [];
    let data = this.props.data || [];

    data.forEach((item, index) => {
      if (!item.id) {
        item.id = index;
      }
      const perItemRows = this.renderRow(item, index);
      allItemRows = allItemRows.concat(perItemRows);
    });

    let classNames = 'table table-bordered table-hover mb-0';
    if (!noStripes) classNames += ' table-striped';
    if (noStripes) classNames += ' no-stripes';
    return (
      <div className="table-responsive">
        <table className={classNames}>
          {this.renderHeader()}
          <tbody>{data && data.length > 0 ? allItemRows : this.renderNoContent()}</tbody>
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
  columns: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      accessor: PropTypes.string,
      colName: PropTypes.string,
      type: PropTypes.string,
      cell: PropTypes.function
    })
  ),
  actions: PropTypes.array,
  onDetails: PropTypes.func,
  onConfig: PropTypes.func,
  onDelete: PropTypes.func,
  toPresent: PropTypes.array,
  noContent: PropTypes.any,
  handleExtraExpand: PropTypes.func,
  handleExtraCollapse: PropTypes.func
};

export default Table;
