import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import Table from './Table';
import { TABLE_FAVORITE } from '../../utils/constants';

vi.mock('react-router-dom', () => ({
  Link: props => <div {...props} />,
  useLocation: () => ({}),
  useNavigate: () => vi.fn(),
  useParams: () => ({}),
  useNavigationType: () => 'POP'
}));

describe('Table favourite action', () => {
  const columns = [{ id: 'name', accessor: 'name', colName: 'Name', type: 'text' }];

  it('renders both states and invokes the callback for each toggle direction', () => {
    const onFavorite = vi.fn();
    render(
      <Table
        columns={columns}
        data={[{ id: 'payments', name: 'payments', favorite: true }, { id: 'orders', name: 'orders' }]}
        actions={[TABLE_FAVORITE]}
        onFavorite={onFavorite}
      />
    );

    expect(screen.getByRole('button', { name: 'Remove from favourites' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Add to favourites' })).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Remove from favourites' }));
    fireEvent.keyDown(screen.getByRole('button', { name: 'Add to favourites' }), { key: 'Enter' });

    expect(onFavorite).toHaveBeenCalledWith(expect.objectContaining({ id: 'payments' }));
    expect(onFavorite).toHaveBeenCalledWith(expect.objectContaining({ id: 'orders' }));
  });
});
