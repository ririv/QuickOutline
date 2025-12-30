import { Numbering } from './numbering';

export enum PageLabelNumberingStyle {
    /**
     * 1, 2, 3, 4...
     */
    DECIMAL_ARABIC_NUMERALS = 'DECIMAL_ARABIC_NUMERALS',
    /**
     * I, II, III, IV...
     */
    UPPERCASE_ROMAN_NUMERALS = 'UPPERCASE_ROMAN_NUMERALS',
    /**
     * i, ii, iii, iv...
     */
    LOWERCASE_ROMAN_NUMERALS = 'LOWERCASE_ROMAN_NUMERALS',
    /**
     * A, B, C, D...
     */
    UPPERCASE_LETTERS = 'UPPERCASE_LETTERS',
    /**
     * a, b, c, d...
     */
    LOWERCASE_LETTERS = 'LOWERCASE_LETTERS',

    NONE = 'NONE'
}

export interface PageLabel {
    pageNum: number; // 1-based page index where the label rule starts
    numberingStyle: PageLabelNumberingStyle;
    labelPrefix?: string | null;
    firstPage?: number; // The number to start counting from (e.g. start from 1)
}

export const STYLE_MAP: Record<string, PageLabelNumberingStyle> = {
    "None": PageLabelNumberingStyle.NONE,
    "1, 2, 3, ...": PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS,
    "i, ii, iii, ...": PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS,
    "I, II, III, ...": PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS,
    "a, b, c, ...": PageLabelNumberingStyle.LOWERCASE_LETTERS,
    "A, B, C, ...": PageLabelNumberingStyle.UPPERCASE_LETTERS,
};

export function simulatePageLabelsLocal(rules: PageLabel[], pageCount: number): string[] {
    const sortedRules = [...rules].sort((a, b) => a.pageNum - b.pageNum);
    const labels: string[] = new Array(pageCount);

    for (let i = 1; i <= pageCount; i++) {
        let activeRule: PageLabel | null = null;
        for (const rule of sortedRules) {
            if (i >= rule.pageNum) {
                activeRule = rule;
            } else {
                break;
            }
        }

        if (activeRule) {
            const offset = i - activeRule.pageNum;
            const start = activeRule.firstPage ?? 1;
            labels[i - 1] = Numbering.formatPageNumber(activeRule.numberingStyle, start + offset, activeRule.labelPrefix || null);
        } else {
            labels[i - 1] = String(i);
        }
    }
    return labels;
}
