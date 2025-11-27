<script lang="ts">
  import type { Snippet } from 'svelte';

  type ButtonType = 'primary' | 'important';
  type RippleColor = 'dark' | 'light';

  interface Props {
    type: ButtonType;
    rippleColor?: RippleColor;
    class?: string;
    children?: Snippet;
    [key:string]: any;
  }

  let {
    type,
    rippleColor = 'light', // Default to dark ripple for light buttons
    class: className,
    children,
    ...rest
  }: Props = $props();

  function rippleEffect(event: MouseEvent) {
    const btn = event.currentTarget as HTMLElement;
    const circle = document.createElement('span');
    const diameter = Math.max(btn.clientWidth, btn.clientHeight);
    const radius = diameter / 2;

    circle.style.width = circle.style.height = `${diameter}px`;
    circle.style.left = `${event.clientX - btn.getBoundingClientRect().left - radius}px`;
    circle.style.top = `${event.clientY - btn.getBoundingClientRect().top - radius}px`;
    
    // Add classes for styling
    circle.classList.add('ripple', rippleColor);
    
    // Remove any existing ripple to ensure only one is active
    const oldRipple = btn.querySelector('.ripple');
    if (oldRipple) {
      oldRipple.remove();
    }
    
    btn.appendChild(circle);

    // Clean up ripple after animation
    setTimeout(() => {
        if(circle.parentElement) {
            circle.parentElement.removeChild(circle);
        }
    }, 600);
  }

</script>

<button
  class="my-button plain-button-{type} {className || ''}"
  onmousedown={rippleEffect}
  {...rest}
>
  {@render children?.()}
</button>

<style>
  /* Component is clean, all styling is in global.css */
</style>
