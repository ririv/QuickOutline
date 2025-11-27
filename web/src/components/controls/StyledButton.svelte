<script lang="ts">
  import type { Snippet } from 'svelte';

  type ButtonType = 'primary' | 'important';

  interface Props {
    type: ButtonType;
    class?: string;
    children?: Snippet;
    [key:string]: any;
  }

  let {
    type,
    class: className,
    children,
    ...rest
  }: Props = $props();

  function rippleEffect(event: MouseEvent) {
    const btn = event.currentTarget as HTMLElement;

    // Create ripple element
    const circle = document.createElement('span');
    const diameter = Math.max(btn.clientWidth, btn.clientHeight);
    const radius = diameter / 2;

    circle.style.width = circle.style.height = `${diameter}px`;
    circle.style.left = `${event.clientX - btn.getBoundingClientRect().left - radius}px`;
    circle.style.top = `${event.clientY - btn.getBoundingClientRect().top - radius}px`;
    circle.classList.add('ripple');
    
    // Ensure only one ripple is active
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
  /* The ripple class and animation are now global, defined in global.css */
  /* This keeps the component cleaner and consolidates button styling. */
</style>
