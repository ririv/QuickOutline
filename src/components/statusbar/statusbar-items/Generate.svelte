<script lang="ts">
  import Icon from '@/components/Icon.svelte';

  interface Props {
    onGenerate?: () => void | Promise<void>;
    loading?: boolean;
  }

  let { onGenerate, loading = false }: Props = $props();

  async function handleClick() {
      if (loading || !onGenerate) return;
      await Promise.resolve(onGenerate());
  }
</script>

<button 
    class="icon-btn generate-btn" 
    class:loading 
    onclick={handleClick} 
    title={loading ? "Generating..." : "Generate PDF"}
    disabled={loading}
>
    <span class="icon-slot">
    {#if loading}
        <div class="spinner">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22C17.5228 22 22 17.5228 22 12C22 9.27455 20.9097 6.80375 19.1414 5" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/></svg>
        </div>
    {:else}
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M8 5V19L19 12L8 5Z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
    {/if}
    </span>
</button>

<style>
  .icon-btn {
      background: transparent;
      border: none;
      cursor: pointer;
      padding: 0;
      block-size: 22px;
      inline-size: 22px;
      min-inline-size: 22px;
      max-inline-size: 22px;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      box-sizing: border-box;
      flex: 0 0 22px;
      transition: background-color 0.2s, color 0.2s, opacity 0.2s;
      align-self: center;
      margin: 0 8px 0 auto;
  }
  
  .icon-btn:hover:not(:disabled) {
      background-color: #e1e4e8;
      color: #1677ff;
  }

  .icon-btn:disabled {
      cursor: not-allowed;
      opacity: 0.6;
  }
  
  .generate-btn {
      color: #4096ff;
      margin-left: 0;
      margin-right: 8px;
  }
  .generate-btn:hover:not(:disabled) {
      background-color: rgba(64, 150, 255, 0.1);
      color: #1677ff;
  }

  .icon-slot {
      inline-size: 16px;
      block-size: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex: 0 0 16px;
      line-height: 0;
  }

  .icon-slot svg {
      display: block;
      flex: 0 0 auto;
  }

  .spinner {
      animation: spin 1s linear infinite;
      display: flex;
      align-items: center;
      justify-content: center;
      inline-size: 16px;
      block-size: 16px;
  }

  @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
  }
</style>
