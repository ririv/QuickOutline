<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import MdEditor from '../../components/MdEditor.svelte';
  import Preview from '../../components/Preview.svelte';
  import { initBridge } from '../../lib/bridge';
  import '../../assets/global.css';
  import { onMount } from 'svelte';

  let editorComponent: MdEditor;
  let previewComponent: Preview;

  onMount(() => {
    // Initialize Bridge to route Java calls to components
    initBridge({
      // Preview actions
      onUpdateSvg: (json) => previewComponent?.renderSvg(json),
      onUpdateImage: (json) => previewComponent?.renderImage(json),
      onSetSvgDoubleBuffering: (enable) => previewComponent?.setDoubleBuffer(enable),

      // Editor actions
      onInitVditor: (md) => editorComponent?.init(md),
      onInsertContent: (text) => editorComponent?.insertValue(text),
      onGetContent: () => editorComponent?.getValue(),
      onSetContent: (md) => editorComponent?.setValue(md),
      onInsertImageMarkdown: (path) => editorComponent?.insertImageMarkdown(path),
      onGetContentHtml: () => editorComponent?.getContentHtml(),
      onGetPayloads: () => editorComponent?.getPayloads(),
    });
  });
</script>

<main>
  <SplitPane initialSplit={50}>
    <div slot="left" class="h-full">
      <MdEditor bind:this={editorComponent} />
    </div>
    <div slot="right" class="h-full">
      <Preview bind:this={previewComponent} mode="combined" />
    </div>
  </SplitPane>
</main>

<style>
  main {
    height: 100vh;
    width: 100vw;
    overflow: hidden;
  }
  .h-full {
      height: 100%;
  }
</style>
