/*eslint-disable*/
import React from 'react';
import { describe, it, vi } from 'vitest';
import ConsumerGroupUpdate from './ConsumerGroupUpdate';
import { render } from '@testing-library/react';

// noinspection JSUnusedGlobalSymbols
vi.mock('react-router-dom', () => ({
  useLocation: vi.fn(),
  useNavigate: vi.fn(),
  useParams: vi.fn()
}));

describe('ConsumerGroupUpdate', () => {
  it('empty test', ({ expect }) => {});
  /*
  const { container } = render(<ConsumerGroupUpdate />);
  const groupedTopicOffset = {
    test1: [
      {
        partition: 0,
        offset: 3,
        firstOffset: 1,
        lastOffset: 3
      }
    ],
    test2: [
      {
        partition: 0,
        offset: 5,
        firstOffset: 5,
        lastOffset: 7
      },
      {
        partition: 1,
        offset: 6,
        firstOffset: 2,
        lastOffset: 8
      }
    ],
    test3: [
      {
        partition: 0,
        offset: 1,
        firstOffset: 1,
        lastOffset: 8
      },
      {
        partition: 1,
        offset: 8,
        firstOffset: 8,
        lastOffset: 9
      },
      {
        partition: 2,
        offset: 3,
        firstOffset: 1,
        lastOffset: 3
      }
    ]
  };

  container.setState({ groupedTopicOffset });

  it('should return processed consummer group offset formData', () => {
    container.createValidationSchema(groupedTopicOffset);

    let expectedFormData = {};
    expectedFormData['offset[test1][0]'] = 3;
    expectedFormData['offset[test2][0]'] = 5;
    expectedFormData['offset[test2][1]'] = 6;
    expectedFormData['offset[test3][0]'] = 1;
    expectedFormData['offset[test3][1]'] = 8;
    expectedFormData['offset[test3][2]'] = 3;

    const actualFormData = container.state.formData;

    expect(actualFormData).toStrictEqual(expectedFormData);
  });

  it('should return render offsets inputs', () => {
    let formData = {};
    formData['offset[test1][0]'] = 0;
    formData['offset[test2][0]'] = 0;
    formData['offset[test2][1]'] = 0;
    formData['offset[test3][0]'] = 0;
    formData['offset[test3][1]'] = 0;
    formData['offset[test3][2]'] = 0;

    container.setState({ formData }, () => {
      expect(container.querySelectorAll('#fieldset-test1')).toHaveLength(1);
      expect(container.querySelectorAll('#legend-test1')).toHaveLength(1);
      expect(container.querySelectorAll('#test1-0-input')).toHaveLength(1);

      expect(container.querySelectorAll('#fieldset-test2')).toHaveLength(1);
      expect(container.querySelectorAll('#legend-test2')).toHaveLength(1);
      expect(container.querySelectorAll('#test2-0-input')).toHaveLength(1);
      expect(container.querySelectorAll('#test2-1-input')).toHaveLength(1);

      expect(container.querySelectorAll('#fieldset-test3')).toHaveLength(1);
      expect(container.querySelectorAll('#legend-test3')).toHaveLength(1);
      expect(container.querySelectorAll('#test3-0-input')).toHaveLength(1);
      expect(container.querySelectorAll('#test3-1-input')).toHaveLength(1);
      expect(container.querySelectorAll('#test3-2-input')).toHaveLength(1);
    });
  });
*/
});
