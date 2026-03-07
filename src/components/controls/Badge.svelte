<script lang="ts">
  import { type Snippet } from 'svelte';

  type Variant = 'default' | 'success' | 'warning' | 'danger' | 'info';

  interface Props {
    variant?: Variant;
    children?: Snippet;
  }

  let { variant = 'default', children }: Props = $props();

  const variants = {
    default: "bg-gray-100 text-gray-700",
    success: "bg-green-100 text-green-700",
    warning: "bg-amber-100 text-amber-700",
    danger: "bg-red-100 text-red-700",
    info: "bg-blue-100 text-blue-700"
  };
</script>

<span class="inline-flex items-center justify-center px-2 py-0.5 rounded text-xs font-medium {variants[variant]}">
  {@render children?.()}
</span>
