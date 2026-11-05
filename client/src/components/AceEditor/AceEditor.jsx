import React, { useMemo } from 'react';
import CodeMirror from '@uiw/react-codemirror';
import { json } from '@codemirror/lang-json';
import { sql } from '@codemirror/lang-sql';
import { copilot } from '@uiw/codemirror-theme-copilot';
import { EditorView } from '@codemirror/view';
import { EditorState } from '@codemirror/state';
import { search } from '@codemirror/search';
import { StreamLanguage } from '@codemirror/language';
import { protobuf } from '@codemirror/legacy-modes/mode/protobuf';
import { properties } from '@codemirror/legacy-modes/mode/properties';
import { useTheme } from '../../context/ThemeContext';

/**
 * Drop-in replacement for react-ace using @uiw/react-codemirror (CodeMirror 6).
 */
const AceEditor = ({ mode, value, onChange, readOnly, style, ...rest }) => {
  const { resolvedTheme } = useTheme();

  // CodeMirror's outer div doesn't propagate height/minHeight into the editor.
  // Extract them from style and apply via EditorView.theme on .cm-editor instead.
  const { height, minHeight, maxHeight, ...remainingStyle } = style || {};

  const sizeTheme = useMemo(() => {
    const editorStyle = {};
    if (minHeight) editorStyle.minHeight = minHeight;
    if (height) editorStyle.height = height;
    if (maxHeight) editorStyle.maxHeight = maxHeight;
    return EditorView.theme({
      '&': editorStyle,
      '.cm-scroller': { maxHeight: maxHeight ?? 'unset', overflow: 'auto' },
      '.cm-content': { fontSize: '12px' }
    });
  }, [height, minHeight, maxHeight]);

  const customBackground = useMemo(() => {
    if (resolvedTheme === 'light') {
      return EditorView.theme(
        {
          '&': { backgroundColor: '#f8f9fa !important', color: '#212529' },
          '.cm-gutters': {
            backgroundColor: '#f0f0f0 !important',
            color: '#6c757d',
            borderRight: '1px solid #dee2e6'
          },
          '.cm-activeLine': { backgroundColor: '#e9ecef' },
          '.cm-activeLineGutter': { backgroundColor: '#e2e6ea' }
        },
        { dark: false }
      );
    }
    return EditorView.theme(
      {
        '&': { backgroundColor: '#171819 !important', color: '#e0e0e0' },
        '.cm-gutters': {
          backgroundColor: '#171819 !important',
          color: '#888',
          borderRight: '1px solid #303030'
        },
        '.cm-activeLine': { backgroundColor: '#1e2021' },
        '.cm-activeLineGutter': { backgroundColor: '#1e2021' }
      },
      { dark: true }
    );
  }, [resolvedTheme]);

  const extensions = useMemo(() => {
    const exts = [customBackground, sizeTheme, EditorView.lineWrapping, search({ top: true })];

    switch (mode) {
      case 'json':
        exts.push(json());
        break;
      case 'sql':
        exts.push(sql());
        break;
      case 'protobuf':
        exts.push(StreamLanguage.define(protobuf));
        break;
      case 'properties':
        exts.push(StreamLanguage.define(properties));
        break;
      default:
        // plain text — no extension needed
        break;
    }

    if (readOnly) {
      exts.push(EditorState.readOnly.of(true));
    }

    return exts;
  }, [mode, readOnly, sizeTheme, customBackground]);

  return (
    <CodeMirror
      value={value || ''}
      theme={resolvedTheme === 'light' ? 'light' : copilot}
      extensions={extensions}
      onChange={onChange}
      style={remainingStyle}
      basicSetup={{
        lineNumbers: true,
        foldGutter: true,
        autocompletion: !readOnly
      }}
      {...rest}
    />
  );
};

export default AceEditor;
