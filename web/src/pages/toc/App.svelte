<script lang="ts">
  import Preview from '../../components/Preview.svelte';
  import { initBridge } from '../../lib/bridge';
  import '../../assets/global.css';
  import { onMount } from 'svelte';

  let previewComponent: Preview;

  onMount(() => {
    // Initialize Bridge to route Java calls to components
    initBridge({
      // Preview actions
      onUpdateSvg: (json) => previewComponent?.renderSvg(json),
      onUpdateImage: (json) => previewComponent?.renderImage(json),
      onSetSvgDoubleBuffering: (enable) => previewComponent?.setDoubleBuffer(enable),
    });
  });
</script>

<main>
    <Preview bind:this={previewComponent} mode="preview-only" />
</main>

<style>
  main {
    height: 100vh;
    width: 100vw;
    overflow: hidden;
  }
</style>
