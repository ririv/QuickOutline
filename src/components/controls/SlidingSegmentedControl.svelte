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

    // Calculate selected index for the sliding indicator
    let selectedIndex = $derived(options.findIndex(o => o.value === value));
    let count = $derived(options.length);

    function getButtonClass(isActive: boolean) {
        const base = "relative z-10 flex items-center justify-center flex-1 gap-1.5 px-3 py-1.5 text-xs font-medium rounded-full transition-colors duration-200 select-none";
        const active = "text-gray-900"; 
        const inactive = "text-gray-500 hover:text-gray-700";
        return `${base} ${isActive ? active : inactive} ${itemClass}`;
    }

    function handleClick(optionValue: string) {
        if (value !== optionValue) {
            value = optionValue;
            onchange?.(optionValue);
        }
    }
</script>

<div class={`relative flex p-1 bg-gray-100 rounded-full gap-0 ${className}`}>
    <!-- Sliding Indicator -->
    {#if selectedIndex !== -1}
        <div 
            class="absolute top-1 bottom-1 bg-white rounded-full shadow-sm ring-1 ring-black/5 transition-all duration-300 ease-[cubic-bezier(0.4,0,0.2,1)]"
            style="left: 4px; width: calc((100% - 8px) / {count}); transform: translateX({selectedIndex * 100}%);"
        ></div>
    {/if}

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