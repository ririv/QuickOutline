<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import Preview from '../../components/Preview.svelte';
  import TocEditor from '../../components/TocEditor.svelte';
  import StatusBar from '../../components/StatusBar.svelte';
  import SectionEditor from '../../components/SectionEditor.svelte';
  import CollapseTrigger from '../../components/CollapseTrigger.svelte';
  import '../../assets/global.css';
  import { onMount } from 'svelte';
  import { slide } from 'svelte/transition';
  
  import { rpc } from '@/lib/api/rpc';
  import { messageStore } from '@/stores/messageStore';
  import { docStore } from '@/stores/docStore';
  import { tocStore } from '@/stores/tocStore.svelte';
  import {PageLabelNumberingStyle} from "@/lib/styleMaps";

  let previewComponent: Preview;
  
  let showHeader = $state(false);
  let showFooter = $state(false);
  
  let debounceTimer: number;

  // Auto-load TOC when file changes
  $effect(() => {
      const path = $docStore.currentFilePath;
      
      // Only load if path has changed (new file opened)
      // If path matches tocStore, we are just remounting (switching tabs), 
      // so we rely on onMount to restore state from store.
      if (tocStore.filePath !== path) {
          if (path) {
              loadOutline();
          } else {
              tocStore.setFile(null);
          }
      }
  });

  async function loadOutline() {
      try {
          const outline = await rpc.getOutline(0);
          // Initialize store with new file and default config
          tocStore.setFile($docStore.currentFilePath, outline || '');
          triggerPreview();
      } catch (e) {
          console.error("Failed to load outline", e);
      }
  }
  
  function handleContentChange(val: string) {
      tocStore.updateContent(val);
      triggerPreview();
  }
  
  function hasContent(config: typeof tocStore.headerConfig) {
      const hasText = Object.entries(config).some(([k, v]) => {
          if (k === 'drawLine') return false;
          return typeof v === 'string' && v.trim().length > 0 && v !== '{p}';
      });
      return hasText || config.drawLine;
  }
  
  // React to config changes
  $effect(() => {
    // Create dependencies on store properties to trigger updates
    const _ = { 
        h: JSON.stringify(tocStore.headerConfig), 
        f: JSON.stringify(tocStore.footerConfig),
        t: tocStore.title,
        o: tocStore.offset,
        i: tocStore.insertPos,
        s: tocStore.style
    };

    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
        triggerPreview();
    }, 500);

    return () => clearTimeout(debounceTimer);
  });
  
  // onMount: just trigger preview if we have content (e.g. switching back to tab)
  onMount(() => {
      if (tocStore.previewData) {
          // Restore cached preview immediately without RPC call
          previewComponent?.renderImage(tocStore.previewData);
          // Restore scroll position after render (timeout to ensure DOM updated)
          setTimeout(() => {
              previewComponent?.restoreScroll(tocStore.scrollTop);
          }, 0);
      } else if (tocStore.content) {
          triggerPreview();
      }
  });

  async function triggerPreview() {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(async () => {
      if (!tocStore.content) {
          return; 
      }
      
      const config = {
        tocContent: tocStore.content,
        title: tocStore.title,
        offset: tocStore.offset,
        insertPos: tocStore.insertPos,
        style: tocStore.style,
        header: tocStore.headerConfig,
        footer: tocStore.footerConfig
      };

      try {
        const resultJson = await rpc.generateTocPreview(config);
        // Only update if we got valid images back
        if (resultJson && resultJson !== "[]") {
            tocStore.previewData = resultJson; // Cache result
            previewComponent?.renderImage(resultJson);
        }
      } catch (e: any) {
        console.error("Preview failed", e);
      }
    }, 500);
  }

  async function handleGenerate() {
      if (!tocStore.content) {
          messageStore.add("Please enter TOC content first.", "WARNING");
          return;
      }

      const config = {
        tocContent: tocStore.content,
        title: tocStore.title,
        offset: tocStore.offset,
        insertPos: tocStore.insertPos,
        style: tocStore.style,
        header: tocStore.headerConfig,
        footer: tocStore.footerConfig
      };
      
      try {
          await rpc.generateTocPage(config, null); 
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
        {#snippet left()}
        <div class="left-panel">
          <!-- Header Trigger & Editor -->
          <CollapseTrigger 
            position="top" 
            label="Header" 
            expanded={showHeader} 
            hasContent={hasContent(tocStore.headerConfig)}
            ontoggle={() => showHeader = !showHeader} 
          />
          {#if showHeader}
            <div transition:slide={{ duration: 200 }}>
              <SectionEditor 
                type="header"
                bind:config={tocStore.headerConfig} 
              />
            </div>
          {/if}

          <div class="header">
            <input type="text" bind:value={tocStore.title} oninput={triggerPreview} placeholder="Title" class="title-input"/>
          </div>
          
          <div class="editor-wrapper">
            <TocEditor 
                bind:value={tocStore.content} 
                onchange={handleContentChange} 
                placeholder="Enter TOC here..."
            />
          </div>

          <!-- Footer Trigger & Editor -->
          {#if showFooter}
            <div transition:slide={{ duration: 200 }}>
              <SectionEditor 
                type="footer"
                bind:config={tocStore.footerConfig} 
              />
            </div>
          {/if}
          <CollapseTrigger 
            position="bottom" 
            label="Footer" 
            expanded={showFooter} 
            hasContent={hasContent(tocStore.footerConfig)}
            ontoggle={() => showFooter = !showFooter} 
          />
        </div>
        {/snippet}
        
        {#snippet right()}
        <div class="h-full">
          <Preview 
            bind:this={previewComponent} 
            mode="image" 
            onrefresh={triggerPreview} 
            onScroll={(top) => tocStore.scrollTop = top}
          />
        </div>
        {/snippet}
      </SplitPane>
  </div>

  <StatusBar 
      bind:offset={tocStore.offset} 
      bind:insertPos={tocStore.insertPos} 
      bind:style={tocStore.style} 
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
