<script lang="ts">
  import HeaderFooterEditor from './HeaderFooterEditor.svelte';
  import CollapseTrigger from './CollapseTrigger.svelte';
  import { slide } from 'svelte/transition';
  import type { HeaderFooterConfig } from '@/lib/api/rpc.ts';

  interface Props {
      headerConfig: HeaderFooterConfig;
      footerConfig: HeaderFooterConfig;
      showHeader: boolean;
      showFooter: boolean;
      onHeaderChange?: () => void;
      onFooterChange?: () => void;
      children?: import('svelte').Snippet;
  }

  let {
      headerConfig = $bindable(),
      footerConfig = $bindable(),
      showHeader = $bindable(),
      showFooter = $bindable(),
      onHeaderChange,
      onFooterChange,
      children
  }: Props = $props();

  function toggleHeader() {
      showHeader = !showHeader;
  }

  function toggleFooter() {
      showFooter = !showFooter;
  }
</script>

<div class="page-layout-editor">
  <!-- Header Trigger & Editor -->
  <CollapseTrigger
    position="top"
    label="Header"
    expanded={showHeader}
    content={headerConfig}
    ontoggle={toggleHeader}
  />
  {#if showHeader}
    <div transition:slide={{ duration: 200 }}>
      <HeaderFooterEditor
        type="header"
        bind:config={headerConfig}
        onchange={onHeaderChange}
      />
    </div>
  {/if}

  <!-- Middle Content -->
  <div class="middle-content">
      {@render children?.()}
  </div>

  <!-- Footer Trigger & Editor -->
  {#if showFooter}
    <div transition:slide={{ duration: 200 }}>
      <HeaderFooterEditor
        type="footer"
        bind:config={footerConfig}
        onchange={onFooterChange}
      />
    </div>
  {/if}
  <CollapseTrigger
    position="bottom"
    label="Footer"
    expanded={showFooter}
    content={footerConfig}
    ontoggle={toggleFooter}
  />
</div>

<style>
    .page-layout-editor {
        height: 100%;
        display: flex;
        flex-direction: column;
    }

    .middle-content {
        flex: 1;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        position: relative;
    }
</style>
