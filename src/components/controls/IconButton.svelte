<script lang="ts">
  import { type Snippet } from 'svelte';

  interface Props {
    size?: 'sm' | 'md' | 'lg'; // sm: 24px, md: 32px, lg: 36px
    variant?: 'subtle' | 'primary' | 'danger' | 'solid';
    disabled?: boolean;
    title?: string;
    class?: string;
    children?: Snippet;
    onclick?: (e: MouseEvent) => void;
  }

  let { 
    size = 'md', 
    variant = 'subtle',
    disabled = false, 
    title = '', 
    class: className = '',
    children,
    onclick
  }: Props = $props();

  const baseClasses = "inline-flex items-center justify-center rounded-full transition-all duration-200 active:scale-95 disabled:opacity-50 disabled:pointer-events-none cursor-pointer p-0";

  const sizes = {
    sm: "w-6 h-6",
    md: "w-8 h-8", 
    lg: "w-9 h-9"
  };

  const variants = {
    subtle: "bg-transparent text-gray-500 hover:bg-violet-50 hover:text-violet-600",
    primary: "bg-violet-50 text-violet-600 hover:bg-violet-100 hover:text-violet-700",
    danger: "bg-transparent text-gray-400 hover:bg-red-50 hover:text-red-600",
    solid: "bg-violet-600 text-white hover:bg-violet-700 shadow-sm shadow-violet-200 dark:shadow-none"
  };
</script>

<button 
  type="button"
  {disabled}
  {title}
  class="{baseClasses} {sizes[size]} {variants[variant]} {className}"
  {onclick}
>
  {@render children?.()}
</button>
