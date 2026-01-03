import { messageStore } from '@/stores/messageStore.svelte.ts';
import { docStore } from '@/stores/docStore.svelte.js';
import { pageLabelStore } from '@/stores/pageLabelStore.svelte.js';
import { PageLabelNumberingStyle, pageLabelStyleMap } from '@/lib/styleMaps.ts';
import { setPageLabels, type PageLabel } from '@/lib/api/rust_pdf.ts';
import { pageLabelService } from '@/lib/services/PageLabelService';

export function usePageLabelActions() {
    function addRule() {
        if (!pageLabelStore.startPage) {
             messageStore.add("Please enter Start Page", "WARNING");
             return;
        }

        const newRule = {
            id: Date.now().toString(),
            numberingStyleDisplay: pageLabelStore.numberingStyle == PageLabelNumberingStyle.NONE ? "" : pageLabelStyleMap.getDisplayText(pageLabelStore.numberingStyle),
            prefix: pageLabelStore.prefix,
            start: parseInt(pageLabelStore.startNumber) || 1,
            fromPage: parseInt(pageLabelStore.startPage) || 1
        };

        pageLabelStore.addRule(newRule);
        pageLabelStore.resetForm();
        simulate();
    }

    function deleteRule(ruleId: string) {
        pageLabelStore.deleteRule(ruleId);
        simulate();
    }

    function clearRules() {
        pageLabelStore.removeAllRules();
        pageLabelStore.setSimulatedLabels(docStore.originalPageLabels);
        pageLabelStore.resetForm();
    }

    async function simulate() {
        const rules: PageLabel[] = pageLabelStore.rules.map(r => ({
            pageNum: r.fromPage,
            firstPage: r.start,
            labelPrefix: r.prefix,
            numberingStyle: pageLabelStyleMap.getEnumName(r.numberingStyleDisplay) || PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS
        }));

        try {
            const labels = await pageLabelService.simulateLabels(rules, docStore.pageCount);
            pageLabelStore.setSimulatedLabels(labels);
        } catch (e) {
            console.error("Simulation failed", e);
        }
    }

    function apply() {
        if (!docStore.currentFilePath) {
            messageStore.add("No file opened.", "ERROR");
            return;
        }

        const rules: PageLabel[] = pageLabelStore.rules.map(r => ({
            pageNum: r.fromPage,
            firstPage: r.start,
            labelPrefix: r.prefix,
            numberingStyle: pageLabelStyleMap.getEnumName(r.numberingStyleDisplay) || PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS
        }));
        
        setPageLabels(docStore.currentFilePath, rules, null).then(() => {
             messageStore.add("Page labels applied successfully!", "SUCCESS");
        }).catch(e => {
             messageStore.add("Failed to apply page labels: " + e.message, "ERROR");
        });
    }

    return {
        addRule,
        deleteRule,
        clearRules,
        simulate,
        apply
    };
}