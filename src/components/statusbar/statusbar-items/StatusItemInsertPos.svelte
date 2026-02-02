<script lang="ts">
  import StatusBarItem from '../StatusBarItem.svelte';
  import InsertPositionPopup from '@/components/statusbar/statusbar-popup/InsertPositionPopup.svelte';
  import Icon from '@/components/Icon.svelte';

  export interface InsertionSettings {
    pos: number;
    autoCorrect: boolean;
    showAutoCorrect: boolean;
  }

  interface Props {
    insertion: InsertionSettings;
    onchange?: () => void;
  }

  let { insertion = $bindable(), onchange }: Props = $props();
</script>

<StatusBarItem id="insert-pos" title="Set Insert Position">
    {#snippet icon()}
        <Icon name="insert-position" width="14" height="14" />
    {/snippet}
    Pos {insertion.pos}
    {#snippet popup(triggerEl)}
        <InsertPositionPopup 
            bind:insertPos={insertion.pos} 
            bind:autoCorrect={insertion.autoCorrect} 
            showAutoCorrect={insertion.showAutoCorrect} 
            {triggerEl} 
            {onchange}
        />
    {/snippet}
</StatusBarItem>
