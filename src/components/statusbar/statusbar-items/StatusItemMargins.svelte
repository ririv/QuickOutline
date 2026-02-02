<script lang="ts">
  import StatusBarItem from '../StatusBarItem.svelte';
  import PageMarginsPopup from '@/components/statusbar/statusbar-popup/PageMarginsPopup.svelte';
  import { type PageMargins } from "@/lib/types/page.ts";

  interface Props {
    margins: PageMargins;
    onchange?: () => void;
  }

  let { margins = $bindable(), onchange }: Props = $props();

  let summary = $derived.by(() => {
      const { top, bottom, left, right } = margins;
      let marginText = '';
      
      if (top === bottom && left === right && top === left) {
          marginText = `${top}mm`;
      } else if (top === bottom && left === right) {
          marginText = `${top}x${left}mm`;
      } else {
          marginText = '*';
      }
      
      return `${marginText}`;
  });
</script>

<StatusBarItem id="page-margins" title="Margins: {summary}">
    {#snippet icon()}
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="1" stroke-opacity="0.2"></rect>
            <path d="M7 3v18M17 3v18M3 7h18M3 17h18" stroke-width="1.5" stroke-dasharray="2 2"></path>
        </svg>
    {/snippet}
    {summary}
    {#snippet popup(triggerEl)}
        <PageMarginsPopup bind:margins {onchange} {triggerEl} />
    {/snippet}
</StatusBarItem>
