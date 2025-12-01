<script lang="ts">
    interface Option {
        value: string;
        icon: string;
        label: string;
        title?: string;
    }

    interface Props {
        value: string;
        options: Option[];
        class?: string;
    }

    let { value = $bindable(), options, class: className = '' }: Props = $props();

    let selectedOption = $derived(options.find(o => o.value === value) || options[0]);
    let selectedIndex = $derived(options.findIndex(o => o.value === value));
</script>

<div class={`inline-flex items-center gap-3 ${className}`}>
    <!-- Capsule Switch -->
    <div 
        class="relative flex p-0.5 bg-gray-100 rounded-full gap-2 select-none shadow-sm border border-gray-200 cursor-pointer"
        onclick={() => value = options[(selectedIndex + 1) % options.length].value}
    >
        <!-- Sliding Indicator -->
        <div 
            class="absolute top-0.5 bottom-0.5 bg-white rounded-full shadow-sm ring-1 ring-black/5 transition-all duration-300 ease-[cubic-bezier(0.4,0,0.2,1)] pointer-events-none"
            style="left: 2px; width: 24px; transform: translateX({selectedIndex * 32}px);"
        ></div>

        {#each options as option}
            <button 
                class="relative z-10 flex items-center justify-center w-6 h-6 rounded-full transition-colors duration-200 outline-none cursor-pointer"
                title={option.title || option.label}
                type="button"
            >
                <img 
                    src={option.icon} 
                    alt="" 
                    class="w-3.5 h-3.5 transition-all duration-200 {value === option.value ? 'opacity-100 scale-110' : 'opacity-50 grayscale hover:opacity-75'}" 
                />
            </button>
        {/each}
    </div>

    <!-- Label on the right -->
    <span class="text-xs font-medium text-gray-600 min-w-[60px] transition-all" key={selectedOption.value}>
        {selectedOption.label}
    </span>
</div>
