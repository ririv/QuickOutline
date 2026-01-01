<script>
  import { onMount } from 'svelte';
  
  let htmlContent = $state('');
  let styleContent = $state('');

  onMount(() => {
    try {
        const payloadStr = localStorage.getItem('print-payload');
        if (payloadStr) {
            const data = JSON.parse(payloadStr);
            htmlContent = data.html || '';
            styleContent = data.styles || '';
        }
    } catch (e) {
        console.error("Failed to load print payload", e);
        htmlContent = "<h1>Error loading content</h1>";
    }
  });
</script>

<svelte:head>
    <!-- Inject dynamic styles passed from the editor -->
    {@html `<style>${styleContent}</style>`}
</svelte:head>

<!-- markdown-body class is crucial for styles to apply -->
<div class="markdown-body p-8">
    {@html htmlContent}
</div>

<style>
    /* Ensure page breaks work properly */
    @media print {
        :global(body) {
            margin: 0;
            padding: 0;
        }
    }
</style>
