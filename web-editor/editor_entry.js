import { EditorState, EditorSelection } from '@codemirror/state';
import { EditorView, ViewPlugin, lineNumbers, highlightActiveLine, highlightActiveLineGutter, drawSelection, dropCursor, keymap } from '@codemirror/view';
import { markdown } from '@codemirror/lang-markdown';
import { syntaxHighlighting, defaultHighlightStyle, HighlightStyle } from '@codemirror/language';
import { tags } from '@lezer/highlight';
import { defaultKeymap, history, historyKeymap, indentMore, indentLess } from '@codemirror/commands';
import { highlightSelectionMatches, searchKeymap } from '@codemirror/search';
import { autocompletion, completionKeymap, acceptCompletion } from '@codemirror/autocomplete';
import { indentUnit } from '@codemirror/language';

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

  // === Custom Tab/Shift+Tab indent/outdent logic (VSCode-like) ===
  const customIndentKeymap = [
    {
      key: 'Tab',
      run: (view) => {
        // 若有补全面板，优先用 Tab 接受补全
        try { if (acceptCompletion(view)) return true; } catch(_) {}
        console.log('[TAB] Custom Tab keymap triggered');
        const selection = view.state.selection.main;
        const { from, to } = selection;
        const doc = view.state.doc;
        
        // Check if multiple lines are selected or single line is fully selected
        const fromLine = doc.lineAt(from);
        const toLine = doc.lineAt(to);
        const isMultiLine = fromLine.number !== toLine.number;
        const isSingleLineFullySelected = 
          fromLine.number === toLine.number && 
          from === fromLine.from && 
          to === fromLine.to;
        
        if (isMultiLine || isSingleLineFullySelected) {
          // Indent all selected lines
          console.log('[TAB] Indenting multiple lines or full line');
          return indentMore(view);
        } else {
          // Insert tab at cursor
          console.log('[TAB] Inserting tab character at cursor');
          view.dispatch({
            changes: { from, to, insert: '\t' },
            selection: { anchor: from + 1 }
          });
          return true;
        }
      }
    },
    {
      key: 'Shift-Tab',
      run: (view) => {
        // Always outdent selected lines
        return indentLess(view);
      }
    }
  ];

  // === Markdown helpers for toggle wrappers & link ===
  function toggleInlineWrapperCmd(marker) {
    return (view) => {
      try {
        const tr = view.state.changeByRange(r => {
          const from = r.from, to = r.to;
          const doc = view.state.doc;
          if (from === to) {
            const insert = marker + marker;
            return {
              changes: { from, to, insert },
              range: EditorSelection.cursor(from + marker.length)
            };
          } else {
            const leftStart = Math.max(0, from - marker.length);
            const left = doc.sliceString(leftStart, from);
            const right = doc.sliceString(to, Math.min(doc.length, to + marker.length));
            if (left === marker && right === marker) {
              return {
                changes: [
                  { from: to, to: to + marker.length, insert: '' },
                  { from: leftStart, to: from, insert: '' }
                ],
                range: EditorSelection.range(from - marker.length, to - marker.length)
              };
            } else {
              return {
                changes: [
                  { from: to, to, insert: marker },
                  { from, to: from, insert: marker }
                ],
                range: EditorSelection.range(from + marker.length, to + marker.length)
              };
            }
          }
        });
        view.dispatch(tr);
        return true;
      } catch (e) { console.warn('[CM6] toggleInlineWrapper error', e); }
      return false;
    };
  }

  function togglePairWrapperCmd(open, close) {
    return (view) => {
      try {
        const tr = view.state.changeByRange(r => {
          const from = r.from, to = r.to;
          const doc = view.state.doc;
          if (from === to) {
            return {
              changes: { from, to, insert: open + close },
              range: EditorSelection.cursor(from + open.length)
            };
          } else {
            const leftStart = Math.max(0, from - open.length);
            const rightEnd = Math.min(doc.length, to + close.length);
            const left = doc.sliceString(leftStart, from);
            const right = doc.sliceString(to, rightEnd);
            if (left === open && right === close) {
              return {
                changes: [
                  { from: to, to: rightEnd, insert: '' },
                  { from: leftStart, to: from, insert: '' }
                ],
                range: EditorSelection.range(from - open.length, to - open.length)
              };
            } else {
              return {
                changes: [
                  { from: to, to, insert: close },
                  { from, to: from, insert: open }
                ],
                range: EditorSelection.range(from + open.length, to + open.length)
              };
            }
          }
        });
        view.dispatch(tr);
        return true;
      } catch (e) { console.warn('[CM6] togglePairWrapper error', e); }
      return false;
    };
  }

  function insertOrEditLinkCmd() {
    // 简化：不再弹窗，直接包裹为 [选中文本]()，并把光标放在括号内；无选区时插入 []() 并把光标放在括号内
    return (view) => {
      try {
        const tr = view.state.changeByRange(r => {
          const from = r.from, to = r.to;
          if (from === to) {
            // 无选区：插入 []()，光标到 [] 内
            const insert = '[]()';
            return {
              changes: { from, to, insert },
              range: EditorSelection.cursor(from + 1) // inside []
            };
          } else {
            // 有选区：包裹为 [text]()，光标到 () 内
            const selText = view.state.doc.sliceString(from, to);
            const insert = `[${selText}]()`;
            return {
              changes: { from, to, insert },
              range: EditorSelection.cursor(from + selText.length + 3)
            };
          }
        });
        if (tr.changes.empty) return false;
        view.dispatch(tr);
        return true;
      } catch (e) { console.warn('[CM6] insertOrEditLink error', e); }
      return false;
    };
  }

  // === Internal heading completion inside markdown link URL ===
  function collectHeadings(state){
    const doc = state.doc;
    const res = [];
    let inFence = false;
    for (let i = 1; i <= doc.lines; i++) {
      const line = doc.line(i);
      const t = line.text;
      if (/^```/.test(t)) { inFence = !inFence; continue; }
      if (inFence) continue;
      const m = t.match(/^\s*(?:>\s*)*(#{1,6})\s+(.+?)\s*$/);
      if (m) {
        const level = m[1].length;
        const text = m[2];
        res.push({ lineNo: i, level, text });
      }
    }
    return res;
  }

  function slugifyHeading(text){
    try {
      const lower = text.trim().toLowerCase();
      return lower
        .replace(/[`~!@#$%^&*()_=+\[\]{};:'",.<>/?\\|]/g, '')
        .replace(/\s+/g, '-')
        .replace(/-+/g, '-');
    } catch(_e){ return text; }
  }

  function linkHeadingCompletion(context){
    const pos = context.pos;
    const doc = context.state.doc;
    const line = doc.lineAt(pos);
    const rel = pos - line.from;
    const s = line.text;
    // Find last '(' before cursor and check pattern [...](|here)
    const openParenIdx = s.lastIndexOf('(', rel);
    if (openParenIdx < 0) return null;
    const rb = openParenIdx - 1;
    if (rb < 0 || s[rb] !== ']') return null;
    const lb = s.lastIndexOf('[', rb);
    if (lb < 0) return null;
    const closeParenIdx = s.indexOf(')', rel);
    const urlFrom = line.from + openParenIdx + 1;
    const urlTo = closeParenIdx >= 0 ? line.from + closeParenIdx : pos;
    if (pos < urlFrom || pos > urlTo) return null;

    // 仅在括号内输入了 # 时触发补全（不在行首触发）
    const typed = doc.sliceString(urlFrom, pos);
    if (!typed.startsWith('#')) return null;

    const headings = collectHeadings(context.state);
    if (!headings.length) return null;
    // 基于 slug 去重，避免同名标题导致的重复项
    const seen = new Set();
    const options = [];
    for (const h of headings) {
      const slug = slugifyHeading(h.text);
      if (seen.has(slug)) continue;
      seen.add(slug);
      options.push({
        label: h.text,           // 仅显示标题文本，避免与 #slug 重复感
        type: `H${h.level}`,     // 用类型位显示 H1/H2/H3...
        filterText: slug,        // 让输入的 slug 进行过滤
        apply: slug              // 已输入 '#'
      });
    }
    // from/to 也排除 #，让过滤/替换更自然
    return { from: urlFrom + 1, to: urlTo, options };
  }

  const customMarkdownKeymap = [
    { key: 'Mod-b', run: toggleInlineWrapperCmd('**') },
    { key: 'Mod-i', run: toggleInlineWrapperCmd('*') },
    { key: 'Mod-u', run: togglePairWrapperCmd('<u>', '</u>') },
    { key: 'Mod-k', run: insertOrEditLinkCmd() },
  ];

  // Compose keymap: completion + custom indent + default + history + search
  const combinedKeymap = [
    ...completionKeymap,
    ...customIndentKeymap,
    ...customMarkdownKeymap,
    ...defaultKeymap,
    ...historyKeymap,
    ...searchKeymap
  ];

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


  // === 阻止 Tab 键的默认焦点遍历行为 ===
  // 使用 domEventHandlers 在 DOM 层拦截 Tab 键，阻止默认行为但允许 CodeMirror 处理
  const preventTabDefault = EditorView.domEventHandlers({
    keydown(event, view) {
      if (event.key === 'Tab') {
        console.log('[TAB] Tab key pressed, preventing default');
        event.preventDefault();  // 阻止默认的焦点遍历
        // 返回 false 让事件继续传递给 keymap 处理
        return false;
      }
      return false;
    }
  });

  const candidates = [
    { name: 'markdown()', ext: markdown() },
    { name: 'indentUnit.of("\\t")', ext: indentUnit.of('\t') }, // Use tab for indentation
    { name: 'lineNumbers()', ext: lineNumbers() },
    { name: 'highlightActiveLine()', ext: highlightActiveLine() },
    { name: 'highlightActiveLineGutter()', ext: highlightActiveLineGutter() },
    // 单击行号选中整行 + Shift 扩展（不含拖拽，拖拽后续单独完善）
    { name: 'gutterLineSelection', ext: ViewPlugin.fromClass(class {
      constructor(view){
        this.view = view;
        this.dragging = false;
        this.anchorLine = null; // 拖拽锚点（行）
        this.onMouseDown = this.onMouseDown.bind(this);
        this.onMouseMove = this.onMouseMove.bind(this);
        this.onMouseUp = this.onMouseUp.bind(this);
        // 用捕获阶段监听，确保能在 CodeMirror 默认处理前截获
        view.dom.addEventListener('mousedown', this.onMouseDown, true);
      }
      destroy(){
        this.view.dom.removeEventListener('mousedown', this.onMouseDown, true);
        window.removeEventListener('mousemove', this.onMouseMove, true);
        window.removeEventListener('mouseup', this.onMouseUp, true);
      }
      isInGutter(target){
        if (!target) return false;
        const el = target.nodeType === 3 ? target.parentElement : target;
        return !!(el && el.closest && (el.closest('.cm-gutters') || el.closest('.cm-gutter') || el.closest('.cm-lineNumbers') || el.closest('.cm-gutterElement')));
      }
      lineAtY(y){
        const rect = this.view.contentDOM.getBoundingClientRect();
        const x = rect.left + 2; // 靠内容区最左，避免受 gutter 宽度影响
        const pos = this.view.posAtCoords({ x, y });
        if (pos == null) return null;
        return this.view.state.doc.lineAt(pos);
      }
      fullLineRange(line){
        const doc = this.view.state.doc;
        const isLast = line.number === doc.lines;
        const from = line.from;
        const to = isLast ? line.to : doc.line(line.number + 1).from; // 选到下一行起点，包含换行
        return { from, to };
      }
      selectLinesBetween(lineA, lineB){
        const first = lineA.number <= lineB.number ? lineA : lineB;
        const last = lineA.number <= lineB.number ? lineB : lineA;
        const rangeA = this.fullLineRange(first);
        const rangeB = this.fullLineRange(last);
        const from = rangeA.from;
        const to = rangeB.to;
        this.view.dispatch({ selection: EditorSelection.single(from, to), scrollIntoView: false });
      }
      autoScrollIfNeeded(clientY){
        const sd = this.view.scrollDOM;
        const rect = sd.getBoundingClientRect();
        const margin = 16;
        const step = 24;
        if (clientY < rect.top + margin) sd.scrollTop -= step;
        else if (clientY > rect.bottom - margin) sd.scrollTop += step;
      }
      onMouseDown(ev){
        try {
          if (ev.button !== 0) return; // 仅处理左键
          if (!this.isInGutter(ev.target)) return;
          ev.preventDefault();
          const clicked = this.lineAtY(ev.clientY);
          if (!clicked) return;

          const sel = this.view.state.selection.main;
          if (ev.shiftKey && !sel.empty) {
            this.anchorLine = this.view.state.doc.lineAt(sel.anchor);
          } else {
            this.anchorLine = clicked;
          }

          // 初始选择
          this.selectLinesBetween(this.anchorLine, clicked);

          // 开始拖拽
          this.dragging = true;
          window.addEventListener('mousemove', this.onMouseMove, true);
          window.addEventListener('mouseup', this.onMouseUp, true);
        } catch(e) {
          console.warn('[CM6] gutterLineSelection mousedown error', e);
        }
      }
      onMouseMove(ev){
        if (!this.dragging) return;
        try {
          ev.preventDefault();
          this.autoScrollIfNeeded(ev.clientY);
          const cur = this.lineAtY(ev.clientY);
          if (!cur || !this.anchorLine) return;
          this.selectLinesBetween(this.anchorLine, cur);
        } catch(e) {
          console.warn('[CM6] gutterLineSelection mousemove error', e);
        }
      }
      onMouseUp(){
        if (!this.dragging) return;
        this.dragging = false;
        this.anchorLine = null;
        window.removeEventListener('mousemove', this.onMouseMove, true);
        window.removeEventListener('mouseup', this.onMouseUp, true);
      }
    }) },
    { name: 'drawSelection()', ext: drawSelection() },
    { name: 'dropCursor()', ext: dropCursor() },
    { name: 'history()', ext: history() },
  // 自动补全：仅开启“链接内输入 # 的内部标题补全”
    { name: 'autocompletion()', ext: autocompletion({override:[linkHeadingCompletion]}) },
    { name: 'highlightSelectionMatches()', ext: highlightSelectionMatches() },
    { name: 'keymap.of(combinedKeymap)', ext: keymap.of(combinedKeymap) },
    { name: 'preventTabDefault', ext: preventTabDefault },  // 放在 keymap 之后，先让 keymap 处理，然后阻止默认行为
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
