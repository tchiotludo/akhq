import { describe, expect, it } from 'vitest';
import { compile } from 'sass';
import { resolve } from 'node:path';

const stylesheet = resolve('src/index.scss');
const compiledStyles = compile(stylesheet, {
  quietDeps: true,
  silenceDeprecations: ['color-functions', 'global-builtin', 'if-function', 'import', 'mixed-decls']
}).css;

describe('theme-aware styles', () => {
  it('uses the active body background for tab content', () => {
    expect(compiledStyles).toMatch(
      /\.tabs-container \.tab-content\s*\{[^}]*background:\s*var\(--bs-body-bg, #fff\)/
    );
  });

  it('keeps light sidebar labels dark and semibold', () => {
    expect(compiledStyles).toContain('--akhq-sidebar-color: #212529;');
    expect(compiledStyles).toContain('--akhq-sidebar-label-weight: 600;');
    expect(compiledStyles).toContain('font-weight: var(--akhq-sidebar-label-weight, 400);');
    expect(compiledStyles).toMatch(
      /\.sidenav---navtext---1AE_f > \*\s*\{\s*color: var\(--akhq-sidebar-color\)/
    );
    expect(compiledStyles).toMatch(
      /\.sidenav---navicon---3gCRo > \*\s*\{\s*color: var\(--akhq-sidebar-icon-color\)/
    );
  });

  it('retains the dark sidebar palette and theme-aware topic-data backgrounds', () => {
    expect(compiledStyles).toContain('--akhq-sidebar-color: #f8f9fa;');
    expect(compiledStyles).toContain('--akhq-sidebar-label-weight: 600;');
    expect(compiledStyles).toMatch(
      /\.sidenav---navtext---1AE_f > \*[^{}]*\{\s*color: var\(--bs-primary, #005f81\)/
    );
    expect(compiledStyles).toMatch(
      /\.page-item\.info \.page-link\s*\{\s*color: var\(--bs-body-color\);\s*background: var\(--bs-secondary-bg\)/
    );
    expect(compiledStyles).toMatch(
      /\.btn-theme-secondary\s*\{[^}]*--bs-btn-color: var\(--bs-body-color\);[^}]*--bs-btn-bg: var\(--bs-secondary-bg\)/
    );
    expect(compiledStyles).toMatch(
      /\.badge-theme-secondary\s*\{\s*color: var\(--bs-body-color\) !important;\s*background-color: var\(--bs-secondary-bg\) !important;/
    );
    expect(compiledStyles).toContain(
      '.headers-modal {\n  background-color: var(--bs-body-bg, #171819);'
    );
  });

  it('keeps Tail dropdown toggles theme-aware when closed and open', () => {
    expect(compiledStyles).toMatch(
      /\.dropdown-toggle:not\(\.btn-theme-secondary\)[^{}]*\{\s*background-color: transparent !important;/
    );
    expect(compiledStyles).toMatch(
      /nav \.show > \.btn-secondary\.dropdown-toggle\.btn-theme-secondary\s*\{\s*background-color: var\(--bs-btn-active-bg\) !important;/
    );
    expect(compiledStyles).toMatch(
      /\.bootstrap-select \.dropdown-toggle\s*\{\s*border-color: var\(--bs-border-color, #ced4da\);/
    );
  });
});
