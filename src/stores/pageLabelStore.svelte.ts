import { PageLabelNumberingStyle, pageLabelStyleMap, type PageLabel } from "@/lib/types/page-label.ts";
import { pageLabelService } from "@/lib/services/PageLabelService";
import { SvelteMap } from "svelte/reactivity";

class PageLabelStore {
    // Key: pageNum (fromPage), Value: PageLabel
    rulesMap = new SvelteMap<number, PageLabel>();
    
    // Derived sorted list for UI rendering
    sortedRules = $derived(
        Array.from(this.rulesMap.values()).sort((a, b) => a.pageNum - b.pageNum)
    );

    // Alias for backward compatibility (read-only)
    get rules() { return this.sortedRules; }

    numberingStyle = $state(PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS);
    prefix = $state("");
    startNumber = $state("");
    startPage = $state("");
    isFormOpen = $state(false);
    
    simulatedLabels = $state.raw<string[]>([]);

    init(originalLabels: string[]) {
        this.rulesMap.clear();
        this.resetForm();
        this.simulatedLabels = originalLabels;
    }

    async setRules(rustRules: PageLabel[], totalPages: number) {
        this.rulesMap.clear();
        for (const r of rustRules) {
            this.rulesMap.set(r.pageNum, r);
        }

        if (this.rulesMap.size > 0) {
            const labels = await pageLabelService.simulateLabels(this.getFinalRules(), totalPages);
            this.setSimulatedLabels(labels);
        }
    }

    /**
     * Returns standard PageLabel format for business logic.
     */
    getFinalRules(): PageLabel[] {
        return this.sortedRules;
    }

    addOrUpdateRule(rule: PageLabel) {
        // Map automatically handles deduplication/overwrite by key (pageNum)
        this.rulesMap.set(rule.pageNum, rule);
    }

    getRuleByPage(page: number): PageLabel | undefined {
        return this.rulesMap.get(page);
    }

    deleteRule(pageNum: number) {
        this.rulesMap.delete(pageNum);
    }

    removeAllRules() {
        this.rulesMap.clear();
    }

    resetForm() {
        this.startPage = "";
        this.prefix = "";
        this.startNumber = "";
    }

    setSimulatedLabels(labels: string[]) {
        this.simulatedLabels = labels;
    }
}

export const pageLabelStore = new PageLabelStore();
