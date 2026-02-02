<script lang="ts">
  import StatusBarItem from '../StatusBarItem.svelte';
  import HeaderFooterPopup from '@/components/statusbar/statusbar-popup/HeaderFooterPopup.svelte';
  import Icon from '@/components/Icon.svelte';
  import { type HeaderFooterLayout } from "@/lib/types/page.ts";

  interface Props {
    layout: HeaderFooterLayout;
    onchange?: () => void;
  }

  let { layout = $bindable(), onchange }: Props = $props();

  let summary = $derived.by(() => {
      const { headerDist, footerDist } = layout;
      if (headerDist !== undefined && footerDist !== undefined) {
           return {
               type: headerDist === footerDist ? 'both-equal' : 'both-diff',
               header: headerDist,
               footer: footerDist
           };
      } else if (headerDist !== undefined) {
          return { type: 'header-only', header: headerDist };
      } else if (footerDist !== undefined) {
          return { type: 'footer-only', footer: footerDist };
      }
      return null;
  });
</script>

<StatusBarItem id="header-footer" title="Header & Footer Position">
    {#snippet icon()}
        <Icon name="header-footer" width="14" height="14" />
    {/snippet}
    
    <span class="hf-summary-content">
      {#if summary}
          {#if summary.type === 'both-equal'}
              <Icon name="arrow-up-down" width="10" height="10" />{summary.header}mm
          {:else if summary.type === 'both-diff'}
              <Icon name="arrow-up" width="10" height="10" />{summary.header} <Icon name="arrow-down" width="10" height="10" />{summary.footer}mm
          {:else if summary.type === 'header-only'}
              <Icon name="arrow-up" width="10" height="10" />{summary.header}mm
          {:else if summary.type === 'footer-only'}
              <Icon name="arrow-down" width="10" height="10" />{summary.footer}mm
          {/if}
      {/if}
    </span>
    
    {#snippet popup(triggerEl)}
        <HeaderFooterPopup bind:layout {onchange} {triggerEl} />
    {/snippet}
</StatusBarItem>

<style>
  .hf-summary-content {
      display: inline-flex;
      align-items: center;
      gap: 2px;
  }

  .hf-summary-content :global(svg) {
      transform: translateY(-1px);
  }
</style>
