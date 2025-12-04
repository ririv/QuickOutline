<script lang="ts">
    interface Props {
        value?: string | number;
        type?: string;
        placeholder?: string;
        disabled?: boolean;
        id?: string;
        className?: string; // Optional custom class
        min?: string | number;
        step?: string | number;
        autofocus?: boolean;
        numericType?: 'integer' | 'unsigned-integer'; // Restrict input to integers (signed or unsigned)
        icon?: string; // Icon path
        width?: string; // Custom width
        oninput?: (e: Event) => void;
        onchange?: (e: Event) => void;
    }

    let {
        value = $bindable(''),
        type = 'text', // 默认 type 为 text
        placeholder = '',
        disabled = false,
        id = '',
        className = '',
        min,
        step,
        autofocus = false,
        numericType,
        icon,
        width,
        oninput,
        onchange
    }: Props = $props();

    // 内部实际渲染的 type，如果启用了数字限制，则强制为 text
    const actualType = numericType ? 'text' : type;

    function handleKeydown(e: KeyboardEvent) {
        if (!numericType) return;

        // 允许导航和编辑键
        const allowedKeys = [
            'Backspace', 'Delete', 'Tab', 'Enter', 'Escape',
            'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
            'Home', 'End'
        ];
        
        // 允许 Ctrl/Cmd + A/C/V/X/Z
        if ((e.ctrlKey || e.metaKey) && ['a', 'c', 'v', 'x', 'z'].includes(e.key.toLowerCase())) {
            return;
        }

        if (allowedKeys.includes(e.key)) {
            return;
        }

        // 允许数字 0-9
        if (/^[0-9]$/.test(e.key)) {
            return;
        }

        // 允许负号（仅当 numericType 为 'integer' 时）
        // 注意：这里不做严格的位置检查，只做简单放行，严格清洗在 input 事件中
        if (numericType === 'integer' && e.key === '-') {
            return;
        }

        // 阻止其他所有键
        e.preventDefault();
    }

    function handlePaste(e: ClipboardEvent) {
        if (!numericType) return;
        
        e.preventDefault();
        
        const pastedText = (e.clipboardData || (window as any).clipboardData).getData('text');
        
        // 简单的预清洗
        let cleanText = '';
        if (numericType === 'integer') {
             // 允许数字和负号，后续逻辑会处理位置
             cleanText = pastedText.replace(/[^0-9-]/g, '');
        } else {
             cleanText = pastedText.replace(/[^0-9]/g, '');
        }
        
        if (cleanText) {
             const inputEl = e.target as HTMLInputElement;
             const start = inputEl.selectionStart ?? 0;
             const end = inputEl.selectionEnd ?? 0;
             
             // 使用 setRangeText 替代 execCommand，并正确处理光标位置
             // 'end' 模式会将光标移到插入内容之后
             inputEl.setRangeText(cleanText, start, end, 'end');
             
             // 必须手动触发事件以通知 Svelte 和其他监听器
             inputEl.dispatchEvent(new Event('input', { bubbles: true }));
             inputEl.dispatchEvent(new Event('change', { bubbles: true }));
        }
    }

    // 处理内部输入事件，用于清洗由输入法(IME)或非键盘操作（如拖放）引入的非法字符
    function handleInternalInput(e: Event) {
        if (numericType) {
            const inputElement = e.target as HTMLInputElement;
            const originalValue = inputElement.value;
            
            let cleanedValue = '';
            
            if (numericType === 'integer') {
                // 允许负数的清洗逻辑
                // 1. 移除非数字和非负号
                let temp = originalValue.replace(/[^0-9-]/g, '');
                
                // 2. 处理负号位置：只能出现在开头，且只能有一个
                const hasMinus = temp.startsWith('-');
                temp = temp.replace(/-/g, ''); // 移除所有负号
                
                if (hasMinus) {
                    cleanedValue = '-' + temp;
                } else {
                    cleanedValue = temp;
                }
            } else {
                // 只允许正整数
                cleanedValue = originalValue.replace(/[^0-9]/g, '');
            }

            // 1. 立即修正 DOM，保证视觉上没有字母
            if (originalValue !== cleanedValue) {
                inputElement.value = cleanedValue;
            }

            // 2. 关键：强制将清洗后的值赋给 value 变量
            // 这样 Svelte (以及绑定的 Store) 才会收到干净的值，防止脏数据进入 Store
            // 并在后续重绘时被错误地显示出来。
            value = cleanedValue;
        }
        // 调用外部传入的 oninput 回调
        if (oninput) oninput(e);
    }
