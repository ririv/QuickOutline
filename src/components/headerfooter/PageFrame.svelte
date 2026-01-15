<script lang="ts">
  import HeaderFooterEditor from './HeaderFooterEditor.svelte';
  import CollapseTrigger from './CollapseTrigger.svelte';
  import { slide } from 'svelte/transition';
  import type { HeaderFooterConfig } from '@/lib/types/header-footer.ts';
  import { type HeaderFooterLayout, defaultHeaderFooterLayout } from '@/lib/types/page';

  interface Props {
      headerConfig: HeaderFooterConfig;
      footerConfig: HeaderFooterConfig;
      hfLayout?: HeaderFooterLayout;
      showHeader: boolean;
      showFooter: boolean;
      onHeaderChange?: () => void;
      onFooterChange?: () => void;
      children?: import('svelte').Snippet;
  }

  let {
      headerConfig = $bindable(),
      footerConfig = $bindable(),
      hfLayout = $bindable(defaultHeaderFooterLayout),
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
        bind:padding={hfLayout.headerPadding}
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
        bind:padding={hfLayout.footerPadding}
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
