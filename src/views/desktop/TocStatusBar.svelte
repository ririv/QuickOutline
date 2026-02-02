<script lang="ts">
  import StatusBar from '@/components/statusbar/StatusBar.svelte';
  import StatusBarGroup from '@/components/statusbar/StatusBarGroup.svelte';
  import Icon from '@/components/Icon.svelte';
  import Tooltip from '@/components/Tooltip.svelte';
  
  import StatusItemOffset from '@/components/statusbar/statusbar-items/StatusItemOffset.svelte';
  import StatusItemPageLabel from '@/components/statusbar/statusbar-items/StatusItemPageLabel.svelte';
  import StatusItemInsertPos from '@/components/statusbar/statusbar-items/StatusItemInsertPos.svelte';
  import StatusItemPageSize from '@/components/statusbar/statusbar-items/StatusItemPageSize.svelte';
  import StatusItemMargins from '@/components/statusbar/statusbar-items/StatusItemMargins.svelte';
  import StatusItemColumnLayout from '@/components/statusbar/statusbar-items/StatusItemColumnLayout.svelte';
  import StatusItemHeaderFooter from '@/components/statusbar/statusbar-items/StatusItemHeaderFooter.svelte';
  import Generate from '@/components/statusbar/statusbar-items/Generate.svelte';
  import Guide from '@/components/statusbar/statusbar-items/Guide.svelte';

  import { tocStore } from '@/stores/tocStore.svelte.js';
  import type { PageSizeDetectionState } from '@/lib/pdf-processing/usePdfPageSizeDetection.svelte';

  interface Props {
    onGenerate?: () => void;
    onParamChange?: () => void;
    onGuide?: () => void;
    layoutDetection?: PageSizeDetectionState;
  }

  let { 
    onGenerate,
    onParamChange,
    onGuide,
    layoutDetection
  }: Props = $props();
  
  // Group expansion states
  let g1Expanded = $state(true);
  let g2Expanded = $state(true);
  let g3Expanded = $state(true);
</script>

<StatusBar>
  <StatusBarGroup bind:expanded={g1Expanded} title="Page Logic">
      <StatusItemOffset bind:offset={tocStore.offset} onchange={onParamChange} />
      <StatusItemPageLabel bind:pageLabel={tocStore.pageLabel} onchange={onParamChange} />
  </StatusBarGroup>

  <StatusBarGroup bind:expanded={g2Expanded} title="Layout Basics">
      <StatusItemInsertPos bind:insertion={tocStore.insertionConfig} onchange={onParamChange} />
      <StatusItemPageSize 
          bind:pageSize={tocStore.pageLayout.pageSize} 
          onchange={onParamChange} 
          {layoutDetection}
      />
  </StatusBarGroup>

  <StatusBarGroup bind:expanded={g3Expanded} title="Advanced Layout">
      <StatusItemMargins bind:margins={tocStore.pageLayout.margins} onchange={onParamChange} />
      <StatusItemColumnLayout bind:layout={tocStore.columnLayout} onchange={onParamChange} />
      <StatusItemHeaderFooter bind:layout={tocStore.hfLayout} onchange={onParamChange} />
  </StatusBarGroup>

  <div class="spacer"></div>
  
  <Guide {onGuide} />
  <Generate {onGenerate} />
</StatusBar>

<style>
  .spacer { flex: 1; }
</style>