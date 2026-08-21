export type PageNumberDisplayMode = 'original' | 'calculated';

export function detectPageOffset(labels: readonly string[]): number {
    const firstNumberedPageIndex = labels.indexOf('1');
    return firstNumberedPageIndex > 0 ? firstNumberedPageIndex : 0;
}

export function resolveDisplayedPageNumber(
    pageNum: string | null,
    offset: number,
    mode: PageNumberDisplayMode,
): string {
    if (!pageNum || mode === 'original') return pageNum ?? '';
    if (pageNum.startsWith('#') || pageNum.startsWith('@')) return pageNum;
    const numericPage = Number.parseInt(pageNum, 10);
    return Number.isNaN(numericPage) ? pageNum : String(numericPage + offset);
}
