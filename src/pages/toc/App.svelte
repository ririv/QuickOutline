<script lang="ts">
  import SplitPane from '../../components/SplitPane.svelte';
  import Preview from '../../components/Preview.svelte';
  import TocEditor from '../../components/editor/TocEditor.svelte';
  import StatusBar from '../../components/StatusBar.svelte';
  import PageFrame from '../../components/headerfooter/PageFrame.svelte';
  import '../../assets/global.css';
  import { onMount } from 'svelte';
  
  import { rpc } from '@/lib/api/rpc';
  import { generateTocPage, type TocConfig, type TocLinkDto } from '@/lib/api/rust_pdf';
  import { outlineService } from '@/lib/services/OutlineService';
  import { messageStore } from '@/stores/messageStore';
  import { docStore } from '@/stores/docStore';
  import { tocStore } from '@/stores/tocStore.svelte';
  import { printStore } from '@/stores/printStore.svelte'; // Import global print store
  import { appStore, FnTab } from '@/stores/appStore';
  import { generateTocHtml, DOT_GAP } from '@/lib/toc-gen/toc-generator.tsx';
  import { generateSectionHtml } from '@/lib/utils/html-generator';
  import { generatePageCss } from '@/lib/preview-engine/css-generator';
  import { TocPrintTemplate } from '@/lib/templates/TocPrintTemplate.tsx';
  import { getTocLinkData, getPageCount } from '@/lib/preview-engine/paged-engine';
  import { resolveLinkTarget } from '@/lib/services/PageLinkResolver';
  import { invoke } from '@tauri-apps/api/core';
  import { get } from 'svelte/store';

  let previewComponent: Preview;
  
  let showHeader = $state(false);
  let showFooter = $state(false);
  
  let debounceTimer: number;

  let activeTab = $state($appStore.activeTab); // Local state for activeTab
  // Subscribe to appStore updates
  $effect(() => {
    return appStore.subscribe(val => {
      activeTab = val.activeTab;
    });
  });

  // Refresh preview when tab becomes active to restore CSS
  $effect(() => {
      if (activeTab === FnTab.tocGenerator) {
          setTimeout(() => triggerPreview(), 0);
      }
  });

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
          const path = $docStore.currentFilePath;
          if (!path) return;
          
          await outlineService.loadOutline(path);
          triggerPreview();
      } catch (e) {
          console.error("Failed to load outline", e);
      }
  }
  
  function handleContentChange(val: string) {
      tocStore.updateContent(val);
      triggerPreview();
  }
  
  // React to config changes
  $effect(() => {
    // Create dependencies on store properties to trigger updates
    const _ = { 
        h: JSON.stringify(tocStore.headerConfig), 
        f: JSON.stringify(tocStore.footerConfig),
        pl: JSON.stringify(tocStore.pageLayout),
        hfl: JSON.stringify(tocStore.hfLayout),
        t: tocStore.title,
        o: tocStore.offset,
        i: JSON.stringify(tocStore.insertionConfig), // Watch insertion object
        s: tocStore.numberingStyle
    };

    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
        triggerPreview();
    }, 500);

    return () => clearTimeout(debounceTimer);
  });
  
  // onMount: just trigger preview if we have content (e.g. switching back to tab)
  onMount(() => {
      // Check if store matches current file
      if (tocStore.filePath !== $docStore.currentFilePath) {
          // Store is stale, do not render previewData
          return;
      }

      if (tocStore.previewData) {
          // Restore cached preview immediately without RPC call
          previewComponent?.renderSvg(tocStore.previewData);
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
      
      try {
        let pageOffset = 0;
        let threshold = 1;

        if (tocStore.insertionConfig.autoCorrect) {
            const tocPageCount = getPageCount();
            if (tocPageCount > 0) {
                pageOffset = tocPageCount;
                threshold = Math.max(1, (parseInt(String(tocStore.insertionConfig.pos), 10) || 0) + 1);
            }
        }

        // Generate HTML locally instead of calling RPC
        const { html, styles } = generateTocHtml(
            tocStore.content, 
            tocStore.title, 
            tocStore.offset, 
            tocStore.numberingStyle,
            undefined, // Use default indentStep
            tocStore.pageLayout,
            pageOffset,
            threshold
        );
        
        // Update payload in store, which is passed to Preview component
        tocStore.previewData = {
            html,
            styles,
            header: tocStore.headerConfig,
            footer: tocStore.footerConfig,
            pageLayout: tocStore.pageLayout,
            hfLayout: tocStore.hfLayout
        };
        
      } catch (e: any) {
        console.error("Preview generation failed", e);
      }
    }, 500);
  }

  async function handleGenerate() {
      if (!tocStore.content) {
          messageStore.add("Please enter TOC content first.", "WARNING");
          return;
      }

      try {
          // 1. Calculate Links and Resolve Targets
          const rawLinks = getTocLinkData();
          const links: TocLinkDto[] = [];
          
          // Get current labels from docStore
          const labels = $docStore.originalPageLabels;

          const insertPosVal = parseInt(String(tocStore.insertionConfig.pos), 10) || 0;

          const resolverConfig = {
              labels: labels && labels.length > 0 ? labels : null,
              offset: tocStore.offset,
              insertPos: insertPosVal
          };

          for (const raw of rawLinks) {
              const target = resolveLinkTarget(raw.targetPageLabel, resolverConfig);
              if (target !== null) {
                  links.push({
                      tocPageIndex: raw.tocPageIndex,
                      x: raw.x,
                      y: raw.y,
                      width: raw.width,
                      height: raw.height,
                      targetPageIndex: target.index,
                      targetIsOriginal: target.isOriginal
                  });
              }
          }

          // Prepare offset logic for visual page numbers (if auto-correct enabled)
          let pageOffset = 0;
          let threshold = 1;
          if (tocStore.insertionConfig.autoCorrect) {
              const tocPageCount = getPageCount();
              if (tocPageCount > 0) {
                  pageOffset = tocPageCount;
                  threshold = Math.max(1, insertPosVal + 1);
              }
          }

          // 2. Generate HTML with correction
          const { html, styles } = generateTocHtml(
            tocStore.content,
            tocStore.title,
            tocStore.offset,
            tocStore.numberingStyle,
            undefined, // Use default indentStep
            tocStore.pageLayout,
            pageOffset,
            threshold
          );
          
          const headerHtml = generateSectionHtml(tocStore.headerConfig);
          const footerHtml = generateSectionHtml(tocStore.footerConfig);
          const pageCss = generatePageCss(tocStore.headerConfig, tocStore.footerConfig, tocStore.pageLayout, tocStore.hfLayout);

          const fullHtml = TocPrintTemplate({
            styles,
            pageCss,
            headerHtml,
            footerHtml,
            tocHtml: html,
            dotGap: DOT_GAP // Pass the dynamic dotGap
          });

          // 3. Generate PDF via Rust
          messageStore.add("Generating PDF...", "INFO");
          const filename = `toc_${Date.now()}.pdf`;
          
          // Use global print mode
          let modeParam = printStore.mode.toLowerCase();
          if (printStore.mode === 'HeadlessChrome') {
              modeParam = 'headless_chrome';
          }

          const pdfPath = await invoke('print_to_pdf', { 
              html: fullHtml, 
              filename: filename,
              mode: modeParam
              // browserPath 和 forceDownload 参数可根据需要添加 UI 控件来设置
          });
          
          console.log("PDF Generated at:", pdfPath); // Added console.log

          // 4. Send to Backend for stitching
          const config: TocConfig = {
            tocContent: tocStore.content,
            tocPdfPath: pdfPath as string, // Path to the generated PDF
            title: tocStore.title,
            insertPos: parseInt(String(tocStore.insertionConfig.pos), 10),
            numberingStyle: tocStore.numberingStyle,
            header: tocStore.headerConfig,
            footer: tocStore.footerConfig,
            links: links
          };

          const currentFile = $docStore.currentFilePath;
          if (!currentFile) throw new Error("No file opened");
          await generateTocPage(currentFile, config, null);
          console.info("PDF generated");
          messageStore.add("Table of Contents generated successfully!", "SUCCESS");

      } catch (e: any) {
          console.error("Generate failed", e);
          messageStore.add("Failed: " + e.message || e, "ERROR");
      }
  }

