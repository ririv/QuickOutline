import { invoke } from '@tauri-apps/api/core';
import type { PageLabelNumberingStyle } from '@/lib/styleMaps';
import type { HeaderFooterConfig } from './rpc';

export interface PageLabel {
    pageNum: number;
    numberingStyle: PageLabelNumberingStyle;
    labelPrefix?: string | null;
    firstPage?: number | null;
}

export interface TocLinkDto {
    tocPageIndex: number;
    x: number;
    y: number;
    width: number;
    height: number;
    targetPageIndex: number;
    targetIsOriginalDoc: boolean;
}

export interface TocConfig {
    tocContent: string;
    title: string;
    insertPos: number;
    tocPageLabel: PageLabel | null;
    header: HeaderFooterConfig | null;
    footer: HeaderFooterConfig | null;
    tocPdfPath?: string;
    links?: TocLinkDto[];
}

/**
 * Invokes the Rust backend to generate and insert the TOC page into the PDF.
 * @param srcFilePath Path to the source PDF.
 * @param config Configuration for the TOC generation.
 * @param destFilePath Optional destination path. If null, a new file will be created automatically.
 * @returns Promise resolving to the path of the generated PDF.
 */
export async function generateTocPage(
    srcFilePath: string, 
    config: TocConfig, 
    destFilePath: string | null
): Promise<string> {
    return invoke<string>('generate_toc_page', {
        srcPath: srcFilePath,
        config: config,
        destPath: destFilePath
    });
}

/**
 * Invokes the Rust backend to set page labels for the PDF.
 * @param srcFilePath Path to the source PDF.
 * @param rules List of page label rules.
 * @param destFilePath Optional destination path.
 * @returns Promise resolving to the path of the modified PDF.
 */
export async function setPageLabels(
    srcFilePath: string,
    rules: PageLabel[],
    destFilePath: string | null
): Promise<string> {
    return invoke<string>('set_page_labels', {
        srcPath: srcFilePath,
        rules: rules,
        destPath: destFilePath
    });
}
