export interface PageLayout {
    size: 'A4' | 'A3' | 'Letter' | 'Legal';
    orientation: 'portrait' | 'landscape';
    marginTop: number;
    marginBottom: number;
    marginLeft: number;
    marginRight: number;
}

export const defaultPageLayout: PageLayout = {
    size: 'A4',
    orientation: 'portrait',
    marginTop: 20,
    marginBottom: 20,
    marginLeft: 20,
    marginRight: 20
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

// Standard page sizes in millimeters [width, height]
export const PAGE_SIZES_MM: Record<string, [number, number]> = {
    'A4': [210, 297],
    'A3': [297, 420],
    'Letter': [215.9, 279.4],
    'Legal': [215.9, 355.6]
};