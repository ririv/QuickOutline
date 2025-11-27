<script lang="ts">
  type Option = {
    value: string;
    label: string;
  };

  interface Props {
    options: Option[];
    value?: string;
    name: string;
    class?: string;
  }

  let {
    options,
    value = $bindable(),
    name,
    class: className,
  }: Props = $props();

</script>

<div class="{className}">
  {#each options as option (option.value)}
    <label class="group flex cursor-pointer items-center select-none">
      <input 
        type="radio"
        {name}
        value={option.value}
        bind:group={value}
        class="peer sr-only"
      />
      
      <!-- The custom radio circle -->
      <div 
        class="radio-circle flex h-4 w-4 shrink-0 items-center justify-center rounded-full border-[1.5px] border-el-default-border transition-all duration-200 ease-in-out group-hover:border-el-primary peer-checked:border-el-primary"
      >
        <!-- The inner dot -->
        <div class="radio-dot h-2 w-2 rounded-full bg-transparent transition-all duration-200 ease-in-out"></div>
      </div>

      <!-- The label text -->
      <span class="ml-[5px] text-el-default-text transition-colors duration-200 ease-in-out group-hover:text-el-primary">
        {option.label}
      </span>
    </label>
  {/each}
</div>

<style>
    /* 
      Tailwind's `peer-checked` only works on direct siblings. 
      To style the inner dot (a child of a sibling), we use a standard CSS rule.
      This targets the `.radio-dot` only when the `.peer` input is checked.
      We use the CSS variable defined in `global.css`'s `@theme` block.
    */
    .peer:checked ~ .radio-circle > .radio-dot {
        background-color: var(--color-el-primary);
    }
</style>
