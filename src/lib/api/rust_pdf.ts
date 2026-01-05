import { invoke } from '@tauri-apps/api/core';
import type { PageLabelNumberingStyle } from '@/lib/styleMaps';
import type { ViewScaleType } from '@/lib/types/pdf';
import type { BookmarkData } from '@/lib/types/bookmark';
import type { HeaderFooterConfig } from '@/lib/types/header-footer';

export type LoadMode = 'DirectFile' | 'MemoryBuffer';

export interface PageLabel {
    pageNum: number;
    numberingStyle: PageLabelNumberingStyle;
    labelPrefix?: string | null;
    firstPage?: number;
}

// ... existing convertForRust/convertFromRust ...

function convertForRust(bookmark: any): any {
    const copy = { ...bookmark };
    if (copy.pageNum !== null && copy.pageNum !== undefined) {
        const num = parseInt(String(copy.pageNum), 10);
        copy.pageNum = isNaN(num) ? null : num;
    } else {
        copy.pageNum = null;
    }
    if (copy.children && Array.isArray(copy.children)) {
        copy.children = copy.children.map((child: any) => convertForRust(child));
    }
    return copy;
}

function convertFromRust(bookmark: any): any {
    const copy = { ...bookmark };
    if (copy.pageNum !== null && copy.pageNum !== undefined) {
        copy.pageNum = String(copy.pageNum);
    } else {
        copy.pageNum = null;
    }
    if (copy.children && Array.isArray(copy.children)) {
        copy.children = copy.children.map((child: any) => convertFromRust(child));
    }
    return copy;
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
 * Loads a PDF document into the session cache.
 * @param path Path to the PDF file.
 * @param mode Load mode ('DirectFile' or 'MemoryBuffer'). Defaults to 'DirectFile' if omitted by backend, but explicit is better.
 */
export async function loadDocument(path: string, mode?: LoadMode): Promise<void> {
    return invoke('load_pdf_document', { path, mode });
}

/**
 * Closes a PDF document session, releasing memory/handles.
 * @param path Path to the PDF file.
 */
export async function closeDocument(path: string): Promise<void> {
    return invoke('close_pdf_document', { path });
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
// ...

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

/**
 * Gets the page label rules from the PDF using Rust backend.
 * @param path Path to the PDF file.
 * @returns Promise resolving to a list of PageLabel rules.
 */
export async function getPageLabelRules(path: string): Promise<PageLabel[]> {
    return invoke<PageLabel[]>('get_page_label_rules', { path });
}

/**
 * Gets the formatted page labels from the PDF using Rust backend.
 * @param path Path to the PDF file.
 * @returns Promise resolving to a list of page label strings.
 */
export async function getPageLabels(path: string): Promise<string[]> {
    return invoke<string[]>('get_page_labels', { path });
}

/**
 * Simulates page labels based on the provided rules using Rust backend.
 * @param rules List of page label rules.
 * @param totalPages Total number of pages to generate labels for.
 * @returns Promise resolving to a list of page label strings.
 */
export async function simulatePageLabels(rules: PageLabel[], totalPages: number): Promise<string[]> {
    return invoke<string[]>('simulate_page_labels', { rules, totalPages });
}

/**
 * Fetches the outline as a hierarchical bookmark structure.
 */
export async function getOutlineAsBookmark(srcFilePath: string, offset: number): Promise<BookmarkData> {
    const result = await invoke<any>('get_outline_as_bookmark', { path: srcFilePath, offset });
    return convertFromRust(result);
}

/**
 * Saves the outline to a PDF file.
 */
export async function saveOutline(
    srcFilePath: string, 
    bookmarkRoot: BookmarkData, 
    destFilePath: string | null, 
    offset: number, 
    viewMode: ViewScaleType = 'NONE'
): Promise<string> {
    const rustRoot = convertForRust(bookmarkRoot);
    return invoke<string>('save_outline', { 
        srcPath: srcFilePath, 
        bookmarkRoot: rustRoot, 
        destPath: destFilePath, 
        offset, 
        viewMode 
    });
}

/**

 * Opens the external editor (VS Code) with the provided content and cursor position.

 */

export async function openExternalEditor(
    content: string,
    line: number = 1,
    col: number = 1,
    editorId: string = 'auto'
): Promise<void> {
    return invoke('open_external_editor', { content, line, col, editorId });
}

/**
 * Extracts the visual TOC from the PDF document based on layout analysis.
 * @param srcFilePath Path to the source PDF.
 * @returns Promise resolving to a list of strings (lines of the TOC).
 */
export async function extractToc(srcFilePath: string): Promise<string[]> {
    return invoke<string[]>('extract_toc', { path: srcFilePath });
}
