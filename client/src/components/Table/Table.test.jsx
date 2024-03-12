/*eslint-disable*/
import React from 'react';
import { describe, it, vi } from 'vitest';
import Table from './Table';
import { render } from '@testing-library/react';
import {
  TABLE_ADD,
  TABLE_DELETE,
  TABLE_DETAILS,
  TABLE_CONFIG,
  TABLE_EDIT
} from '../../utils/constants';
import { Simulate } from 'react-dom/test-utils';

// noinspection JSUnusedGlobalSymbols
vi.mock('react-router-dom', () => ({
  useLocation: vi.fn(),
  useNavigate: vi.fn(),
  useParams: vi.fn(),
  Link: el => <div {...el}></div>
}));

describe('Table', () => {
  let text = '';
  const columns = [
    {
      id: 'test_1',
      accessor: 'test_1',
      colName: 'test_1',
      type: 'text'
    },
    {
      id: 'test_2',
      accessor: 'test_2',
      colName: 'test_2',
      type: 'text',
      cell: obj => {
        return (
          <input
            value={text}
            onChange={e => {
              text = e.target.value;
            }}
            data-testId={'test-name'}
            key={'test'}
            name={'name'}
            id={'name'}
            className="form-control"
            placeholder={'insert a name'}
          />
        );
      }
    },
    {
      id: 'test_3',
      accessor: 'test_3',
      colName: 'test_3',
      type: 'text'
    }
  ];
  const data = [
    {
      test_1: 'test',
      test_2: 'test2',
      test_3: 'test3'
    },
    {
      test_1: 'test',
      test_2: 'test2',
      test_3: 'test3'
    }
  ];
  const actions = [TABLE_ADD, TABLE_DELETE, TABLE_DETAILS, TABLE_CONFIG, TABLE_EDIT];
  const header = [
    { colName: 'MockHead1', colSpan: 1 },
    { colName: 'MockHead2', colSpan: 2 }
  ];

  let value = '';
  const mockFunction = newValue => {
    value = newValue;
  };

  const { container } = render(
    <Table
      has2Headers
      firstHeader={header}
      columns={columns}
      data={data}
      actions={actions}
      onAdd={() => {
        mockFunction(TABLE_ADD);
      }}
      onDetails={() => {
        mockFunction(TABLE_DETAILS);
      }}
      onConfig={() => {
        mockFunction(TABLE_CONFIG);
      }}
      onEdit={() => {
        mockFunction(TABLE_EDIT);
      }}
      onDelete={() => {
        mockFunction(TABLE_DELETE);
      }}
    />
  );

  it('renders successfully', ({ expect }) => {
    expect(container.querySelectorAll('tbody tr')).toHaveLength(2);
  });

  it('renders empty', ({ expect }) => {
    const { container } = render(<Table columns={columns} data={[]} />);
    const empty = container.querySelector('td div');
    expect(empty.textContent).toBe('No data available');
  });

  it('changes custom cell value', ({ expect }) => {
    let input = container.querySelector('td #name');
    input.value = 'test';
    Simulate.change(input);
    expect(text).toEqual('test');
  });

  it('does add logic', ({ expect }) => {
    let add = container.querySelector('#add');
    Simulate.click(add);
    expect(value).toEqual(TABLE_ADD);
  });

  it('does edit logic', ({ expect }) => {
    let edit = container.querySelector(`#${TABLE_EDIT}`);
    Simulate.click(edit);
    expect(value).toEqual(TABLE_EDIT);
  });

  // it('does details logic', ({ expect }) => {
  //   let details = container.querySelector(`#${TABLE_DETAILS}`);
  //   Simulate.click(details);
  //   expect(value).toEqual(TABLE_DETAILS);
  // });
  //
  // it('does config logic', ({ expect }) => {
  //   let config = container.querySelector(`#${TABLE_CONFIG}`);
  //   Simulate.click(config);
  //   expect(value).toEqual(TABLE_CONFIG);
  // });

  it('does delete logic', ({ expect }) => {
    let remove = container.querySelector(`#${TABLE_DELETE}`);
    Simulate.click(remove);
    expect(value).toEqual(TABLE_DELETE);
  });

  it('renders two headers', ({ expect }) => {
    let headers = container.querySelectorAll('thead');
    expect(headers).toHaveLength(2);
  });

  it('renders only two columns on first header', ({ expect }) => {
    let firstHeader = container.querySelector('#firstHeader');
    let columns = firstHeader.querySelectorAll('#headerColumn');
    expect(columns).toHaveLength(2);
  });
});
