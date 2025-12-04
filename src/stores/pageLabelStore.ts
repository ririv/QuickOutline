import { writable } from 'svelte/store';
import {PageLabelNumberingStyle, pageLabelStyleMap} from "@/lib/styleMaps";

export interface PageLabelRule {
    id: string;
    styleDisplay: string;
    prefix: string;
    start: number;
    fromPage: number;
}

export interface PageLabelState {
    rules: PageLabelRule[];
    numberingStyle: PageLabelNumberingStyle;
    prefix: string;
    startNumber: string;
    startPage: string;
    simulatedLabels: string[];
}

const initialState: PageLabelState = {
    rules: [],
    numberingStyle: PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS,
    prefix: "",
    startNumber: "",
    startPage: "",
    simulatedLabels: []
};

function createPageLabelStore() {
    const { subscribe, set, update } = writable<PageLabelState>(initialState);

    return {
        subscribe,
        set,
        update,
        addRule: (rule: PageLabelRule) => update(state => ({
            ...state,
            rules: [...state.rules, rule]
        })),
        deleteRule: (ruleId: string) => update(state => ({
            ...state,
            rules: state.rules.filter(r => r.id !== ruleId)
        })),
        updateForm: (updates: Partial<PageLabelState>) => update(state => ({
            ...state,
            ...updates
        })),
        resetForm: () => update(state => ({
            ...state,
            startPage: "",
            prefix: "",
            startNumber: ""
        })),
        setSimulatedLabels: (labels: string[]) => update(state => ({ ...state, simulatedLabels: labels })),
        resetAll: () => set(initialState)
    };
}

export const pageLabelStore = createPageLabelStore();
