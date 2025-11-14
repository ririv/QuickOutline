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

  // 简化：取消 JS 端防抖，直接发送，每次内容变化都回调；只依赖 Java 侧 DebouncedPreviewer 统一限频
  // 诊断: JS 层变更 -> Java 回调链路日志
  let __changeSeq = 0;
  let lastSentLen = 0;
  let lastDocLen = 0;
  let lastChangeTime = 0;
  const updateListener = EditorView.updateListener.of(u => {
    if (!u.docChanged) {
      if (u.selectionSet) console.log('[CM6 DEBUG] selection changed (no doc change)');
      return;
    }
    const txt = u.state.doc.toString();
    const len = txt.length;
    lastDocLen = len;
    lastChangeTime = Date.now();
    const seq = ++__changeSeq;
    console.log('[CM6 DEBUG] change #' + seq + ' len=' + len);
    if (window.javaCallback?.log) {
      try { window.javaCallback.log('[CM6 DEBUG] JS->Java notifyUpdated seq=' + seq + ' len=' + len); } catch(e) {}
    }
    // 新机制：仅发送信号，不直接传递整段文本，交由 Java 端主动 pull
    try { window.javaCallback?.notifyUpdated && window.javaCallback.notifyUpdated(seq); } catch(e) { console.warn('[CM6 notifyUpdated error]', e); }
  });

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

  const candidates = [
    { name: 'markdown()', ext: markdown() },
    { name: 'syntaxHighlighting(defaultHighlightStyle)', ext: syntaxHighlighting(defaultHighlightStyle) },
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
  // 输入/焦点事件诊断
  view.dom.addEventListener('keydown', e => {
    console.log('[CM6 EVT] keydown key=' + e.key + ' code=' + e.code);
  });
  view.dom.addEventListener('input', e => {
    console.log('[CM6 EVT] input event valueLen=' + view.state.doc.length);
  });
  view.dom.addEventListener('compositionstart', e => {
    console.log('[CM6 EVT] compositionstart');
  });
  view.dom.addEventListener('compositionupdate', e => {
    console.log('[CM6 EVT] compositionupdate data=' + (e.data||''));
  });
  view.dom.addEventListener('compositionend', e => {
    console.log('[CM6 EVT] compositionend finalData=' + (e.data||''));
  });
  view.dom.addEventListener('focus', () => console.log('[CM6 EVT] focus'));
  view.dom.addEventListener('blur', () => console.log('[CM6 EVT] blur'));

  // 手动调试函数：可在 WebView 控制台执行 window.debugDumpContent()
  window.debugDumpContent = function() {
    const txt = view.state.doc.toString();
    console.log('[CM6 DEBUG] manual dump len=' + txt.length + ' head="' + txt.slice(0,50).replace(/\n/g,'\\n') + '"');
    return txt;
  };
  window.editorViewFocus = function(){
    try { view.focus(); console.log('[CM6 DEBUG] editorView.focus() invoked'); } catch(e){ console.log('[CM6 DEBUG] focus error', e); }
  };

  // 轮询回退：若由于某种原因 notifyUpdated 未被 Java 捕获，长度变化仍强制触发一次通知
  setInterval(() => {
    try {
      const len = view.state.doc.length;
      if (len !== lastSentLen) {
        lastSentLen = len;
        if (window.javaCallback?.log) {
          try { window.javaCallback.log('[CM6 FALLBACK] poll len=' + len); } catch(e) {}
        }
        window.javaCallback?.notifyUpdated && window.javaCallback.notifyUpdated(__changeSeq);
      }
    } catch(e) {
      console.warn('[CM6 FALLBACK poll error]', e);
    }
  }, 800);
  window.__cm6_initialized = true;
  window.editorView = view;
  if (window.javaCallback?.log) {
    try { window.javaCallback.log('[CM6 DEBUG] EditorView initialized. initialDocLen=' + (initialDoc?initialDoc.length:0)); } catch(e) {}
  }
  return view;
};
