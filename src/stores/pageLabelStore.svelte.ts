import { PageLabelNumberingStyle, pageLabelStyleMap } from "@/lib/styleMaps";
import type { PageLabel } from "@/lib/pdf-processing/page-label";
import { pageLabelService } from "@/lib/services/PageLabelService";

export interface PageLabelRule {
    id: string;
    numberingStyleDisplay: string;
    prefix: string;
    start: number;
    fromPage: number;
}

class PageLabelStore {
    rules = $state<PageLabelRule[]>([]);
    numberingStyle = $state(PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS);
    prefix = $state("");
    startNumber = $state("");
    startPage = $state("");
    
    simulatedLabels = $state.raw<string[]>([]);

    init(originalLabels: string[]) {
        this.rules = [];
        this.resetForm();
        this.simulatedLabels = originalLabels;
    }

    async setRules(rustRules: PageLabel[], totalPages: number) {
        this.rules = rustRules.map(r => ({
            id: Math.random().toString(36).substring(2, 9),
            fromPage: r.pageNum,
            numberingStyleDisplay: pageLabelStyleMap.getDisplayText(r.numberingStyle),
            prefix: r.labelPrefix || '',
            start: r.firstPage || 1
        }));

        if (this.rules.length > 0) {
            const labels = await pageLabelService.simulateLabels(this.getFinalRules(), totalPages);
            this.setSimulatedLabels(labels);
        }
    }

    /**
     * Converts UI rules back to standard PageLabel format for business logic.
     */
    getFinalRules(): PageLabel[] {
        return this.rules.map(r => ({
            pageNum: r.fromPage,
            numberingStyle: pageLabelStyleMap.getEnumName(r.numberingStyleDisplay)!,
            labelPrefix: r.prefix || null,
            firstPage: r.start
        }));
    }

    addRule(rule: PageLabelRule) {
        this.rules.push(rule);
    }

    deleteRule(ruleId: string) {
        this.rules = this.rules.filter(r => r.id !== ruleId);
    }

    removeAllRules() {
        this.rules = [];
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
