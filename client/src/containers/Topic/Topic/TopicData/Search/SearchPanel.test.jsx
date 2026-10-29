import React, { useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import '@testing-library/jest-dom/vitest';
import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SearchPanel from './SearchPanel';
import { emptyDraft } from './searchModel';
import { expressionPreview } from './preview';

const Harness = ({
  initial = emptyDraft(),
  applied = null,
  onApply = vi.fn(),
  onClear = vi.fn(),
  isSearching = false,
  onStop
}) => {
  const [draft, setDraft] = useState(initial);
  return (
    <SearchPanel
      draft={draft}
      onDraftChange={setDraft}
      applied={applied}
      onApply={onApply}
      onClear={onClear}
      onStop={onStop}
      isSearching={isSearching}
    />
  );
};

afterEach(cleanup);

describe('SearchPanel', () => {
  it('shows only the simple editor by default and applies the expression', async () => {
    const user = userEvent.setup();
    const onApply = vi.fn();
    render(<Harness onApply={onApply} />);

    expect(screen.getByTestId('simple-search')).toBeInTheDocument();
    expect(screen.queryByTestId('advanced-search')).toBeNull();

    await user.type(screen.getByLabelText('Search text'), 'foo bar');
    await user.click(screen.getByRole('button', { name: 'Search' }));

    expect(onApply).toHaveBeenCalledWith({
      version: 1,
      groups: [
        {
          conditions: [
            { field: 'value', operator: 'contains', value: 'foo' },
            { join: 'OR', field: 'value', operator: 'contains', value: 'bar' }
          ]
        }
      ]
    });
  });

  it('hides the Any/All toggle for non-Contains operators', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    expect(screen.getByRole('group', { name: 'Term logic' })).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('Operator'), 'equals');
    expect(screen.queryByRole('group', { name: 'Term logic' })).toBeNull();
  });

  it('converts Simple to Advanced and hides the simple editor', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.type(screen.getByLabelText('Search text'), 'a b');
    await user.click(screen.getByRole('button', { name: 'All' }));
    await user.click(screen.getByRole('button', { name: 'Advanced' }));

    expect(screen.queryByTestId('simple-search')).toBeNull();
    const rows = screen.getAllByTestId('condition-row');
    expect(rows).toHaveLength(2);
    expect(within(rows[1]).getByLabelText('Join for condition 2')).toHaveValue('AND');
    expect(screen.getByTestId('group-scope')).toHaveTextContent('Scope: Value');
  });

  it('shows Mixed scope, adds rows and groups, and removes rows', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(screen.getByRole('button', { name: 'Advanced' }));
    await user.click(screen.getByRole('button', { name: '+ Add condition' }));
    await user.selectOptions(screen.getAllByLabelText('Field')[1], 'key');
    expect(screen.getByTestId('group-scope')).toHaveTextContent('Mixed');

    await user.click(screen.getAllByRole('button', { name: 'Remove condition' })[1]);
    expect(screen.getAllByTestId('condition-row')).toHaveLength(1);

    await user.click(screen.getByRole('button', { name: '+ Add group' }));
    expect(screen.getAllByTestId('search-group')).toHaveLength(2);
  });

  it('disables Search while a value is missing in Advanced', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(screen.getByRole('button', { name: 'Advanced' }));
    expect(screen.getByRole('button', { name: 'Search' })).toBeDisabled();
    expect(screen.getByRole('alert')).toHaveTextContent('A value is required.');
    await user.type(screen.getByLabelText('Value'), 'x');
    expect(screen.getByRole('button', { name: 'Search' })).toBeEnabled();
  });

  it('refuses Advanced -> Simple when lossy and offers a new simple search', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(screen.getByRole('button', { name: 'Advanced' }));
    await user.click(screen.getByRole('button', { name: '+ Add group' }));
    await user.click(screen.getByRole('button', { name: 'Simple' }));

    expect(screen.getByTestId('switch-blocked')).toHaveTextContent('more than one group');
    expect(screen.getByTestId('advanced-search')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Start a new simple search' }));
    expect(screen.getByTestId('simple-search')).toBeInTheDocument();
    expect(screen.queryByTestId('switch-blocked')).toBeNull();
  });

  it('converts a representable Advanced draft back to Simple', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.type(screen.getByLabelText('Search text'), 'a b');
    await user.click(screen.getByRole('button', { name: 'Advanced' }));
    await user.click(screen.getByRole('button', { name: 'Simple' }));
    expect(screen.getByLabelText('Search text')).toHaveValue('a b');
  });

  it('shows Stop while searching and calls onStop', async () => {
    const user = userEvent.setup();
    const onStop = vi.fn();
    render(<Harness isSearching onStop={onStop} />);
    expect(screen.queryByRole('button', { name: 'Search' })).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Stop' }));
    expect(onStop).toHaveBeenCalled();
  });

  it('keeps the applied filters visible and Clear calls onClear', async () => {
    const user = userEvent.setup();
    const onClear = vi.fn();
    const applied = {
      version: 1,
      groups: [{ conditions: [{ field: 'key', operator: 'equals', value: 'k1' }] }]
    };
    render(<Harness applied={applied} onClear={onClear} />);
    expect(screen.getByTestId('applied-filters')).toHaveTextContent('Key equals "k1"');
    await user.click(screen.getByRole('button', { name: 'Clear' }));
    expect(onClear).toHaveBeenCalled();
  });
});

describe('expressionPreview', () => {
  it('groups explicitly and is empty without an expression', () => {
    expect(expressionPreview(null)).toBe('');
    expect(
      expressionPreview({
        version: 1,
        groups: [
          {
            conditions: [
              { field: 'value', operator: 'contains', value: 'a' },
              { join: 'OR', field: 'value', operator: 'contains', value: 'b' }
            ]
          },
          { join: 'AND', conditions: [{ field: 'key', operator: 'matchesRegex', value: 'x' }] }
        ]
      })
    ).toBe('(Value contains "a" OR Value contains "b") AND Key matches regex "x"');
  });
});
