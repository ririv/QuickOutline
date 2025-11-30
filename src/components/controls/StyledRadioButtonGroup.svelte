<script lang="ts">
  type Option = {
    value: string;
    label: string;
    icon?: string;
  }

  interface Props {
    options: Option[];
    value?: string | null; // Allow null for deselection
    name?: string; // kept for compatibility, but not used for input anymore
    allowDeselect?: boolean;
    onchange?: (value: string | null) => void;
    hasBorder?: boolean; // New prop for border control
  }

  let { 
    options,
    value = $bindable(),
    allowDeselect = false,
    onchange,
    hasBorder = false // Default to no border
  }: Props = $props();

  function handleClick(optionValue: string) {
    let newValue: string | null;
    if (allowDeselect && value === optionValue) {
      newValue = null; // Deselect
    } else {
      newValue = optionValue;
    }
    value = newValue;
    onchange?.(newValue);
  }
</script>

<!-- Container: Flex for border collapsing logic -->
<div class="flex w-full {hasBorder ? 'isolate' : 'overflow-hidden rounded-md'}">
  {#each options as option (option.value)}
    <button
      class="flex items-center justify-center flex-1 p-2 transition-colors focus:outline-none
             {hasBorder 
                ? `-ml-[1px] first:ml-0 first:rounded-l-md last:rounded-r-md border
                   ${value === option.value 
                      ? 'border-el-primary text-el-primary bg-white z-10' 
                      : 'border-el-default-border text-gray-600 bg-white hover:text-el-primary z-0'}` 
                : `${value === option.value 
                      ? 'bg-blue-200 text-blue-800' 
                      : 'bg-transparent text-gray-600 hover:bg-gray-100'}`}"
      onclick={() => handleClick(option.value)}
      title={option.label} 
      type="button"
    >
      {#if option.icon}
        <img src={option.icon} alt={option.label} class="w-4 h-4" />
      {:else}
        <span class="text-sm whitespace-nowrap">{option.label}</span>
      {/if}
    </button>
  {/each}
</div>
