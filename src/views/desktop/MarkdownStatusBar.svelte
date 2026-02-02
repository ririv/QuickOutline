<script lang="ts">
  import StatusBar from '@/components/statusbar/StatusBar.svelte';
  import StatusBarGroup from '@/components/statusbar/StatusBarGroup.svelte';
  
  import StatusItemPageLabel from '@/components/statusbar/statusbar-items/StatusItemPageLabel.svelte';
  import StatusItemInsertPos from '@/components/statusbar/statusbar-items/StatusItemInsertPos.svelte';
  import StatusItemPageSize from '@/components/statusbar/statusbar-items/StatusItemPageSize.svelte';
  import StatusItemMargins from '@/components/statusbar/statusbar-items/StatusItemMargins.svelte';
  import StatusItemHeaderFooter from '@/components/statusbar/statusbar-items/StatusItemHeaderFooter.svelte';
  import Generate from '@/components/statusbar/statusbar-items/Generate.svelte';

  import { markdownStore } from '@/stores/markdownStore.svelte.js';
  import type { PageSizeDetectionState } from '@/lib/pdf-processing/usePdfPageSizeDetection.svelte';

  interface Props {
    onGenerate?: () => void;
    onParamChange?: () => void;
    layoutDetection?: PageSizeDetectionState;
  }

  let { 
    onGenerate,
    onParamChange,
    layoutDetection
  }: Props = $props();
  
  // Group expansion states
  let g1Expanded = $state(true);
  let g2Expanded = $state(true);
  let g3Expanded = $state(true);
</script>

<StatusBar>
  <!-- Group 1: Page Logic -->
  <StatusBarGroup bind:expanded={g1Expanded} title="Page Logic">
      <StatusItemPageLabel bind:pageLabel={markdownStore.pageLabel} onchange={onParamChange} />
  </StatusBarGroup>

  <!-- Group 2: Layout Basics -->
  <StatusBarGroup bind:expanded={g2Expanded} title="Layout Basics">
      <StatusItemInsertPos bind:insertion={markdownStore.insertionConfig} onchange={onParamChange} />
      <StatusItemPageSize 
          bind:pageSize={markdownStore.pageLayout.pageSize} 
          onchange={onParamChange} 
          {layoutDetection}
      />
  </StatusBarGroup>

  <!-- Group 3: Advanced Layout -->
  <StatusBarGroup bind:expanded={g3Expanded} title="Advanced Layout">
      <StatusItemMargins bind:margins={markdownStore.pageLayout.margins} onchange={onParamChange} />
      <StatusItemHeaderFooter bind:layout={markdownStore.hfLayout} onchange={onParamChange} />
  </StatusBarGroup>

  <div class="spacer"></div>

  <Generate {onGenerate} />
</StatusBar>

<style>
  .spacer { flex: 1; }
</style>