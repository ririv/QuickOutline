import { EditorState } from '@codemirror/state';
import { EditorView, lineNumbers, highlightActiveLine, highlightActiveLineGutter, drawSelection, dropCursor, keymap } from '@codemirror/view';
import { markdown } from '@codemirror/lang-markdown';
import { syntaxHighlighting, defaultHighlightStyle, HighlightStyle } from '@codemirror/language';
import { tags } from '@lezer/highlight';
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

  // 简化：取消 JS 端防抖，直接发送，每次内容变化都回调；只依赖 Java 侧 DebouncedPreviewer 统一限频
  // 诊断: JS 层变更 -> Java 回调链路日志
  // 仅轮询方案：不在 JS 侧触发任何跨桥回调，Java 侧自行轮询 window.getContent()

  // Compose keymap: default + history + search (+ completion 可按需再开启)
  const combinedKeymap = [...defaultKeymap, ...historyKeymap, ...searchKeymap /*, ...completionKeymap*/];

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

  // Define comprehensive Markdown syntax highlighting using CSS classes
  // These classes map to styles defined in markdown-highlight.css
  const markdownHighlighting = HighlightStyle.define([
    { tag: tags.heading, class: 'cm-heading' },
    { tag: tags.heading1, class: 'cm-heading1' },
    { tag: tags.heading2, class: 'cm-heading2' },
    { tag: tags.heading3, class: 'cm-heading3' },
    { tag: tags.heading4, class: 'cm-heading4' },
    { tag: tags.heading5, class: 'cm-heading5' },
    { tag: tags.heading6, class: 'cm-heading6' },
    { tag: tags.strong, class: 'cm-strong' },
    { tag: tags.emphasis, class: 'cm-em' },
    { tag: tags.link, class: 'cm-link' },
    { tag: tags.url, class: 'cm-url' },
    { tag: tags.monospace, class: 'cm-code' },
    { tag: tags.quote, class: 'cm-quote' },
    { tag: tags.list, class: 'cm-list' },
    { tag: tags.punctuation, class: 'cm-punctuation' },
    { tag: tags.meta, class: 'cm-meta' },
    { tag: tags.bracket, class: 'cm-bracket' },
    { tag: tags.strikethrough, class: 'cm-strikethrough' },
    { tag: tags.escape, class: 'cm-escape' },
  ]);


  const candidates = [
    { name: 'markdown()', ext: markdown() },
    { name: 'lineNumbers()', ext: lineNumbers() },
    { name: 'highlightActiveLine()', ext: highlightActiveLine() },
    { name: 'highlightActiveLineGutter()', ext: highlightActiveLineGutter() },
    { name: 'drawSelection()', ext: drawSelection() },
    { name: 'dropCursor()', ext: dropCursor() },
    { name: 'history()', ext: history() },
    // 自动补全可按需恢复：取消注释下面一行
    // { name: 'autocompletion()', ext: autocompletion({override:[markdownExtraCompletions]}) },
    { name: 'highlightSelectionMatches()', ext: highlightSelectionMatches() },
    { name: 'keymap.of(combinedKeymap)', ext: keymap.of(combinedKeymap) },
    { name: 'EditorView.lineWrapping', ext: EditorView.lineWrapping },
    { name: 'syntaxHighlighting(defaultHighlightStyle)', ext: syntaxHighlighting(defaultHighlightStyle) },
    // Apply custom Markdown highlighting (maps to CSS classes in markdown-highlight.css)
    { name: 'syntaxHighlighting(markdownHighlighting)', ext: syntaxHighlighting(markdownHighlighting) }
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
  // 诊断事件日志移除：减少桥接调用数量，提升稳定性

  // 手动调试函数：可在 WebView 控制台执行 window.debugDumpContent()
  window.debugDumpContent = function() { return view.state.doc.toString(); };
  window.editorViewFocus = function(){ try { view.focus(); } catch(e){} };

  // 说明：JS 侧不再做任何跨桥通知或轮询；仅由 Java 侧定时轮询 window.getContent()。
  // 经尝试，JS->Java发送事件通知，在第一次渲染后，后续会无效（接受不到任何打字更新信息）
  window.__cm6_initialized = true;
  window.editorView = view;
  // 初始化日志移除，减少 Java 桥调用
  return view;
};
