export const THEME_DARK = 'dark';
export const THEME_LIGHT = 'light';
export const THEME_SYSTEM = 'system';
export const THEME_STORAGE_KEY = 'akhq-theme';

export function getStoredTheme() {
  if (typeof window === 'undefined' || !window.localStorage) {
    return THEME_DARK;
  }
  return localStorage.getItem(THEME_STORAGE_KEY) || THEME_DARK;
}

export function getSystemTheme() {
  if (typeof window === 'undefined' || !window.matchMedia) {
    return THEME_DARK;
  }
  return window.matchMedia('(prefers-color-scheme: light)').matches ? THEME_LIGHT : THEME_DARK;
}

export function resolveTheme(theme) {
  if (theme === THEME_SYSTEM) {
    return getSystemTheme();
  }
  return theme === THEME_LIGHT ? THEME_LIGHT : THEME_DARK;
}

export function applyThemeToDOM(effectiveTheme) {
  if (typeof document === 'undefined') return;

  const root = document.getElementById('root');
  if (root) {
    root.setAttribute('data-bs-theme', effectiveTheme);
  }
  document.documentElement.setAttribute('data-bs-theme', effectiveTheme);

  let colorSchemeMeta = document.querySelector('meta[name="color-scheme"]');
  if (!colorSchemeMeta) {
    colorSchemeMeta = document.createElement('meta');
    colorSchemeMeta.name = 'color-scheme';
    document.head.appendChild(colorSchemeMeta);
  }
  colorSchemeMeta.content = effectiveTheme;
}

export function setStoredTheme(theme) {
  if (typeof window !== 'undefined' && window.localStorage) {
    localStorage.setItem(THEME_STORAGE_KEY, theme);
  }
  const effective = resolveTheme(theme);
  applyThemeToDOM(effective);
  return effective;
}

