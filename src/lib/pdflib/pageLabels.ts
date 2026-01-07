import { PDFDocument, PDFName, PDFDict, PDFArray, PDFNumber, PDFString, PDFHexString } from 'pdf-lib';
import { PageLabelNumberingStyle, type PageLabel } from '@/lib/types/page-label.ts';

/**
 * Mapping from PDF internal style names (with leading slash as returned by pdf-lib) 
 * to our shared enum.
 */
const PDF_STYLE_TO_ENUM: Record<string, PageLabelNumberingStyle> = {
    '/D': PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS,
    '/R': PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS,
    '/r': PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS,
    '/A': PageLabelNumberingStyle.UPPERCASE_LETTERS,
    '/a': PageLabelNumberingStyle.LOWERCASE_LETTERS,
};

/**
 * Inverse mapping: internal enum to PDF style characters.
 */
const ENUM_TO_PDF_STYLE: Record<string, string> = {
    [PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS]: 'D',
    [PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS]: 'R',
    [PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS]: 'r',
    [PageLabelNumberingStyle.UPPERCASE_LETTERS]: 'A',
    [PageLabelNumberingStyle.LOWERCASE_LETTERS]: 'a',
};

/**
 * Extract PageLabel rules from a PDF using pdf-lib in the frontend.
 */
export async function getPageLabelRules(data: Uint8Array | ArrayBuffer): Promise<PageLabel[]> {
    try {
        const pdfDoc = await PDFDocument.load(data, { ignoreEncryption: true });
        const catalog = pdfDoc.catalog;
        const resolvedPageLabels = pdfDoc.context.lookup(catalog.get(PDFName.of('PageLabels')));

        if (!resolvedPageLabels || !(resolvedPageLabels instanceof PDFDict)) {
            return [];
        }

        const resolvedNums = pdfDoc.context.lookup(resolvedPageLabels.get(PDFName.of('Nums')));
        if (!resolvedNums || !(resolvedNums instanceof PDFArray)) {
            return [];
        }

        const rules: PageLabel[] = [];
        for (let i = 0; i < resolvedNums.size(); i += 2) {
            const indexObj = pdfDoc.context.lookup(resolvedNums.get(i));
            const labelDict = pdfDoc.context.lookup(resolvedNums.get(i + 1));

            if (!(indexObj instanceof PDFNumber) || !(labelDict instanceof PDFDict)) {
                continue;
            }

            const pageNum = indexObj.asNumber() + 1;
            
            let numberingStyle = PageLabelNumberingStyle.NONE;
            const s = pdfDoc.context.lookup(labelDict.get(PDFName.of('S')));
            if (s instanceof PDFName) {
                numberingStyle = PDF_STYLE_TO_ENUM[s.asString()] || PageLabelNumberingStyle.NONE;
            } else {
                numberingStyle = PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS;
            }

            let labelPrefix: string | null = null;
            const p = pdfDoc.context.lookup(labelDict.get(PDFName.of('P')));
            if (p instanceof PDFString || p instanceof PDFHexString) {
                labelPrefix = p.decodeText();
            }

            let firstPage = 1;
            const st = pdfDoc.context.lookup(labelDict.get(PDFName.of('St')));
            if (st instanceof PDFNumber) {
                firstPage = st.asNumber();
            }

            rules.push({ pageNum, numberingStyle, labelPrefix, firstPage });
        }

        return rules.sort((a, b) => a.pageNum - b.pageNum);
    } catch (e) {
        console.error("[PdfLib] Failed to parse page labels:", e);
        return [];
    }
}

/**
 * Set PageLabel rules in a PDF using pdf-lib and return the modified bytes.
 */
export async function setPageLabelRules(data: Uint8Array | ArrayBuffer, rules: PageLabel[]): Promise<Uint8Array> {
    const pdfDoc = await PDFDocument.load(data, { ignoreEncryption: true });
    const { context } = pdfDoc;

    // 1. Create the Nums array: [index, dict, index, dict...]
    const nums = context.obj([]);
    
    // Sort rules by pageNum (required by spec)
    const sortedRules = [...rules].sort((a, b) => a.pageNum - b.pageNum);

    for (const rule of sortedRules) {
        const pageIndex = Math.max(0, rule.pageNum - 1);
        const labelDict = context.obj({});

        // Set Style (S)
        const pdfStyleChar = ENUM_TO_PDF_STYLE[rule.numberingStyle];
        if (pdfStyleChar) {
            labelDict.set(PDFName.of('S'), PDFName.of(pdfStyleChar));
        }

        // Set Prefix (P)
        if (rule.labelPrefix) {
            labelDict.set(PDFName.of('P'), PDFString.of(rule.labelPrefix));
        }

        // Set Start Number (St)
        if (rule.firstPage !== undefined && rule.firstPage !== 1) {
            labelDict.set(PDFName.of('St'), PDFNumber.of(rule.firstPage));
        } else if (rule.firstPage === 1) {
            // Optional but good for clarity, usually 1 is default
            labelDict.set(PDFName.of('St'), PDFNumber.of(1));
        }

        nums.push(PDFNumber.of(pageIndex));
        nums.push(labelDict);
    }

    // 2. Create the PageLabels dictionary
    const pageLabelsDict = context.obj({
        Nums: nums
    });

    // 3. Update Catalog (Use Indirect Object for better compatibility)
    const pageLabelsRef = context.register(pageLabelsDict);
    pdfDoc.catalog.set(PDFName.of('PageLabels'), pageLabelsRef);

    // 4. Save and return
    return await pdfDoc.save();
}