<script lang="ts">
  import StatusBarItem from '../StatusBarItem.svelte';
  import PageLabelPopup from '@/components/statusbar/statusbar-popup/PageLabelPopup.svelte';
  import Icon from '@/components/Icon.svelte';
  import { PageLabelNumberingStyle, generateRulePreview, type PageLabel } from "@/lib/types/page-label.ts";
  import labelSimpleIcon from '@/assets/icons/label-simple.svg?raw';

  interface Props {
    pageLabel: PageLabel;
    onchange?: () => void;
  }

  let { pageLabel = $bindable(), onchange }: Props = $props();

  const removeSuffix = (str:string, suffix:string) => str.endsWith(suffix) ? str.slice(0, -suffix.length) : str;
</script>

<StatusBarItem id="page-label" title="Set Numbering Style">
    {#snippet icon()}
        <Icon data={labelSimpleIcon} width="14" height="14" />
    {/snippet}

    {#if pageLabel.numberingStyle === PageLabelNumberingStyle.NONE && !pageLabel.labelPrefix}
        None
    {:else}
        {removeSuffix(generateRulePreview(pageLabel, 1), "...")}
    {/if}
    
    {#snippet popup(triggerEl)}
        <PageLabelPopup bind:pageLabel {onchange} {triggerEl} />
    {/snippet}
</StatusBarItem>
