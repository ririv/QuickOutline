// web/src/lib/styleMaps.ts
import { Numbering } from './pdf-processing/numbering';

export enum PageLabelNumberingStyle {
    /**
     * 1, 2, 3...
     */
    DECIMAL_ARABIC_NUMERALS = 'DECIMAL_ARABIC_NUMERALS',
    /**
     * I, II, III...
     */
    UPPERCASE_ROMAN_NUMERALS = 'UPPERCASE_ROMAN_NUMERALS',
    /**
     * i, ii, iii...
     */
    LOWERCASE_ROMAN_NUMERALS = 'LOWERCASE_ROMAN_NUMERALS',
    /**
     * A, B, C...
     */
    UPPERCASE_LETTERS = 'UPPERCASE_LETTERS',
    /**
     * a, b, c...
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

interface StyleMapEntry {
    displayText: string;
    enumName: PageLabelNumberingStyle;
}

const styles: StyleMapEntry[] = [
    { displayText: "None", enumName: PageLabelNumberingStyle.NONE },
    { displayText: "1, 2, 3, ...", enumName: PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS },
    { displayText: "i, ii, iii, ...", enumName: PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS },
    { displayText: "I, II, III, ...", enumName: PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS },
    { displayText: "a, b, c, ...", enumName: PageLabelNumberingStyle.LOWERCASE_LETTERS },
    { displayText: "A, B, C, ...", enumName: PageLabelNumberingStyle.UPPERCASE_LETTERS },
];

export const pageLabelStyleMap = {
    getEnumName: (displayText: string): PageLabelNumberingStyle | undefined => {
        const found = styles.find(s => s.displayText === displayText);
        return found ? found.enumName : undefined;
    },
    getDisplayText: (enumName: PageLabelNumberingStyle): string => {
        const found = styles.find(s => s.enumName === enumName);
        return found ? found.displayText : 'None';
    },
    getAllStyles: (): StyleMapEntry[] => styles
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