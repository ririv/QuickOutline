<script lang="ts">
    import { generateRulePreview, type PageLabel } from '@/lib/types/page-label.ts';

    interface Props {
        rule: PageLabel;
        labelWidth?: string;
    }

    let { rule, labelWidth = '100px' }: Props = $props();

    let previewText = $derived(generateRulePreview(rule));
</script>

<div class="grid items-center gap-2.5" style="grid-template-columns: {labelWidth} 1fr;">
    <div class="text-right text-[11px] text-[#909399] uppercase tracking-wider font-semibold">Preview</div>
    <div class="px-2.5 py-1.5 bg-slate-50/50 border border-dashed border-slate-200 rounded text-[12px] text-slate-500 font-mono">
        {previewText}
    </div>
</div>
