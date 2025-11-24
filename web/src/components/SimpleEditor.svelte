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

  function handleInput(e: Event) {
    const target = e.target as HTMLTextAreaElement;
    value = target.value;
    onchange?.(value);
  }

  function handleKeydown(e: KeyboardEvent) {
    if (e.key === 'Tab') {
      e.preventDefault();
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const text = value; // Use bound value
      
      if (e.shiftKey) {
        // Unindent (Shift + Tab) - Placeholder
      } else {
        // Indent (Tab)
        const indent = '    ';
        // Update value
        value = text.substring(0, start) + indent + text.substring(end);
        
        // Restore selection/cursor (need tick? usually direct DOM update is safer for cursor)
        // Svelte updates are async, so setting selection immediately after value update might fail if DOM hasn't updated.
        // However, since we bind value, Svelte will update DOM.
        // A safer way is to manually update DOM value then tell Svelte, or use tick().
        // Let's try direct DOM manipulation for cursor stability, Svelte binding will catch up.
        
        // Actually, in Svelte 5, let's trust the binding but we might need to restore cursor position after update.
        // Direct DOM update + notify change is often smoother for textareas.
        
        // Let's stick to the previous logic style but adapted:
        textarea.value = value; // Force update DOM to set selection immediately
        textarea.selectionStart = textarea.selectionEnd = start + indent.length;
        
        // Trigger callback
        onchange?.(value);
      }
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const start = textarea.selectionStart;
      const text = value;
      
      const lastNewLine = text.lastIndexOf('\n', start - 1);
      const currentLineStart = lastNewLine + 1;
      const currentLine = text.substring(currentLineStart, start);
      
      const match = currentLine.match(/^(\s*)/);
      const indent = match ? match[1] : '';
      
      const insert = '\n' + indent;
      value = text.substring(0, start) + insert + text.substring(textarea.selectionEnd);
      
      textarea.value = value;
      textarea.selectionStart = textarea.selectionEnd = start + insert.length;
      
      onchange?.(value);
    }
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
    background-color: #f8f9fa;
    color: #333;
    box-sizing: border-box;
  }
  
  textarea:focus {
    background-color: #fff;
  }
</style>