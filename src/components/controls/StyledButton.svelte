<script lang="ts">
  import type { Snippet } from 'svelte';
  import { ripple } from '@/lib/actions/ripple';

  type ButtonType = 'primary' | 'important';
  type RippleColor = 'dark' | 'light';

  interface Props {
    type: ButtonType;
    rippleColor?: RippleColor;
    class?: string;
    children?: Snippet;
    element?: HTMLButtonElement; // Define 'element' as a bindable prop
    onclick?: (event: MouseEvent) => void;  // 显示提供，让编辑器知道这个属性存在，提供类型检查（vscode不提供也可以，IDEA目前必须提供）
    [key:string]: any;
  }

  let {
    type,
    rippleColor = 'light', // Default to light ripple for these button types
    class: className,
    children,
    element = $bindable(), // Use $bindable() to make the 'element' prop two-way bindable
    onclick,
    ...rest
  }: Props = $props();

  // --- Tailwind CSS class definitions ---

  // Base styles that correspond to the old .my-button class
  const baseClasses = 'h-8 px-[15px] text-xs font-medium rounded inline-flex justify-center items-center whitespace-nowrap border select-none relative overflow-hidden transition-colors duration-200';

  // Type-specific styles
  const typeClasses = {
    primary: 'bg-el-plain-primary-bg text-el-primary border-el-plain-primary-border hover:bg-el-plain-primary-bg-hover',
    important: 'bg-el-plain-important-bg text-el-important border-el-plain-important-border hover:bg-el-plain-important-bg-hover'
  };
</script>

<button
  bind:this={element}
  class={`${baseClasses} ${typeClasses[type]} ${className || ''}`}
  use:ripple={{ color: rippleColor === 'dark' ? 'rgba(0,0,0,0.2)' : 'rgba(255,255,255,0.3)' }}
  onclick={onclick}
  {...rest}
>
  {@render children?.()}
</button>

<style>
  /* No more ripple styles needed here */
</style>
