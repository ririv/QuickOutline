import { PDFDocument, PDFName, PDFDict, PDFArray, PDFNumber, PDFString, PDFHexString } from 'pdf-lib';
import { PageLabelNumberingStyle, type PageLabel } from '@/lib/pdf-processing/page-label';

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
 * Extract PageLabel rules from a PDF using pdf-lib in the frontend.
 * Optimized to be extremely fast and bypass Rust lopdf bottlenecks.
 */
export async function getPageLabelRulesFromPdf(arrayBuffer: ArrayBuffer): Promise<PageLabel[]> {
    try {
        const pdfDoc = await PDFDocument.load(arrayBuffer, { ignoreEncryption: true });
        const catalog = pdfDoc.catalog;
        
        // Use context.lookup to safely resolve indirect references
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

            const pageNum = indexObj.asNumber() + 1; // 0-based to 1-based
            
            // Extract Style (S)
            let numberingStyle = PageLabelNumberingStyle.NONE;
            const s = pdfDoc.context.lookup(labelDict.get(PDFName.of('S')));
            if (s instanceof PDFName) {
                numberingStyle = PDF_STYLE_TO_ENUM[s.asString()] || PageLabelNumberingStyle.NONE;
            } else {
                // If S is missing, spec says it's Decimal by default
                numberingStyle = PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS;
            }

            // Extract Prefix (P)
            // Supports both literal strings (PDFString) and hex strings (PDFHexString)
            let labelPrefix: string | null = null;
            const p = pdfDoc.context.lookup(labelDict.get(PDFName.of('P')));
            if (p instanceof PDFString || p instanceof PDFHexString) {
                labelPrefix = p.decodeText();
            }

            // Extract Start Number (St)
            let firstPage = 1;
            const st = pdfDoc.context.lookup(labelDict.get(PDFName.of('St')));
            if (st instanceof PDFNumber) {
                firstPage = st.asNumber();
            }

            rules.push({ pageNum, numberingStyle, labelPrefix, firstPage });
        }

        return rules
        // Final safety: Ensure rules are sorted by page index
        // return rules.sort((a, b) => a.pageNum - b.pageNum);

    } catch (e) {
        console.error("[PdfLib] Failed to parse page labels:", e);
        return [];
    }
}
