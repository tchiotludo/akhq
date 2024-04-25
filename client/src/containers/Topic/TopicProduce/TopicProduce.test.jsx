/*eslint-disable*/
import React from 'react';
import { describe, it, vi } from 'vitest';
import TopicProduce from './TopicProduce';
import { render } from '@testing-library/react';

vi.mock('./../../../prefix', () => ({
  default: () => ''
}));

// noinspection JSUnusedGlobalSymbols
vi.mock('react-router-dom', () => ({
  useLocation: vi.fn(),
  useNavigate: vi.fn(),
  useNavigationType: vi.fn(),
  useParams: () => ({ clusterId: '123', topicId: '456' })
}));

vi.mock('axios', async importActual => {
  const mod = await importActual();
  return {
    ...mod,
    default: {
      ...mod.default,
      isCancel: err => true
    }
  };
});

describe('TopicProduce', () => {
  it('should add a new header to headers list', ({ expect }) => {
    const element = <TopicProduce />;

    const { container, rerender } = render(element);
    const headers = container.querySelector('[data-testid="headers"]');

    const button = container.querySelector('[data-testid="button_0"]');
    button.click();

    rerender(element);

    expect(headers.children.length).toBe(2);
  });

  it('should remove a header from headers list', ({ expect }) => {
    const element = <TopicProduce />;

    const { container, rerender } = render(element);
    const headers = container.querySelector('[data-testid="headers"]');

    const button = container.querySelector('[data-testid="button_0"]');
    button.click();

    rerender(element);

    expect(headers.children.length).toBe(2);

    const button2 = container.querySelector('[data-testid="button_1"]');
    button2.click();

    rerender(element);

    expect(headers.children.length).toBe(1);
  });
});
