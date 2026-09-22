import React, { createContext, useContext, useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import {
  THEME_DARK,
  THEME_LIGHT,
  THEME_SYSTEM,
  getStoredTheme,
  resolveTheme,
  applyThemeToDOM,
  setStoredTheme
} from '../utils/theme';

export const ThemeContext = createContext({
  theme: THEME_DARK,
  resolvedTheme: THEME_DARK,
  setTheme: () => {}
});

export const ThemeProvider = ({ children }) => {
  const [theme, setThemeState] = useState(() => getStoredTheme());
  const [resolvedTheme, setResolvedTheme] = useState(() => resolveTheme(getStoredTheme()));

  const setTheme = newTheme => {
    setThemeState(newTheme);
    const resolved = setStoredTheme(newTheme);
    setResolvedTheme(resolved);
  };

  useEffect(() => {
    const resolved = resolveTheme(theme);
    setResolvedTheme(resolved);
    applyThemeToDOM(resolved);

    if (theme === THEME_SYSTEM && typeof window !== 'undefined' && window.matchMedia) {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: light)');
      const handleChange = () => {
        const nextResolved = resolveTheme(THEME_SYSTEM);
        setResolvedTheme(nextResolved);
        applyThemeToDOM(nextResolved);
      };

      mediaQuery.addEventListener('change', handleChange);
      return () => mediaQuery.removeEventListener('change', handleChange);
    }
  }, [theme]);

  return (
    <ThemeContext.Provider value={{ theme, resolvedTheme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

ThemeProvider.propTypes = {
  children: PropTypes.node
};

export const useTheme = () => useContext(ThemeContext);
