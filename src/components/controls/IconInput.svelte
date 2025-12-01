<script lang="ts">
    interface Props {
        value: string;
        icon?: string;
        placeholder?: string;
        width?: string;
        class?: string;
        oninput?: (e: Event) => void;
        [key: string]: any;
    }

    let { 
        value = $bindable(), 
        icon, 
        placeholder = '', 
        width = '60px', 
        class: className = '', 
        oninput,
        ...rest 
    }: Props = $props();
</script>

<div 
    class={`flex items-center px-2 py-1.5 bg-gray-50 border border-gray-200 rounded-md focus-within:border-blue-500 focus-within:ring-2 focus-within:ring-blue-100 transition-all group ${className}`}
    style="width: {width};"
>
    {#if icon}
        <img 
            src={icon} 
            alt="icon" 
            class="w-4 h-4 text-gray-400 mr-2 opacity-60 group-focus-within:opacity-100 transition-opacity select-none" 
        />
    {/if}
    <input 
        type="text" 
        class="bg-transparent border-none outline-none text-sm placeholder-gray-400 flex-1 w-0 min-w-0" 
        {placeholder}
        bind:value={value}
        {oninput}
        {...rest}
    />
</div>
