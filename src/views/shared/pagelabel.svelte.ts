import { messageStore } from '@/stores/messageStore.svelte.ts';
import { docStore } from '@/stores/docStore.svelte.js';
import { pageLabelStore } from '@/stores/pageLabelStore.svelte.js';
import { confirm } from '@/stores/confirm.svelte';
import { PageLabelNumberingStyle, pageLabelStyleMap, type PageLabel } from '@/lib/types/page-label.ts';
import { pageLabelService } from '@/lib/services/PageLabelService';
import { formatError } from '@/lib/utils/error';

export function usePageLabelActions() {
    function addRule() {
        if (!pageLabelStore.startPage) {
             messageStore.add("Please enter Start Page", "WARNING");
             return;
        }

        const newRule: PageLabel = {
            pageNum: parseInt(pageLabelStore.startPage) || 1,
            numberingStyle: pageLabelStore.numberingStyle,
            labelPrefix: pageLabelStore.prefix || null,
            firstPage: parseInt(pageLabelStore.startNumber) || 1
        };

        pageLabelStore.addOrUpdateRule(newRule);
        pageLabelStore.resetForm();
        simulate();
    }

    function deleteRule(fromPage: number) {
        pageLabelStore.deleteRule(fromPage);
        simulate();
    }

    async function resetToOriginal() {
        const ok = await confirm(
            "Are you sure you want to reset all rules to the original settings from the PDF?",
            "Reset Rules",
            { type: 'warning', confirmText: 'Reset', cancelText: 'Cancel' }
        );
        if (!ok) return;

        if (docStore.originalRules && docStore.originalRules.length > 0) {
            await pageLabelStore.setRules(docStore.originalRules, docStore.pageCount);
        } else {
            pageLabelStore.removeAllRules();
            pageLabelStore.setSimulatedLabels(docStore.originalPageLabels);
        }
        pageLabelStore.resetForm();
    }

    async function clearRules() {
        const ok = await confirm(
            "Are you sure you want to delete ALL rules? This will reset the page numbering to simple sequential numbers (1, 2, 3...).",
            "Delete All Rules",
            { type: 'error', confirmText: 'Delete All', cancelText: 'Cancel' }
        );
        if (!ok) return;

        pageLabelStore.removeAllRules();
        pageLabelStore.resetForm();
        simulate();
    }

    async function simulate() {
        const rules = pageLabelStore.getFinalRules();
        try {
            const labels = await pageLabelService.simulateLabels(rules, docStore.pageCount);
            pageLabelStore.setSimulatedLabels(labels);
        } catch (e) {
            console.error("Simulation failed", e);
        }
    }

    async function apply() {
        if (!docStore.currentFilePath) {
            messageStore.add("No file opened.", "ERROR");
            return;
        }

        try {
            const rules = pageLabelStore.getFinalRules();
            const destPath = await pageLabelService.saveRulesAsNewFile(docStore.currentFilePath, rules);
            messageStore.add("Page labels applied and saved to: " + destPath, "SUCCESS");
        } catch (e: any) {
            messageStore.add("Failed to apply page labels: " + formatError(e), "ERROR");
        }
    }

    return {
        addRule,
        deleteRule,
        resetToOriginal,
        clearRules,
        simulate,
        apply
    };
}