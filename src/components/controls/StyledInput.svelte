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
        oninput?: (e: Event) => void;
        onchange?: (e: Event) => void;
    }

    let {
        value = $bindable(''),
        type = 'text',
        placeholder = '',
        disabled = false,
        id = '',
        className = '',
        min,
        step,
        autofocus = false,
        oninput,
        onchange
    }: Props = $props();
</script>

<!-- svelte-ignore a11y_autofocus -->
<input
        {id}
        {type}
        bind:value
        {placeholder}
        {disabled}
        {min}
        {step}
        {autofocus}
        class="styled-input {className}"
        {oninput}
        {onchange}
/>

<style>
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
