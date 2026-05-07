<script lang="ts">
  import { type Snippet } from 'svelte';

  /* Props 定义 */
  interface Props {
    variant?: 'primary' | 'secondary' | 'subtle' | 'solid';
    size?: 'sm' | 'md' | 'lg';
    disabled?: boolean;
    type?: 'button' | 'submit' | 'reset';
    title?: string;
    class?: string;
    children?: Snippet; /* Svelte 5 插槽替代方案 */
    onclick?: (e: MouseEvent) => void; /* 直接接收事件处理函数 */
  }

  let { 
    variant = 'primary', 
    size = 'md', 
    disabled = false, 
    type = 'button', 
    title = '', 
    class: className = '',
    children,
    onclick
  }: Props = $props();

  /* 基础类 */
  const baseClasses = "inline-flex items-center justify-center gap-2 font-semibold transition-all duration-200 active:scale-95 disabled:opacity-50 disabled:pointer-events-none cursor-pointer";
  
  /* 变体类 (Tailwind) */
  const variants = {
    primary: "bg-violet-50 text-violet-600 hover:bg-violet-100 hover:text-violet-700",
    secondary: "bg-gray-100 text-gray-700 hover:bg-gray-200",
    subtle: "bg-transparent text-gray-500 hover:bg-violet-50 hover:text-violet-600",
    solid: "bg-violet-600 text-white hover:bg-violet-700 shadow-sm shadow-violet-200 dark:shadow-none"
  };

  /* 尺寸类 */
  const sizes = {
    sm: "px-3 py-1.5 text-xs rounded-md",
    md: "px-4 py-2 text-sm rounded-lg",
    lg: "px-6 py-3 text-base rounded-xl"
  };

  const computedSize = $derived(size);
</script>

<button 
  {type}
  {disabled}
  {title}
  class="{baseClasses} {variants[variant]} {sizes[computedSize]} {className}"
  {onclick}
>
  {@render children?.()}
</button>
