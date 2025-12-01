<script lang="ts">
    interface Option {
        value: string;
        label?: string;
        icon?: string;
        title?: string;
    }

    interface Props {
        value: string;
        options: Option[];
        class?: string;
        itemClass?: string;
        onchange?: (value: string) => void;
    }

    let { value = $bindable(), options, class: className = '', itemClass = '', onchange }: Props = $props();

    function getButtonClass(isActive: boolean) {
        const base = "flex items-center justify-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md transition-all duration-200 select-none";
        const active = "bg-white text-gray-900 shadow-sm ring-1 ring-black/5"; 
        const inactive = "text-gray-500 hover:text-gray-700 hover:bg-gray-200/50";
        return `${base} ${isActive ? active : inactive} ${itemClass}`;
    }

    function handleClick(optionValue: string) {
        if (value !== optionValue) {
            value = optionValue;
            onchange?.(optionValue);
        }
    }
</script>

<div class={`flex p-1 bg-gray-100 rounded-lg gap-1 ${className}`}>
    {#each options as option}
        <button 
            class={getButtonClass(value === option.value)}
            onclick={() => handleClick(option.value)}
            title={option.title}
            type="button"
        >
            {#if option.icon}
                <img src={option.icon} alt="" class="w-3.5 h-3.5 opacity-80" />
            {/if}
            {#if option.label}
                <span>{option.label}</span>
            {/if}
        </button>
    {/each}
</div>
