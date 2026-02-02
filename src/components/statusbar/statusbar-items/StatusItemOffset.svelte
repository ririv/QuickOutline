<script lang="ts">
  import StatusBarItem from '../StatusBarItem.svelte';
  import OffsetPopup from '@/components/statusbar/statusbar-popup/OffsetPopup.svelte';
  import Icon from '@/components/Icon.svelte';

  interface Props {
    offset: number;
    onchange?: () => void;
  }

  let { offset = $bindable(), onchange }: Props = $props();
</script>

<StatusBarItem id="offset" title="Set Page Offset">
    {#snippet icon()}
        <Icon name="offset" width="14" height="14" />
    {/snippet}
    Offset {offset}
    {#snippet popup(triggerEl)}
        <OffsetPopup bind:offset {onchange} {triggerEl} />
    {/snippet}
</StatusBarItem>
