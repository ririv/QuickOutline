import { writable } from 'svelte/store';

export interface PageLabelRule {
    id: string;
    style: string;
    styleDisplay: string;
    prefix: string;
    start: number;
    fromPage: number;
}

export interface PageLabelState {
    rules: PageLabelRule[];
    numberingStyle: string;
    prefix: string;
    startNumber: string;
    startPage: string;
}

const initialState: PageLabelState = {
    rules: [],
    numberingStyle: "1, 2, 3, ...",
    prefix: "",
    startNumber: "",
    startPage: ""
};

function createPageLabelStore() {
    const { subscribe, set, update } = writable<PageLabelState>(initialState);

    return {
        subscribe,
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
        resetAll: () => set(initialState)
    };
}

export const pageLabelStore = createPageLabelStore();
