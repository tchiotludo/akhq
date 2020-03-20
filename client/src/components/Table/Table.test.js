/*eslint-disable*/
import React from 'react';
import Enzyme, { shallow } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import Table from './Table';

Enzyme.configure({ adapter: new Adapter() });

describe('Table', () => {
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
        return <span>{obj.test_2}</span>;
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

  it('renders successfully', () => {
    const wrapper = shallow(<Table columns={columns} data={data} />);
    const rows = wrapper.find('tbody tr');
    console.log(rows.length);
    expect(rows).toHaveLength(2);
  });

  it('renders empty', () => {
    const wrapper = shallow(<Table columns={columns} data={[]} />);
    const rows = wrapper.find('tbody tr');
    const empty = rows.find('td div');
    console.log();
    expect(empty.text()).toBe('No topic available');
  });
});
