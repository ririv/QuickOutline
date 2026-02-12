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
    {#if loading}
        <div class="spinner">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22C17.5228 22 22 17.5228 22 12C22 9.27455 20.9097 6.80375 19.1414 5" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/></svg>
        </div>
    {:else}
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M8 5V19L19 12L8 5Z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
    {/if}
</button>

<style>
  .icon-btn {
      background: transparent;
      border: none;
      cursor: pointer;
      padding: 0;
      height: 22px;
      width: 22px;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s;
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

  .spinner {
      animation: spin 1s linear infinite;
      display: flex;
      align-items: center;
      justify-content: center;
  }

  @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
  }
</style>
