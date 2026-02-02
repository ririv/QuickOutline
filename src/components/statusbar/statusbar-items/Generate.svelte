<script lang="ts">
  import Icon from '@/components/Icon.svelte';

  interface Props {
    onGenerate?: () => void;
  }

  let { onGenerate }: Props = $props();
</script>

<button class="icon-btn generate-btn" onclick={onGenerate} title="Generate PDF">
    <Icon name="play" width="20" height="20" />
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
  
  .icon-btn:hover {
      background-color: #e1e4e8;
      color: #1677ff;
  }
  
  .generate-btn {
      color: #4096ff;
      margin-left: 0;
      margin-right: 8px;
  }
  .generate-btn:hover {
      background-color: rgba(64, 150, 255, 0.1);
      color: #1677ff;
  }
</style>
