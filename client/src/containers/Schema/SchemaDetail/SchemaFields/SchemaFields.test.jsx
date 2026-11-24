import React from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import SchemaFields from './SchemaFields';

const v1 = {
  id: 10,
  version: 1,
  schemaType: 'AVRO',
  schema: JSON.stringify({
    type: 'record',
    name: 'User',
    fields: [{ name: 'id', type: 'long', doc: 'Old id' }]
  })
};

const v2 = {
  id: 11,
  version: 2,
  schemaType: 'AVRO',
  schema: JSON.stringify({
    type: 'record',
    name: 'User',
    doc: 'A registered user.',
    fields: [
      { name: 'id', type: 'long', doc: 'Internal user id' },
      { name: 'nickname', type: ['null', 'string'], default: null }
    ]
  })
};

describe('SchemaFields', () => {
  afterEach(() => cleanup());

  it('shows every field of the latest version by default', () => {
    render(<SchemaFields schemas={[v1, v2]} />);

    expect(screen.getByText('A registered user.')).toBeTruthy();
    expect(screen.getByText('Internal user id')).toBeTruthy();
    expect(screen.getByText('nickname')).toBeTruthy();
    expect(screen.getByText('null | string')).toBeTruthy();
    expect(screen.getByText('1 / 2 fields documented')).toBeTruthy();
    expect(screen.queryByText('Old id')).toBeNull();
  });

  it('switches to another version', () => {
    render(<SchemaFields schemas={[v1, v2]} />);

    fireEvent.change(screen.getByLabelText('Version'), { target: { value: '1' } });

    expect(screen.getByText('Old id')).toBeTruthy();
    expect(screen.queryByText('nickname')).toBeNull();
  });

  it('filters by field path or description', () => {
    render(<SchemaFields schemas={[v2]} />);

    fireEvent.change(screen.getByLabelText('Filter fields'), { target: { value: 'internal' } });

    expect(screen.getByText('Internal user id')).toBeTruthy();
    expect(screen.queryByText('nickname')).toBeNull();
  });

  it('explains that only Avro schemas are supported', () => {
    render(
      <SchemaFields
        schemas={[{ id: 3, version: 1, schemaType: 'PROTOBUF', schema: 'syntax = "proto3";' }]}
      />
    );

    expect(
      screen.getByText('Field descriptions are only available for Avro schemas.')
    ).toBeTruthy();
  });

  it('reports a version the server could not parse', () => {
    render(
      <SchemaFields
        schemas={[
          { id: 4, version: 1, schemaType: null, schema: null, exception: 'Invalid schema' }
        ]}
      />
    );

    expect(screen.getByText('This version could not be parsed: Invalid schema')).toBeTruthy();
    expect(screen.queryByText('Field')).toBeNull();
  });

  it('renders nothing until the versions are loaded', () => {
    const { container } = render(<SchemaFields schemas={[]} />);

    expect(container.innerHTML).toBe('');
  });
});
