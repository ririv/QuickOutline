<script lang="ts">
    import DraggableWindow from '../controls/DraggableWindow.svelte';
    import Icon from '@/components/Icon.svelte';

    interface Props {
        visible: boolean;
        onClose: () => void;
        title?: string;
        children?: import('svelte').Snippet;
        actions?: import('svelte').Snippet;
    }

    let { visible = $bindable(false), onClose, title = "Guide", children, actions }: Props = $props();
</script>

<DraggableWindow 
    {title} 
    bind:visible 
    {onClose} 
    {actions}
    width="500px" 
    height="600px"
    initialX={500}
    initialY={100}
>
    {#snippet closeIcon()}
        <Icon name="add" width="14" height="14" style="transform: rotate(45deg);" />
    {/snippet}

    {@render children?.()}
</DraggableWindow>
