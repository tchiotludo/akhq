/*eslint-disable*/
/*
import React from 'react';
import Enzyme, { shallow } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import ConsumerGroupUpdate from './ConsumerGroupUpdate';
import { createMemoryHistory } from 'history';

Enzyme.configure({ adapter: new Adapter() });

describe('ConsumerGroupUpdate', () => {
  const route = '/abc/group/123/update';
  const history = createMemoryHistory({ initialEntries: [route] });
  const wrapper = shallow(
    <ConsumerGroupUpdate
      match={{ params: { clusterId: 'abc', groupId: '123' } }}
    />
  );
  const instance = wrapper.instance();
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
  instance.setState({ groupedTopicOffset });

  it('should return processed consummer group offset formData', () => {
    instance.createValidationSchema(groupedTopicOffset);

    let expectedFormData = {};
    expectedFormData['offset[test1][0]'] = 3;
    expectedFormData['offset[test2][0]'] = 5;
    expectedFormData['offset[test2][1]'] = 6;
    expectedFormData['offset[test3][0]'] = 1;
    expectedFormData['offset[test3][1]'] = 8;
    expectedFormData['offset[test3][2]'] = 3;

    const actualFormData = instance.state.formData;

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

    instance.setState({ formData }, () => {
      expect(wrapper.find('#fieldset-test1')).toHaveLength(1);
      expect(wrapper.find('#legend-test1')).toHaveLength(1);
      expect(wrapper.find('#test1-0-input')).toHaveLength(1);

      expect(wrapper.find('#fieldset-test2')).toHaveLength(1);
      expect(wrapper.find('#legend-test2')).toHaveLength(1);
      expect(wrapper.find('#test2-0-input')).toHaveLength(1);
      expect(wrapper.find('#test2-1-input')).toHaveLength(1);

      expect(wrapper.find('#fieldset-test3')).toHaveLength(1);
      expect(wrapper.find('#legend-test3')).toHaveLength(1);
      expect(wrapper.find('#test3-0-input')).toHaveLength(1);
      expect(wrapper.find('#test3-1-input')).toHaveLength(1);
      expect(wrapper.find('#test3-2-input')).toHaveLength(1);
    });
  });
});

 */
