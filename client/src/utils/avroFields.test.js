import { describe, expect, it } from 'vitest';
import { extractAvroFields, MAX_FIELDS } from './avroFields';

describe('extractAvroFields', () => {
  it('lists every top-level field with its type and doc', () => {
    const schema = {
      type: 'record',
      name: 'User',
      fields: [
        { name: 'id', type: 'long', doc: 'Internal user id' },
        { name: 'email', type: ['null', 'string'], doc: 'Contact address' },
        {
          name: 'createdAt',
          type: { type: 'long', logicalType: 'timestamp-millis' },
          doc: 'Creation time'
        },
        { name: 'undocumented', type: 'string' }
      ]
    };

    expect(extractAvroFields(JSON.stringify(schema))).toEqual([
      { path: 'id', type: 'long', doc: 'Internal user id' },
      { path: 'email', type: 'null | string', doc: 'Contact address' },
      { path: 'createdAt', type: 'long (timestamp-millis)', doc: 'Creation time' },
      { path: 'undocumented', type: 'string', doc: '' }
    ]);
  });

  it('walks nested records, unions, arrays and maps', () => {
    const schema = {
      type: 'record',
      name: 'Order',
      namespace: 'com.example',
      fields: [
        {
          name: 'customer',
          type: [
            'null',
            {
              type: 'record',
              name: 'Customer',
              fields: [{ name: 'name', type: 'string', doc: 'Display name' }]
            }
          ],
          doc: 'Who placed the order'
        },
        {
          name: 'lines',
          type: {
            type: 'array',
            items: {
              type: 'record',
              name: 'Line',
              fields: [{ name: 'sku', type: 'string', doc: 'Product code' }]
            }
          }
        },
        {
          name: 'attributes',
          type: {
            type: 'map',
            values: {
              type: 'record',
              name: 'Attr',
              fields: [{ name: 'v', type: 'int', doc: 'Value' }]
            }
          }
        },
        { name: 'status', type: { type: 'enum', name: 'Status', symbols: ['NEW', 'PAID'] } }
      ]
    };

    expect(extractAvroFields(schema)).toEqual([
      { path: 'customer', type: 'null | Customer', doc: 'Who placed the order' },
      { path: 'customer.name', type: 'string', doc: 'Display name' },
      { path: 'lines', type: 'array<Line>', doc: '' },
      { path: 'lines[].sku', type: 'string', doc: 'Product code' },
      { path: 'attributes', type: 'map<Attr>', doc: '' },
      { path: 'attributes{}.v', type: 'int', doc: 'Value' },
      { path: 'status', type: 'Status', doc: '' }
    ]);
  });

  it('resolves named type references, including across namespaces', () => {
    const schema = {
      type: 'record',
      name: 'Shipment',
      namespace: 'com.example',
      fields: [
        {
          name: 'from',
          type: {
            type: 'record',
            name: 'Address',
            fields: [{ name: 'city', type: 'string', doc: 'City name' }]
          }
        },
        { name: 'to', type: 'Address' },
        { name: 'via', type: ['null', 'com.example.Address'] }
      ]
    };

    expect(extractAvroFields(schema).map(entry => entry.path)).toEqual([
      'from',
      'from.city',
      'to',
      'to.city',
      'via',
      'via.city'
    ]);
  });

  it('stops on recursive types', () => {
    const schema = {
      type: 'record',
      name: 'Node',
      fields: [
        { name: 'label', type: 'string', doc: 'Label' },
        { name: 'next', type: ['null', 'Node'], doc: 'Next node' }
      ]
    };

    expect(extractAvroFields(schema)).toEqual([
      { path: 'label', type: 'string', doc: 'Label' },
      { path: 'next', type: 'null | Node', doc: 'Next node' }
    ]);
  });

  it('resolves short names inside a referenced type against its own namespace', () => {
    const schema = {
      type: 'record',
      name: 'Root',
      namespace: 'a',
      fields: [
        {
          name: 'inner',
          type: {
            type: 'record',
            name: 'Inner',
            fields: [
              {
                name: 'leaf',
                type: { type: 'record', name: 'Leaf', fields: [{ name: 'v', type: 'int' }] }
              },
              { name: 'leaf2', type: 'Leaf' }
            ]
          }
        },
        {
          name: 'other',
          type: {
            type: 'record',
            name: 'Other',
            namespace: 'b',
            fields: [{ name: 'ref', type: 'a.Inner' }]
          }
        }
      ]
    };

    expect(extractAvroFields(schema).map(entry => entry.path)).toContain('other.ref.leaf2.v');
  });

  it('tells apart the record branches of a union', () => {
    const schema = {
      type: 'record',
      name: 'Payment',
      fields: [
        {
          name: 'method',
          type: [
            'null',
            {
              type: 'record',
              name: 'Card',
              fields: [{ name: 'id', type: 'string', doc: 'Card id' }]
            },
            { type: 'record', name: 'Bank', fields: [{ name: 'id', type: 'long', doc: 'Bank id' }] }
          ]
        }
      ]
    };

    expect(extractAvroFields(schema)).toEqual([
      { path: 'method', type: 'null | Card | Bank', doc: '' },
      { path: 'method<Card>.id', type: 'string', doc: 'Card id' },
      { path: 'method<Bank>.id', type: 'long', doc: 'Bank id' }
    ]);
  });

  it('shows logical types of named types', () => {
    const schema = {
      type: 'record',
      name: 'Price',
      fields: [
        {
          name: 'amount',
          type: { type: 'fixed', name: 'Amount', size: 8, logicalType: 'decimal', precision: 10 }
        }
      ]
    };

    expect(extractAvroFields(schema)[0].type).toBe('Amount (decimal)');
  });

  it('stops listing at MAX_FIELDS when shared types blow up', () => {
    // Every level references the next type twice, which doubles the paths at each level.
    const levels = 20;
    const types = [];
    for (let i = levels; i >= 0; i--) {
      types[i] =
        i === levels
          ? { type: 'record', name: `T${i}`, fields: [{ name: 'v', type: 'int' }] }
          : {
              type: 'record',
              name: `T${i}`,
              fields: [
                { name: 'a', type: types[i + 1] },
                { name: 'b', type: `T${i + 1}` }
              ]
            };
    }

    expect(extractAvroFields(types[0])).toHaveLength(MAX_FIELDS);
  });

  it('returns nothing for schemas it cannot read', () => {
    expect(extractAvroFields('not json')).toEqual([]);
    expect(extractAvroFields('"string"')).toEqual([]);
    expect(extractAvroFields(undefined)).toEqual([]);
  });
});
