import { describe, expect, it, vi } from 'vitest';
import { Sidebar } from './Sidebar';

const buildSidebar = (props = {}) => {
  const sidebar = new Sidebar({
    location: { pathname: '/ui/local/topic' },
    params: {},
    router: { navigate: vi.fn() },
    clusters: [],
    children: null,
    expanded: true,
    toggleSidebar: vi.fn(),
    selectedTab: 'topic',
    ...props
  });

  sidebar.setState = state => {
    sidebar.state = { ...sidebar.state, ...state };
  };

  return sidebar;
};

const findElements = (node, type) => {
  const found = [];

  const walk = current => {
    if (!current || typeof current !== 'object') {
      return;
    }
    if (Array.isArray(current)) {
      current.forEach(walk);
      return;
    }
    if (current.type === type) {
      found.push(current);
    }
    if (current.props && current.props.children) {
      walk(current.props.children);
    }
  };

  walk(node);
  return found;
};

describe('Sidebar', () => {
  it('fills the viewport height and stays scrollable when folded', () => {
    const style = buildSidebar({ expanded: false }).render().props.style;

    expect(style.height).toBe('100vh');
    expect(style.overflowY).toBe('auto');
  });

  it('reserves room for the fixed action footer', () => {
    expect(buildSidebar().render().props.style.paddingBottom).toBe('60px');
  });

  it('renders no version paragraph when folded', () => {
    sessionStorage.setItem('version', '1.2.3');

    expect(findElements(buildSidebar({ expanded: false }).render(), 'p')).toHaveLength(0);
  });

  it('renders the version when expanded', () => {
    sessionStorage.setItem('version', '1.2.3');

    const paragraphs = findElements(buildSidebar({ expanded: true }).render(), 'p');

    expect(paragraphs).toHaveLength(1);
    expect(paragraphs[0].props.children).toBe('1.2.3');
  });
});
