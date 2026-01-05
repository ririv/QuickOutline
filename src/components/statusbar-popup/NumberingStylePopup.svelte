<script lang="ts">
  import StyleList from '../StyleList.svelte';
  import ArrowPopup from '../controls/ArrowPopup.svelte';
  import {PageLabelNumberingStyle} from "@/lib/types/page-label.ts";

  interface Props {
    numberingStyle?: PageLabelNumberingStyle;
    onchange?: () => void;
    triggerEl: HTMLElement | undefined;
  }

  let { 
    numberingStyle = $bindable(PageLabelNumberingStyle.NONE),
    onchange,
    triggerEl
  }: Props = $props();

  function handleStyleSelect(s: PageLabelNumberingStyle) {
      numberingStyle = s;
      onchange?.();
  }
</script>

<ArrowPopup 
  placement="top" 
  minWidth="auto" 
  padding="4px"
  {triggerEl}
>
  <StyleList selected={numberingStyle} onselect={handleStyleSelect} />
</ArrowPopup>
