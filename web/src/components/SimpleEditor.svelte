<script lang="ts">
  // Svelte 5 Runes syntax
  interface Props {
    value?: string;
    placeholder?: string;
    onchange?: (val: string) => void;
  }

  let { 
    value = $bindable(''), 
    placeholder = '', 
    onchange 
  }: Props = $props();

  let textarea: HTMLTextAreaElement;
  const INDENT_STRING = '    '; // 4 spaces
  const INDENT_PATTERN = /^(\t|\s{1,4})/; // Matches tab or 1-4 spaces

  function handleInput(e: Event) {
    const target = e.target as HTMLTextAreaElement;
    value = target.value;
    onchange?.(value);
  }

  function handleKeydown(e: KeyboardEvent) {
    if (window.debugBridge) window.debugBridge.log(`[SimpleEditor] Keydown: key='${e.key}', code='${e.code}', keyCode=${e.keyCode}`);
    
    if (e.key === 'Tab' || e.code === 'Tab' || e.keyCode === 9) {
      e.preventDefault();
      if (window.debugBridge) window.debugBridge.log("[SimpleEditor] Processing Tab");
      
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const text = value;
      
      if (e.shiftKey) {
        removeIndent(start, end, text);
      } else {
        addIndent(start, end, text);
      }
    } else if (e.key === 'Enter') {
      e.preventDefault();
      handleEnter();
    }
  }

  // Helper: Get start position of the line containing index
  function getLineStart(text: string, index: number): number {
    const pos = text.lastIndexOf('\n', index - 1);
    return pos === -1 ? 0 : pos + 1;
  }

  // Helper: Get end position of the line containing index
  function getLineEnd(text: string, index: number): number {
    const pos = text.indexOf('\n', index);
    return pos === -1 ? text.length : pos;
  }

  function addIndent(start: number, end: number, text: string) {
    if (start === end) {
        // Insert indent at cursor
        const newVal = text.substring(0, start) + INDENT_STRING + text.substring(end);
        updateValue(newVal, start + INDENT_STRING.length, start + INDENT_STRING.length);
        return;
    }

    // Multiple lines selected
    let effectiveStart = start;
    let effectiveEnd = end;
    
    if (text[effectiveStart] === '\n') effectiveStart++;
    if (text[effectiveEnd - 1] === '\n') effectiveEnd--;

    const startLineStart = getLineStart(text, effectiveStart);
    const selectedText = text.substring(startLineStart, effectiveEnd);
    const lines = selectedText.split('\n');
    
    const newLines = lines.map(line => INDENT_STRING + line);
    const newBlock = newLines.join('\n');
    
    const newVal = text.substring(0, startLineStart) + newBlock + text.substring(effectiveEnd);
    
    updateValue(newVal, startLineStart, startLineStart + newBlock.length);
  }

  function removeIndent(start: number, end: number, text: string) {
    if (start !== end && text.substring(start, end).includes('\n')) {
        // Multiple lines
        let effectiveStart = start;
        let effectiveEnd = end;
        if (text[effectiveStart] === '\n') effectiveStart++;
        if (text[effectiveEnd - 1] === '\n') effectiveEnd--;

        const startLineStart = getLineStart(text, effectiveStart);
        const selectedText = text.substring(startLineStart, effectiveEnd);
        const lines = selectedText.split('\n');
        
        const newLines = lines.map(line => line.replace(INDENT_PATTERN, ''));
        const newBlock = newLines.join('\n');
        
        const newVal = text.substring(0, startLineStart) + newBlock + text.substring(effectiveEnd);
        updateValue(newVal, startLineStart, startLineStart + newBlock.length);

    } else {
        // Single line or cursor
        const lineStart = getLineStart(text, start);
        const lineEnd = getLineEnd(text, start); // use start to find current line
        const currentLine = text.substring(lineStart, lineEnd);
        
        const match = currentLine.match(INDENT_PATTERN);
        if (match) {
            const indentLen = match[0].length;
            const newLine = currentLine.substring(indentLen);
            const newVal = text.substring(0, lineStart) + newLine + text.substring(lineEnd);
            
            let newStart = start - indentLen;
            let newEnd = end - indentLen;
            
            if (newStart < lineStart) newStart = lineStart;
            if (newEnd < lineStart) newEnd = lineStart;
            
            updateValue(newVal, newStart, newEnd);
        }
    }
  }

  function handleEnter() {
      const start = textarea.selectionStart;
      const text = value;
      
      const lineStart = getLineStart(text, start);
      const currentLine = text.substring(lineStart, start);
      
      const match = currentLine.match(/^(\s*)/);
      const indent = match ? match[1] : '';
      
      const insert = '\n' + indent;
      const newVal = text.substring(0, start) + insert + text.substring(textarea.selectionEnd);
      
      updateValue(newVal, start + insert.length, start + insert.length);
  }

  function updateValue(newVal: string, newSelectionStart: number, newSelectionEnd: number) {
      value = newVal;
      textarea.value = newVal; // Force sync
      textarea.selectionStart = newSelectionStart;
      textarea.selectionEnd = newSelectionEnd;
      onchange?.(newVal);
  }
</script>

<div class="simple-editor">
  <textarea
    bind:this={textarea}
    bind:value={value}
    {placeholder}
    oninput={handleInput}
    onkeydown={handleKeydown}
    spellcheck="false"
  ></textarea>
</div>

<style>
  .simple-editor {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
  }
  
  textarea {
    flex: 1;
    width: 100%;
    height: 100%;
    resize: none;
    border: none;
    outline: none;
    padding: 10px;
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 14px;
    line-height: 1.5;
    background-color: transparent;
    color: #333;
    box-sizing: border-box;
  }
  
  textarea:focus {
    background-color: transparent;
  }
</style>
