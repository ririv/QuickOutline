<script lang="ts">
  import StatusBarItem from '../StatusBarItem.svelte';
  import ColumnLayoutPopup from '@/components/statusbar/statusbar-popup/ColumnLayoutPopup.svelte';
  import Icon from '@/components/Icon.svelte';
  import { type ColumnLayoutConfig } from "@/lib/types/page.ts";
  import doubleColumnIcon from '@/assets/icons/double-column.svg?raw';

  interface Props {
    layout: ColumnLayoutConfig;
    onchange?: () => void;
  }

  let { layout = $bindable(), onchange }: Props = $props();
</script>

<StatusBarItem id="column-layout" title="Column Layout">
    {#snippet icon()}
        <Icon data={doubleColumnIcon} width="14" height="14" />
    {/snippet}
    {(layout.count || 1) === 1 ? '1 Col' : `${layout.count} Cols`}
    {#snippet popup(triggerEl)}
        <ColumnLayoutPopup bind:layout {onchange} {triggerEl} />
    {/snippet}
</StatusBarItem>
