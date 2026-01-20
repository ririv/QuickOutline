// Standard page sizes with UI details. Note: w and h are in millimeters (mm)
export const PAPER_FORMAT_OPTIONS = [
    { label: 'A4', detail: '210×297mm', value: 'A4', w: 210, h: 297 },
    { label: 'A3', detail: '297×420mm', value: 'A3', w: 297, h: 420 },
    { label: 'Letter', detail: '8.5×11"', value: 'Letter', w: 215.9, h: 279.4 },
    { label: 'Legal', detail: '8.5×14"', value: 'Legal', w: 215.9, h: 355.6 }
] as const;

export type PaperFormat = typeof PAPER_FORMAT_OPTIONS[number]['value'];

// Helper map for quick lookup [width, height]
export const PAGE_SIZES_MM: Record<string, [number, number]> = Object.fromEntries(
    PAPER_FORMAT_OPTIONS.map(opt => [opt.value, [opt.w, opt.h]])
);

export interface PageMargins {
    top: number;
    bottom: number;
    left: number;
    right: number;
}

export interface PageSize {
    size: PaperFormat;
    orientation: 'portrait' | 'landscape';
}

export interface PageLayout {
    pageSize: PageSize;
    margins: PageMargins;
}

export interface ColumnLayoutConfig {
    count: number;
    direction: 'vertical' | 'horizontal'; // vertical = N-flow (down then right), horizontal = Z-flow (right then down)
    gap: number; // pt
    rule: boolean;
}

export const defaultColumnLayout: ColumnLayoutConfig = {
    count: 1,
    direction: 'vertical',
    gap: 20, 
    rule: false
};

export const defaultPageLayout: PageLayout = {
    pageSize: {
        size: 'A4',
        orientation: 'portrait'
    },
    margins: {
        top: 20,
        bottom: 20,
        left: 20,
        right: 20
    }
};

export interface HeaderFooterLayout {
    headerDist?: number;
    footerDist?: number;
    headerPadding?: number; // pt
    footerPadding?: number; // pt
}

export const defaultHeaderFooterLayout: HeaderFooterLayout = {
    headerDist: 10,
    footerDist: 10,
    headerPadding: 1,
    footerPadding: 1
};

export interface SectionConfig {
    left: string;
    center: string;
    right: string;
    inner: string;
    outer: string;
    drawLine: boolean;
}
