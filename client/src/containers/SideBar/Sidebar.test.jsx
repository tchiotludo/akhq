import { describe, expect, it, vi } from 'vitest';
import { Link } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import constants from '../../utils/constants';

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

  it('renders native links for navigation and updates selection on plain clicks', () => {
    const sidebar = buildSidebar();
    sidebar.state.selectedCluster = 'local';

    const item = sidebar.renderMenuItem(null, constants.NODE, 'Nodes');
    const [link] = findElements(item, Link);

    expect(link.props.to).toBe('/ui/local/node');
    expect(link.props['aria-label']).toBe('Nodes');
    link.props.onClick({
      button: 0,
      ctrlKey: false,
      metaKey: false,
      shiftKey: false,
      altKey: false
    });

    expect(sidebar.state.selectedTab).toBe(constants.NODE);
    expect(sidebar.props.router.navigate).not.toHaveBeenCalled();
  });

  it('positions collapsed flyouts next to the sidebar and within the viewport', () => {
    const sidebar = buildSidebar({ expanded: false });
    const element = {
      getBoundingClientRect: () => ({ top: 500 }),
      parentElement: {
        getBoundingClientRect: () => ({ right: 120 }),
        querySelector: () => ({ scrollHeight: 180 })
      }
    };

    expect(sidebar.flyoutProps('cluster').subnavStyle.visibility).toBe('hidden');
    sidebar.flyoutProps('cluster').onMouseEnter({ currentTarget: element });

    const expectedHeight = Math.min(180, window.innerHeight * 0.3);
    const expectedTop = Math.max(0, Math.min(500, window.innerHeight - expectedHeight - 60));
    expect(sidebar.flyoutProps('cluster').subnavStyle).toEqual({
      position: 'fixed',
      visibility: 'visible',
      left: 120,
      top: expectedTop
    });
  });
});
