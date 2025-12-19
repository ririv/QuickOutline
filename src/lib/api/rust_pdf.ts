import { invoke } from '@tauri-apps/api/core';
import type { PageLabelNumberingStyle } from '@/lib/styleMaps';
import type { HeaderFooterConfig } from './rpc';

export interface TocLinkDto {
    tocPageIndex: number;
    x: number;
    y: number;
    width: number;
    height: number;
    targetPageIndex: number;
}

export interface TocConfig {
    tocContent: string;
    title: string;
    insertPos: number;
    numberingStyle: PageLabelNumberingStyle;
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
