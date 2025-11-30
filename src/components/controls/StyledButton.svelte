<script lang="ts">
  import type { Snippet } from 'svelte';

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

  // --- Ripple Effect Logic (unchanged) ---

  function rippleEffect(event: MouseEvent) {
    const btn = event.currentTarget as HTMLElement;
    const circle = document.createElement('span');
    const diameter = Math.max(btn.clientWidth, btn.clientHeight);
    const radius = diameter / 2;

    circle.style.width = circle.style.height = `${diameter}px`;
    circle.style.left = `${event.clientX - btn.getBoundingClientRect().left - radius}px`;
    circle.style.top = `${event.clientY - btn.getBoundingClientRect().top - radius}px`;

    circle.classList.add('ripple', rippleColor);

    const oldRipple = btn.querySelector('.ripple');
    if (oldRipple) {
      oldRipple.remove();
    }

    btn.appendChild(circle);

    setTimeout(() => {
        if(circle.parentElement) {
            circle.parentElement.removeChild(circle);
        }
    }, 600);
  }

</script>

<button
  bind:this={element}
  class={`${baseClasses} ${typeClasses[type]} ${className || ''}`}
  onmousedown={rippleEffect}
  onclick={onclick}
  {...rest}
>
  {@render children?.()}
</button>

<style>
  /* 
    Ripple effect styles are moved here from global.css.
    They are marked as :global() because the <span> is dynamically created in JavaScript.
  */
  :global(.ripple) {
    position: absolute;
    border-radius: 50%;
    transform: scale(0);
    animation: ripple-animation 600ms linear;
  }

  :global(.ripple.dark) {
    background-color: rgba(0, 0, 0, 0.1);
  }

  :global(.ripple.light) {
    background-color: rgba(255, 255, 255, 0.4);
  }

  @keyframes ripple-animation {
    to {
        transform: scale(4);
        opacity: 0;
    }
  }
</style>
