<script lang="ts">
    interface Props {
        title?: string;
        class?: string; // Correctly define the prop as 'class'
        onclick?: (e: MouseEvent) => void;
        children?: import('svelte').Snippet;
    }
    // Correctly destructure 'class' and rename it to 'className'
    let { title = '', class: className = '', onclick, children }: Props = $props();

    // Base Tailwind classes corresponding to the original .graph-button
    const baseClasses = "inline-flex items-center justify-center bg-transparent border-none cursor-pointer p-[6px] rounded transition-colors duration-200";

    // Tailwind classes for specific variant behaviors, derived from className prop
    // This allows passing 'graph-button-important' or 'active' via className from parent
    let importantHoverClass = $derived(className.includes('graph-button-important') ? 'hover:bg-el-plain-important-bg' : 'hover:bg-black/5');
    let activeBgClass = $derived(className.includes('active') ? 'bg-el-plain-primary-bg' : '');

    // Combine all classes, ensuring external className can still add arbitrary classes
    let finalClasses = $derived(`${baseClasses} ${importantHoverClass} ${activeBgClass} ${className}`);
</script>

<button
    class={finalClasses}
    {title}
    onclick={onclick}
>
    {@render children?.()}
</button>

<style>
    /* 
       Ensures icons (img or svg) directly inside this button component have correct sizing and default opacity.
       Opacity is overridden on hover or when the button itself has the 'active' background class.
    */
    button :global(img), button :global(svg) {
        width: 16px;
        height: 16px;
        display: block;
        opacity: 0.8;
        transition: opacity 0.2s; /* Smooth transition for opacity changes */
    }

    /* On hover of the button, its icon children become fully opaque */
    button:hover :global(img), button:hover :global(svg) {
        opacity: 1;
    }

    /* If the button has the 'active' background class (e.g., bg-el-plain-primary-bg), 
       its icon children should also be fully opaque.
       We directly target the Tailwind class applied to the button for this.
    */
    button.bg-el-plain-primary-bg :global(img), button.bg-el-plain-primary-bg :global(svg) {
        opacity: 1;
    }
</style>
