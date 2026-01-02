<script lang="ts">
    import ThumbnailPane from '@/components/ThumbnailPane.svelte';
    import { docStore } from '@/stores/docStore.svelte';
</script>

<div class="h-full w-full">
    <ThumbnailPane pageCount={docStore.pageCount} />
</div>