</script>

<main>
  <div class="content-area">
      <SplitPane initialSplit={40}>
        {#snippet left()}
        <div class="left-panel">
          <PageFrame
            bind:headerConfig={tocStore.headerConfig}
            bind:footerConfig={tocStore.footerConfig}
            bind:showHeader={showHeader}
            bind:showFooter={showFooter}
            onHeaderChange={triggerPreview}
            onFooterChange={triggerPreview}
          >
            <div class="header">
              <input type="text" bind:value={tocStore.title} oninput={triggerPreview} placeholder="Title" class="title-input"/>
            </div>
            
            <div class="editor-wrapper">
              <TocEditor 
                  bind:value={tocStore.content} 
                  onchange={handleContentChange} 
                  placeholder="Enter TOC here..."
                  offset={tocStore.offset}
                  totalPage={$docStore.pageCount}
                  pageLabels={$docStore.originalPageLabels}
                  insertPos={parseInt(String(tocStore.insertionConfig.pos), 10) || 0}
              />
            </div>
          </PageFrame>
        </div>
        {/snippet}
        
        {#snippet right()}
        <div class="h-full">
          <Preview 
            bind:this={previewComponent} 
            mode="paged"
            pagedPayload={tocStore.previewData}
            isActive={activeTab === FnTab.tocGenerator}
            onrefresh={triggerPreview} 
            onScroll={(top) => tocStore.scrollTop = top}
          />
        </div>
        {/snippet}
      </SplitPane>
  </div>

  <StatusBar 
      bind:offset={tocStore.offset} 
      bind:insertion={tocStore.insertionConfig}
      bind:numberingStyle={tocStore.numberingStyle}
      bind:pageLayout={tocStore.pageLayout}
      bind:hfLayout={tocStore.hfLayout}
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