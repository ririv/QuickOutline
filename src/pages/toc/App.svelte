<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import Preview from '../../components/Preview.svelte';
  import SimpleEditor from '../../components/SimpleEditor.svelte';
  import StatusBar from '../../components/StatusBar.svelte';
  import SectionEditor from '../../components/SectionEditor.svelte';
  import CollapseTrigger from '../../components/CollapseTrigger.svelte';
  import '../../assets/global.css';
  import { onMount } from 'svelte';
  import { slide } from 'svelte/transition';
  
  import { rpc, PageLabelNumberingStyle } from '@/lib/api/rpc';
  import { messageStore } from '@/stores/messageStore';
  import { docStore } from '@/stores/docStore';

  let previewComponent: Preview;
  
  // State
  let tocContent = $state('');
  let title = $state('Table of Contents');
  let offset = $state(0);
  let insertPos = $state(1);
  let style = $state(PageLabelNumberingStyle.NONE); // 【关键修正】现在直接绑定枚举名
  
  let headerConfig = $state({ left: '', center: '', right: '', inner: '', outer: '', drawLine: false });
  let footerConfig = $state({ left: '', center: '{p}', right: '', inner: '', outer: '', drawLine: false });
  
  let showHeader = $state(false);
  let showFooter = $state(false);
  
  let debounceTimer: number;

  // Auto-load TOC when file changes
  $effect(() => {
      const path = $docStore.currentFilePath;
      if (path) {
          loadOutline();
      } else {
          tocContent = '';
      }
  });

  async function loadOutline() {
      try {
          // Default offset 0. Ideally offset should be managed per file.
          const outline = await rpc.getOutline(0);
          if (outline) {
              tocContent = outline;
              // Trigger initial preview
              triggerPreview();
          }
      } catch (e) {
          console.error("Failed to load outline", e);
      }
  }
  
  function hasContent(config: typeof headerConfig) {
      const hasText = Object.entries(config).some(([k, v]) => {
          if (k === 'drawLine') return false;
          return typeof v === 'string' && v.trim().length > 0 && v !== '{p}';
      });
      return hasText || config.drawLine;
  }
  
  // Use $effect to react to changes in headerConfig or footerConfig
  $effect(() => {
    const headerJson = JSON.stringify(headerConfig);
    const footerJson = JSON.stringify(footerConfig);

    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
        if (headerJson !== '{"left":"","center":"","right":"","inner":"","outer":"","drawLine":false}' ||
            footerJson !== '{"left":"","center":"{p}","right":"","inner":"","outer":"","drawLine":false}') {
            triggerPreview();
        }
    }, 500);
  });
  
  // onMount not strictly needed for bridge anymore, RPC is initialized by RpcProvider
  // but we can trigger an initial preview if there's content
  onMount(() => {
      if (tocContent) {
          triggerPreview();
      }
  });

  async function triggerPreview() {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(async () => {
      if (!tocContent) {
          return; // Or clear preview?
      }
      
      const config = {
        tocContent,
        title,
        offset,
        insertPos, // insertPos is part of config, even if unused for preview
        style: style, // 直接使用绑定的枚举名
        header: headerConfig,
        footer: footerConfig
      };

      try {
        // rpc.generateTocPreview returns JSON string of ImagePageUpdate[]
        const resultJson = await rpc.generateTocPreview(config);
        previewComponent?.renderImage(resultJson);
      } catch (e: any) {
        console.error("Preview failed", e);
        // messageStore.add("Preview failed: " + e.message, "ERROR"); // Maybe too noisy for debounce
      }
    }, 500);
  }

  async function handleGenerate() {
      if (!tocContent) {
          messageStore.add("Please enter TOC content first.", "WARNING");
          return;
      }

      const config = {
        tocContent,
        title,
        offset,
        insertPos,
        style: style, // 直接使用绑定的枚举名
        header: headerConfig,
        footer: footerConfig
      };
      
      try {
          await rpc.generateTocPage(config, null); // destFilePath null means overwrite/auto
          messageStore.add("Table of Contents generated successfully!", "SUCCESS");
      } catch (e: any) {
          console.error("Generate failed", e);
          messageStore.add("Failed to generate TOC: " + e.message, "ERROR");
      }
  }

</script>

<main>
  <div class="content-area">
      <SplitPane initialSplit={40}>
        <div slot="left" class="left-panel">
          <!-- Header Trigger & Editor -->
          <CollapseTrigger 
            position="top" 
            label="Header" 
            expanded={showHeader} 
            hasContent={hasContent(headerConfig)}
            ontoggle={() => showHeader = !showHeader} 
          />
          {#if showHeader}
            <div transition:slide={{ duration: 200 }}>
              <SectionEditor 
                type="header"
                bind:config={headerConfig} 
              />
            </div>
          {/if}

          <div class="header">
            <input type="text" bind:value={title} oninput={triggerPreview} placeholder="Title" class="title-input"/>
          </div>
          
          <div class="editor-wrapper">
            <SimpleEditor bind:value={tocContent} onchange={triggerPreview} placeholder="Enter TOC here..." />
          </div>

          <!-- Footer Trigger & Editor -->
          {#if showFooter}
            <div transition:slide={{ duration: 200 }}>
              <SectionEditor 
                type="footer"
                bind:config={footerConfig} 
              />
            </div>
          {/if}
          <CollapseTrigger 
            position="bottom" 
            label="Footer" 
            expanded={showFooter} 
            hasContent={hasContent(footerConfig)}
            ontoggle={() => showFooter = !showFooter} 
          />
        </div>
        
        <div slot="right" class="h-full">
          <!-- mode="image" uses the JSON image updates -->
          <Preview bind:this={previewComponent} mode="image" onrefresh={triggerPreview} />
        </div>
      </SplitPane>
  </div>

  <StatusBar 
      bind:offset 
      bind:insertPos 
      bind:style 
      onGenerate={handleGenerate} 
      onParamChange={triggerPreview} 
  />
</main>

<style>
  main {
    height: 100%;
    width: 100%;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  }
  
  .content-area {
      flex: 1;
      overflow: hidden;
      position: relative;
  }
  
  .h-full { height: 100%; }
  
  .left-panel {
    height: 100%;
    display: flex;
    flex-direction: column;
    border-right: 1px solid #ddd;
    background: #fff;
  }

  .header {
    padding: 10px;
    position: relative;
  }
  
  .header::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 32px;
    width: calc(100% - 64px);
    height: 1px;
    background: #eee;
  }
/* ... middle content ... */
  .title-input {
    width: 100%;
    padding: 16px 22px;
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 20px;
    font-weight: bold;
    border: none;
    background: transparent;
    box-sizing: border-box;
    text-align: center;
    color: #333;
    transition: background-color 0.2s;
  }
  
  .title-input:focus {
    outline: none;
  }

  .title-input:hover {
    background-color: rgba(0, 0, 0, 0.02);
  }

  .editor-wrapper {
    flex: 1;
    overflow: hidden;
    padding: 10px;
  }
</style>
