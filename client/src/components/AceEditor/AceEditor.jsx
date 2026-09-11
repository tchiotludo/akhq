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

// Overrides the background on top of any base theme
const customBackground = EditorView.theme({
  '&': { backgroundColor: '#171819 !important' },
  '.cm-gutters': { backgroundColor: '#171819 !important' },
  '.cm-activeLine': { backgroundColor: '#1e2021' },
  '.cm-activeLineGutter': { backgroundColor: '#1e2021' },
}, { dark: true });

/**
 * Drop-in replacement for react-ace using @uiw/react-codemirror (CodeMirror 6).
 */
const AceEditor = ({
  mode,
  value,
  onChange,
  readOnly,
  style,
  ...rest
}) => {
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
  }, [mode, readOnly, sizeTheme]);

  return (
    <CodeMirror
      value={value || ''}
      theme={copilot}
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
