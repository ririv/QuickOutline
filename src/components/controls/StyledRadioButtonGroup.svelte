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

<div class="inline-flex overflow-hidden rounded-md border border-el-default-border">
  {#each options as option, i (option.value)}
<!--    使用 flex-1 min-w-0 或者 w-1/2，否则按钮宽度会不等分-->
    <label class="relative flex-1 min-w-0">
      <input 
        type="radio"
        {name}
        value={option.value}
        bind:group={value}
        class="sr-only"
      />
      <span
        class="block w-full cursor-pointer px-4 py-1 text-center text-sm text-el-default-text transition-colors duration-200"
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
