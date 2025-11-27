<script lang="ts">
  import type { Snippet } from 'svelte';

  type ButtonType = 'primary' | 'important';

  interface Props {
    type: ButtonType;
    class?: string;
    children?: Snippet;
    [key: string]: any;
  }

  let {
    type,
    class: className,
    children,
    ...rest
  }: Props = $props();
</script>

<button
  class="my-button plain-button-{type} {className || ''}"
  {...rest}
>
  {@render children?.()}
</button>
