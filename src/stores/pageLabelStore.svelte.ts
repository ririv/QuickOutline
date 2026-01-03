import { PageLabelNumberingStyle, pageLabelStyleMap } from "@/lib/styleMaps";
import type { PageLabel } from "@/lib/api/rust_pdf";
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
    
    // 使用 $state.raw 存储大型数组，性能最优
    simulatedLabels = $state.raw<string[]>([]);

    /**
     * 当新文件打开时，由 docStore 主动调用
     */
    init(originalLabels: string[]) {
        this.rules = [];
        this.resetForm();
        // 初始状态下，模拟标签就是原始标签
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
            const labels = await pageLabelService.simulateLabels(rustRules, totalPages);
            this.setSimulatedLabels(labels);
        }
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