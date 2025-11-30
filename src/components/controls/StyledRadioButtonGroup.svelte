<script lang="ts">
  type Option = {
    value: string;
    label: string;
  }

  interface Props {
    options: Option[];
    value?: string;
    name: string;
  }

  let { 
    options,
    value = $bindable(),
    name
  }: Props = $props();

</script>

<div class="grid grid-flow-col auto-cols-fr overflow-hidden rounded-md border border-el-default-border">
  {#each options as option, i (option.value)}
    <label class="relative">
      <input 
        type="radio"
        {name}
        value={option.value}
        bind:group={value}
        class="sr-only"
      />
      <span
        class="flex items-center justify-center w-full h-full cursor-pointer px-2 py-1 text-sm text-el-default-text transition-colors duration-200 whitespace-nowrap"
        class:bg-el-primary={value === option.value}
        class:text-white={value === option.value}
        class:hover:text-el-primary={value !== option.value}
        class:border-r={i < options.length - 1}
        class:border-el-default-border={i < options.length - 1}
      >
        {option.label}
      </span>
    </label>
  {/each}
</div>
