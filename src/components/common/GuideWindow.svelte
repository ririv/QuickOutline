<script lang="ts">
    import DraggableWindow from './DraggableWindow.svelte';

    interface Props {
        visible: boolean;
        onClose: () => void;
        title?: string;
        children?: import('svelte').Snippet;
    }

    let { visible = $bindable(false), onClose, title = "Guide", children }: Props = $props();
</script>

<DraggableWindow 
    {title} 
    bind:visible 
    {onClose} 
    width="500px" 
    height="600px"
    initialX={800}
    initialY={100}
>
    {@render children?.()}
</DraggableWindow>