</script>

<!-- svelte-ignore a11y_autofocus -->
{#if icon}
    <div 
        class="styled-input-wrapper group {className}" 
        class:disabled={disabled}
        style={width ? `width: ${width};` : ''}
    >
        <img 
            src={icon} 
            alt="icon" 
            class="icon"
        />
        <input
            {id}
            type={actualType}
            bind:value
            {placeholder}
            {disabled}
            {min}
            {step}
            {autofocus}
            class="inner-input"
            onkeydown={handleKeydown}
            onpaste={handlePaste}
            oninput={handleInternalInput}
            {onchange}
        />
    </div>
{:else}
    <input
            {id}
            type={actualType}
            bind:value
            {placeholder}
            {disabled}
            {min}
            {step}
            {autofocus}
            class="styled-input {className}"
            style={width ? `width: ${width};` : ''}
            onkeydown={handleKeydown}
            onpaste={handlePaste}
            oninput={handleInternalInput}
            {onchange}
    />
{/if}

<style>
    /* Base styles shared or reused */
    .styled-input {
        background-color: white;
        border: 1px solid #dcdfe6; /* el-default-border */
        border-radius: 4px;
        color: #606266; /* el-default-text */
        padding: 6px 11px;
        font-size: 14px;
        outline: none;
        transition: border-color 0.2s, box-shadow 0.2s;
        width: 100%;
        box-sizing: border-box;
    }

    .styled-input:hover {
        border-color: #409eff; /* el-primary */
    }

    .styled-input:focus {
        border-color: #409eff; /* el-primary */
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2); /* el-primary-shadow */
    }

    .styled-input:disabled {
        background-color: #f5f7fa;
        border-color: #e4e7ed;
        color: #c0c4cc;
        cursor: not-allowed;
    }

    /* Wrapper mode (with icon) */
    .styled-input-wrapper {
        background-color: white;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
        padding: 6px 11px;
        display: flex;
        align-items: center;
        transition: border-color 0.2s, box-shadow 0.2s;
        box-sizing: border-box;
        width: 100%; /* Default width */
    }

    .styled-input-wrapper:hover {
        border-color: #409eff;
    }

    .styled-input-wrapper:focus-within {
        border-color: #409eff;
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
    }

    .styled-input-wrapper.disabled {
        background-color: #f5f7fa;
        border-color: #e4e7ed;
        cursor: not-allowed;
    }

    .icon {
        width: 16px;
        height: 16px;
        margin-right: 8px; /* Equivalent to mr-2 */
        opacity: 0.6;
        color: #99a1af; /* Equivalent to text-gray-400 */
        transition: opacity 0.2s;
        user-select: none;
    }

    .styled-input-wrapper:focus-within .icon {
        opacity: 1;
    }

    .inner-input {
        border: none;
        outline: none;
        background: transparent;
        color: #606266;
        font-size: 14px;
        flex: 1;
        width: 0; /* Allow flex shrink */
        padding: 0;
    }
    
    .inner-input:disabled {
        color: #c0c4cc;
        cursor: not-allowed;
    }

    /* Hide number input spin buttons */
    input[type="number"]::-webkit-outer-spin-button,
    input[type="number"]::-webkit-inner-spin-button {
      -webkit-appearance: none;
      margin: 0;
    }
    input[type="number"] {
      -moz-appearance: textfield;
      appearance: textfield;
    }
</style>