// web/src/lib/styleMaps.ts

export enum PageLabelNumberingStyle {
    DECIMAL_ARABIC_NUMERALS = "DECIMAL_ARABIC_NUMERALS",
    UPPERCASE_ROMAN_NUMERALS = "UPPERCASE_ROMAN_NUMERALS",
    LOWERCASE_ROMAN_NUMERALS = "LOWERCASE_ROMAN_NUMERALS",
    UPPERCASE_LETTERS = "UPPERCASE_LETTERS",
    LOWERCASE_LETTERS = "LOWERCASE_LETTERS",
    NONE = "NONE"
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
