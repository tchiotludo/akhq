/*eslint-disable*/
import React from 'react';
import { describe, it, vi } from 'vitest';
import SchemaCreate from './SchemaCreate';
import { render } from '@testing-library/react';
import { Simulate } from 'react-dom/test-utils';

// noinspection JSUnusedGlobalSymbols
vi.mock('react-router-dom', () => ({
  useLocation: vi.fn(),
  useNavigate: vi.fn(),
  useParams: vi.fn()
}));

describe('SchemaCreate', () => {
  it('empty test', ({ expect }) => {});
  /*
  it('should save input in schemas formData', ({ expect }) => {
    const element = <SchemaCreate />;

    const { container, rerender } = render(element);

    const mockState = {
      subject: 'test',
      schemaData: 'schemaDataTest',
      compatibilityLevel: 'BACKWARD'
    };

    const subject = container.querySelector('#subject');
    subject.name = 'subject';
    subject.value = 'test';
    Simulate.change(subject);
    const schemaData = container.querySelector('#schemaData');
    schemaData.target = 'schemaDataTest';
    Simulate.change(schemaData);
    const compatibilityLevel = container.querySelector('Select');
    compatibilityLevel.name = 'compatibilityLevel';
    compatibilityLevel.value = 'BACKWARD';
    Simulate.change(compatibilityLevel);

    rerender(element);

    expect(container.state.formData).toStrictEqual(mockState);
  });
*/
});
