export function detectPageOffset(labels: readonly string[]): number {
    const firstNumberedPageIndex = labels.indexOf('1');
    return firstNumberedPageIndex > 0 ? firstNumberedPageIndex : 0;
}
