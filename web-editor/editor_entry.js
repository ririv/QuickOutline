import { EditorState } from '@codemirror/state';
import { EditorView, lineNumbers, highlightActiveLine, highlightActiveLineGutter, drawSelection, dropCursor } from '@codemirror/view';
import { markdown } from '@codemirror/lang-markdown';
import { syntaxHighlighting, defaultHighlightStyle } from '@codemirror/language';

// 可选：后续可以加入主题/快捷键扩展
// import { oneDark } from '@codemirror/theme-one-dark';
// import { keymap } from '@codemirror/view';

window.CodeMirrorBootstrap = function(parent, initialDoc, onChange) {
  if (!parent) throw new Error('Parent element missing for CodeMirrorBootstrap');
  if (window.__cm6_initialized) return window.editorView;
  if (window.__CM6_LIB_LOADED) console.warn('[CM6 DIAG] Library already loaded earlier'); else window.__CM6_LIB_LOADED = true;

  const updateListener = EditorView.updateListener.of(u => {
    if (u.docChanged && onChange) onChange(u.state.doc.toString());
  });

  const candidates = [
    { name: 'markdown()', ext: markdown() },
    { name: 'syntaxHighlighting(defaultHighlightStyle)', ext: syntaxHighlighting(defaultHighlightStyle) },
    { name: 'lineNumbers()', ext: lineNumbers() },
    { name: 'highlightActiveLine()', ext: highlightActiveLine() },
    { name: 'highlightActiveLineGutter()', ext: highlightActiveLineGutter() },
    { name: 'drawSelection()', ext: drawSelection() },
    { name: 'dropCursor()', ext: dropCursor() },
    { name: 'EditorView.lineWrapping', ext: EditorView.lineWrapping },
    { name: 'updateListener', ext: updateListener }
  ];

  const goodExts = [];
  candidates.forEach(c => {
    try {
      EditorState.create({ doc: initialDoc || '', extensions: [c.ext] });
      console.log('[CM6 DIAG TEST] OK:', c.name);
      goodExts.push(c.ext);
    } catch (e) {
      console.log('[CM6 DIAG TEST] FAIL:', c.name, '->', e);
    }
  });

  console.log('[CM6 DIAG TEST] Passed extensions count =', goodExts.length);
  const state = EditorState.create({ doc: initialDoc || '', extensions: goodExts });
  const view = new EditorView({ state, parent });
  window.__cm6_initialized = true;
  window.editorView = view;
  return view;
};
