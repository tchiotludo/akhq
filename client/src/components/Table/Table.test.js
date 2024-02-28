/*eslint-disable*/
/*
import React from 'react';
import Enzyme, { shallow } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import Table from './Table';
import {
  TABLE_ADD,
  TABLE_DELETE,
  TABLE_DETAILS,
  TABLE_CONFIG,
  TABLE_EDIT
} from '../../utils/constants';

Enzyme.configure({ adapter: new Adapter() });

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

  const wrapper = shallow(
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

  const rows = wrapper.find('tbody tr');

  it('renders successfully', () => {
    expect(rows).toHaveLength(2);
  });

  it('renders empty', () => {
    const wrapper = shallow(<Table columns={columns} data={[]} />);
    const rows = wrapper.find('tbody tr');
    const empty = rows.find('td div');
    expect(empty.text()).toBe('No topic available');
  });

  it('changes custom cell value', () => {
    let input = rows.first().find('td #name');
    input.simulate('change', { target: { value: 'test' } });
    expect(text).toEqual('test');
  });

  it('does add logic', () => {
    let add = rows.first().find('#add');
    add.simulate('click');
    expect(value).toEqual(TABLE_ADD);
  });

  it('does edit logic', () => {
    let edit = rows.first().find(`#${TABLE_EDIT}`);
    edit.simulate('click');
    expect(value).toEqual(TABLE_EDIT);
  });

  it('does details logic', () => {
    let details = rows.first().find(`#${TABLE_DETAILS}`);
    details.simulate('click');
    expect(value).toEqual(TABLE_DETAILS);
  });

  it('does config logic', () => {
    let details = rows.first().find(`#${TABLE_CONFIG}`);
    details.simulate('click');
    expect(value).toEqual(TABLE_CONFIG);
  });

  it('does delete logic', () => {
    let remove = rows.first().find(`#${TABLE_DELETE}`);
    remove.simulate('click');
    expect(value).toEqual(TABLE_DELETE);
  });

  it('renders two headers', () => {
    let headers = wrapper.find('thead');
    expect(headers).toHaveLength(2);
  });

  it('renders only two columns on first header', () => {
    let headers = wrapper.find('thead');
    let firstHeader = headers.find('#firstHeader');
    let columns = firstHeader.find('#headerColumn');
    expect(columns).toHaveLength(2);
  });
});

 */
