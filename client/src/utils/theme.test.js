import { describe, it, expect, beforeEach, vi } from 'vitest';
import {
  THEME_DARK,
  THEME_LIGHT,
  THEME_SYSTEM,
  THEME_STORAGE_KEY,
  getStoredTheme,
  getSystemTheme,
  resolveTheme,
  applyThemeToDOM,
  setStoredTheme
} from './theme';

describe('theme utils', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-bs-theme');
    const root = document.getElementById('root');
    if (root) {
      root.removeAttribute('data-bs-theme');
    }
    const meta = document.querySelector('meta[name="color-scheme"]');
    if (meta) {
      meta.remove();
    }
  });

  describe('getStoredTheme', () => {
    it('returns THEME_DARK by default when nothing is stored', () => {
      expect(getStoredTheme()).toBe(THEME_DARK);
    });

    it('returns stored theme when present', () => {
      localStorage.setItem(THEME_STORAGE_KEY, THEME_LIGHT);
      expect(getStoredTheme()).toBe(THEME_LIGHT);
    });
  });

  describe('resolveTheme', () => {
    it('resolves dark theme directly', () => {
      expect(resolveTheme(THEME_DARK)).toBe(THEME_DARK);
    });

    it('resolves light theme directly', () => {
      expect(resolveTheme(THEME_LIGHT)).toBe(THEME_LIGHT);
    });

    it('resolves system theme based on matchMedia', () => {
      window.matchMedia = vi.fn().mockImplementation(query => ({
        matches: query.includes('light'),
        media: query,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn()
      }));

      expect(resolveTheme(THEME_SYSTEM)).toBe(THEME_LIGHT);
    });
  });

  describe('applyThemeToDOM', () => {
    it('sets data-bs-theme on documentElement and root, and updates color-scheme meta', () => {
      const rootDiv = document.createElement('div');
      rootDiv.id = 'root';
      document.body.appendChild(rootDiv);

      applyThemeToDOM(THEME_LIGHT);

      expect(document.documentElement.getAttribute('data-bs-theme')).toBe(THEME_LIGHT);
      expect(rootDiv.getAttribute('data-bs-theme')).toBe(THEME_LIGHT);
      const meta = document.querySelector('meta[name="color-scheme"]');
      expect(meta).not.toBeNull();
      expect(meta.getAttribute('content')).toBe(THEME_LIGHT);

      rootDiv.remove();
    });
  });

  describe('setStoredTheme', () => {
    it('persists theme to localStorage and applies to DOM', () => {
      setStoredTheme(THEME_LIGHT);
      expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe(THEME_LIGHT);
      expect(document.documentElement.getAttribute('data-bs-theme')).toBe(THEME_LIGHT);
    });
  });
});

