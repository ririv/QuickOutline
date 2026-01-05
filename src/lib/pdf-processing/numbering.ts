import { PageLabelNumberingStyle } from '../types/page-label.ts';

export class Numbering {
    
    static toRoman(number: number, upperCase: boolean): string {
        return upperCase ? this.toRomanUpperCase(number) : this.toRomanLowerCase(number);
    }

    static toRomanLowerCase(number: number): string {
        return this.toRomanUpperCase(number).toLowerCase();
    }

    static toRomanUpperCase(number: number): string {
        if (number < 1 || number > 3999) return "";
        const thousands = ["", "M", "MM", "MMM"];
        const hundreds = ["", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"];
        const tens = ["", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"];
        const ones = ["", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"];

        const idxThousands = Math.floor(number / 1000);
        const idxHundreds = Math.floor((number % 1000) / 100);
        const idxTens = Math.floor((number % 100) / 10);
        const idxOnes = number % 10;

        return thousands[idxThousands] + hundreds[idxHundreds] + tens[idxTens] + ones[idxOnes];
    }

    static toLatinAlphabetNumber(number: number, upperCase: boolean): string {
        return upperCase ? this.toLatinAlphabetNumberUpperCase(number) : this.toLatinAlphabetNumberLowerCase(number);
    }

    static toLatinAlphabetNumberLowerCase(number: number): string {
        return this.toLatinAlphabetNumberUpperCase(number).toLowerCase();
    }

    static toLatinAlphabetNumberUpperCase(number: number): string {
        if (number <= 0) return "";
        let sb = "";
        let n = number;
        while (n > 0) {
            n--;
            const charCode = 'A'.charCodeAt(0) + (n % 26);
            sb = String.fromCharCode(charCode) + sb;
            n = Math.floor(n / 26);
        }
        return sb;
    }

    static formatPageNumber(style: PageLabelNumberingStyle, number: number, prefix: string | null): string {
        let label = "";
        switch (style) {
            case PageLabelNumberingStyle.NONE:
                label = "";
                break;
            case PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS:
                label = String(number);
                break;
            case PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS:
                label = this.toRomanUpperCase(number);
                break;
            case PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS:
                label = this.toRomanLowerCase(number);
                break;
            case PageLabelNumberingStyle.UPPERCASE_LETTERS:
                label = this.toLatinAlphabetNumberUpperCase(number);
                break;
            case PageLabelNumberingStyle.LOWERCASE_LETTERS:
                label = this.toLatinAlphabetNumberLowerCase(number);
                break;
        }

        return prefix ? prefix + label : label;
    }
}
