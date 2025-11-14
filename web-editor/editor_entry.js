import { EditorState } from '@codemirror/state';
import { EditorView, lineNumbers, highlightActiveLine, highlightActiveLineGutter, drawSelection, dropCursor, keymap } from '@codemirror/view';
import { markdown } from '@codemirror/lang-markdown';
import { syntaxHighlighting, defaultHighlightStyle } from '@codemirror/language';
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands';
import { highlightSelectionMatches, searchKeymap } from '@codemirror/search';
import { autocompletion, completionKeymap } from '@codemirror/autocomplete';

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

  // Compose keymap: default + history + search
  const combinedKeymap = [...defaultKeymap, ...historyKeymap, ...searchKeymap, ...completionKeymap];

  // Minimal custom Markdown completion source (headings, fenced code blocks, task list, table pipes)
  function markdownExtraCompletions(context) {
    const before = context.matchBefore(/(?:#+\s?|```[a-zA-Z]*|[-*]\s\[[ x]?\]|\|\s?|:?[a-zA-Z0-9_:-]{2,})$/);
    if (!before) return null; // No trigger pattern matched
    const token = before.text;
    let options = [];
    if (/^#+\s?$/.test(token)) {
      options = [1,2,3,4,5,6].map(l => ({label: '#'.repeat(l)+' ', type: 'keyword', info: 'Heading level '+l}));
    } else if (/^```[a-zA-Z]*$/.test(token)) {
      const langs = ['java','js','ts','json','xml','yaml','markdown','bash','sql'];
      options = langs.map(l => ({label: '```'+l+'\n', apply: '```'+l+'\n', type:'keyword', info: 'Code fence '+l}));
    } else if (/^[-*]\s\[[ x]?\]$/.test(token)) {
      options = [
        {label: '- [ ] ', apply: '- [ ] ', type:'keyword', info:'Empty task'},
        {label: '- [x] ', apply: '- [x] ', type:'keyword', info:'Completed task'}
      ];
    } else if (/^\|\s?$/.test(token)) {
      options = [
        {label: '| Col1 | Col2 |', apply: '| Col1 | Col2 |\n| --- | --- |\n', type:'text', info:'Table header'},
        {label: '| --- | --- |', type:'text', info:'Table separator'}
      ];
    } else if (/^[a-zA-Z0-9_:-]{2,}$/.test(token)) {
      // Simple emoji shortcut demo (extendable)
      const emojiMap = { smile:'😄', warning:'⚠️', fire:'🔥', check:'✅', x:'❌'};
      options = Object.entries(emojiMap)
        .filter(([name]) => name.startsWith(token.toLowerCase()))
        .map(([name, emoji]) => ({label: name, detail: emoji, apply: emoji+' ', type:'constant', info:'Emoji'}));
    }
    if (!options.length) return null;
    return {from: before.from, options};
  }

  const candidates = [
    { name: 'markdown()', ext: markdown() },
    { name: 'syntaxHighlighting(defaultHighlightStyle)', ext: syntaxHighlighting(defaultHighlightStyle) },
    { name: 'lineNumbers()', ext: lineNumbers() },
    { name: 'highlightActiveLine()', ext: highlightActiveLine() },
    { name: 'highlightActiveLineGutter()', ext: highlightActiveLineGutter() },
    { name: 'drawSelection()', ext: drawSelection() },
    { name: 'dropCursor()', ext: dropCursor() },
    { name: 'history()', ext: history() },
    // { name: 'autocompletion()', ext: autocompletion({override:[markdownExtraCompletions]}) },
    { name: 'highlightSelectionMatches()', ext: highlightSelectionMatches() },
    { name: 'keymap.of(combinedKeymap)', ext: keymap.of(combinedKeymap) },
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
