import React from 'react';
import { describe, expect, it } from 'vitest';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Table from './Table';

describe('Table theme styles', () => {
  it('uses the theme body background for expanded rows', () => {
    const { container } = render(
      <MemoryRouter>
        <Table
          columns={[
            {
              id: 'value',
              accessor: 'value',
              colName: 'Value',
              extraRow: true,
              extraRowContent: () => <div>Expanded value</div>
            }
          ]}
          data={[{ id: 1, value: 'message' }]}
          actions={[]}
          extraRow
        />
      </MemoryRouter>
    );

    expect(
      container
        .querySelector('tbody tr:nth-child(2) td')
        .style.getPropertyValue('--bs-table-bg-state')
    ).toBe('var(--bs-body-bg, #fff)');
  });
});
