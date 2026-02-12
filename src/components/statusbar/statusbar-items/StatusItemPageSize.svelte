<script lang="ts">
  import StatusBarItem from '../StatusBarItem.svelte';
  import PageSizePopup from '@/components/statusbar/statusbar-popup/PageSizePopup.svelte';
  import Icon from '@/components/Icon.svelte';
  import { type PageLayout, PAGE_SIZES_MM } from "@/lib/types/page.ts";
  import type { PageSizeDetectionState } from '@/lib/pdf-processing/usePdfPageSizeDetection.svelte.js';

  interface Props {
    pageSize: PageLayout['pageSize'];
    layoutDetection?: PageSizeDetectionState;
    mode?: 'new' | 'edit';
    onchange?: () => void;
  }

  let { 
    pageSize = $bindable(), 
    layoutDetection, 
    mode = 'edit', 
    onchange 
  }: Props = $props();

  let pageSizeAutoDetect = $state(true);

  let summary = $derived.by(() => {
      const format = (num: number) => {
          const rounded = Math.round(num * 10) / 10;
          return Number.isInteger(rounded) ? rounded.toString() : rounded.toFixed(1);
      };

      let w: number;
      let h: number;
      let matchedName: string = '';

      if (pageSize.type === 'preset') {
          matchedName = pageSize.size;
      }

      // In Auto mode, the actual detection might differ from the selected size
      if (pageSizeAutoDetect && layoutDetection?.actualDimensions) {
          w = layoutDetection.actualDimensions.width;
          h = layoutDetection.actualDimensions.height;
          
          // Match standard size name for the auto-detected dimensions
          const rw = Math.round(w * 10) / 10;
          const rh = Math.round(h * 10) / 10;

          const matched = Object.entries(PAGE_SIZES_MM).find(([_, [sw, sh]]) => 
              (rw === sw && rh === sh) || (rw === sh && rh === sw)
          );
          if (matched) matchedName = matched[0] as any;
          else matchedName = '';
      } else {
          if (pageSize.type === 'preset') {
            const std = PAGE_SIZES_MM[pageSize.size];
            if (pageSize.orientation === 'landscape') {
                w = std[1];
                h = std[0];
            } else {
                w = std[0];
                h = std[1];
            }
          } else {
            w = pageSize.width;
            h = pageSize.height;
          }
      }

      const orientation = w > h ? 'Landscape' : 'Portrait';
      const dimStr = `${format(w)}×${format(h)}mm`;
      
      return {
          display: matchedName || dimStr,
          full: matchedName ? `${dimStr} (${matchedName}, ${orientation})` : dimStr,
          orientation: orientation.toLowerCase() as 'portrait' | 'landscape'
      };
  });
</script>

<StatusBarItem 
    id="page-size" 
    title="Page Size: {summary.full}"
>
    {#snippet icon()}
        <!-- svelte-ignore css_unused_selector -->
        <div class="icon-rotator" class:rotated={summary.orientation === 'landscape'}>
            <Icon name="page-setup" width="16" height="16" />
        </div>
    {/snippet}
    {summary.display}
    {#snippet popup(triggerEl)}
        <PageSizePopup 
            bind:pageSize 
            bind:autoDetect={pageSizeAutoDetect}
            {onchange} 
            {triggerEl} 
            {mode}
            detection={layoutDetection}
        />
    {/snippet}
</StatusBarItem>

<style>
  .icon-rotator {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 100%;
      transition: transform 0.2s ease;
      will-change: transform;
  }

  .icon-rotator.rotated {
      transform: rotate(90deg);
  }
</style>
