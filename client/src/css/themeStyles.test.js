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

  it('uses the active theme text color for the editable page number', () => {
    expect(compiledStyles).toMatch(
      /input\.page-input\s*\{[^}]*color:\s*var\(--bs-body-color, #212529\)/
    );
  });

  it('uses the active theme text color for table action icons and links', () => {
    expect(compiledStyles).toMatch(
      /\.action-hover\.khq-row-action\s*\{\s*color: var\(--bs-body-color\);/
    );
    expect(compiledStyles).toMatch(
      /\.action-hover\.khq-row-action a,\s*\.action-hover\.khq-row-action a:hover\s*\{\s*color: var\(--bs-body-color\);/
    );
  });

  it('uses a softer dark palette and horizontal-only table separators', () => {
    expect(compiledStyles).toContain('--bs-body-bg: #0d1117;');
    expect(compiledStyles).toContain('--bs-body-color: #e6edf3;');
    expect(compiledStyles).toContain('--bs-secondary-bg: #161b22;');
    expect(compiledStyles).toMatch(
      /\[data-bs-theme=dark\] \.table\s*\{[^}]*--bs-table-striped-bg: transparent;/
    );
    expect(compiledStyles).toMatch(
      /\[data-bs-theme=dark\] \.table-bordered > :not\(caption\) > \* > \*\s*\{\s*border: 0;\s*border-bottom: var\(--bs-border-width\) solid var\(--bs-table-border-color\);/
    );
  });

  it('uses a soft light palette and horizontal-only table separators', () => {
    expect(compiledStyles).toContain('--bs-body-color: #1f2328;');
    expect(compiledStyles).toContain('--bs-secondary-bg: #f6f8fa;');
    expect(compiledStyles).toContain('--bs-tertiary-bg: #eaeef2;');
    expect(compiledStyles).toContain('--bs-border-color: #d1d9e0;');
    expect(compiledStyles).toMatch(
      /\[data-bs-theme=light\] \.table,\s*\[data-bs-theme=dark\] \.table\s*\{[^}]*--bs-table-striped-bg: transparent;/
    );
    expect(compiledStyles).toMatch(
      /\[data-bs-theme=light\] \.table-bordered > :not\(caption\) > \* > \*,\s*\[data-bs-theme=dark\] \.table-bordered > :not\(caption\) > \* > \*\s*\{\s*border: 0;\s*border-bottom: var\(--bs-border-width\) solid var\(--bs-table-border-color\);/
    );
  });

  it('keeps light sidebar labels dark and semibold', () => {
    expect(compiledStyles).toContain('--akhq-sidebar-color: #1f2328;');
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
    expect(compiledStyles).toContain('--akhq-sidebar-color: #e6edf3;');
    expect(compiledStyles).toContain('--akhq-sidebar-icon-color: #c9d1d9;');
    expect(compiledStyles).toContain('--akhq-sidebar-label-weight: 600;');
    expect(compiledStyles).toContain('--akhq-sidebar-active-color: #7ddcff;');
    expect(compiledStyles).toMatch(
      /\.sidenav---navtext---1AE_f > \*[^{}]*\{\s*color: var\(--akhq-sidebar-active-color, var\(--bs-primary, #005f81\)\)/
    );
    expect(compiledStyles).toContain('--akhq-sidebar-active-color: #005f81;');
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
