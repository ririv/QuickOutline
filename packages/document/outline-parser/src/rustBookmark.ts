import type { BookmarkData } from './bookmark';

export interface RustBookmark {
    id: string;
    title: string;
    pageNum: number | null;
    level: number;
    children: RustBookmark[];
}

export interface RustOutlineDocument {
    pageCount: number;
    outline: RustBookmark;
}

export function toRustBookmark(bookmark: BookmarkData): RustBookmark {
    return {
        id: bookmark.id,
        title: bookmark.title,
        pageNum: parseBookmarkPageNumber(bookmark.pageNum),
        level: bookmark.level,
        children: bookmark.children.map(toRustBookmark),
    };
}

export function fromRustBookmark(bookmark: RustBookmark): BookmarkData {
    return {
        id: bookmark.id,
        title: bookmark.title,
        pageNum: bookmark.pageNum === null ? null : String(bookmark.pageNum),
        level: bookmark.level,
        children: bookmark.children.map(fromRustBookmark),
    };
}

export function parseRustBookmark(value: unknown, path = '目录'): RustBookmark {
    const node = requireRecord(value, path);
    if (!Array.isArray(node.children)) throw new Error(`${path}.children 不是数组`);
    const pageNum = node.pageNum;
    if (pageNum !== null && !Number.isInteger(pageNum)) {
        throw new Error(`${path}.pageNum 不是整数或 null`);
    }
    return {
        id: requireString(node.id, `${path}.id`),
        title: requireString(node.title, `${path}.title`),
        pageNum: pageNum as number | null,
        level: requireInteger(node.level, `${path}.level`),
        children: node.children.map((child, index) => parseRustBookmark(child, `${path}.children[${index}]`)),
    };
}

export function parseRustOutlineDocumentJson(raw: string): RustOutlineDocument {
    let value: unknown;
    try {
        value = JSON.parse(raw);
    } catch {
        throw new Error('WASM 返回的目录不是有效 JSON');
    }
    const document = requireRecord(value, '目录文档');
    return {
        pageCount: requireInteger(document.pageCount, '目录文档.pageCount'),
        outline: parseRustBookmark(document.outline, '目录文档.outline'),
    };
}

function parseBookmarkPageNumber(value: string | null): number | null {
    if (value === null) return null;
    const page = Number.parseInt(String(value), 10);
    return Number.isNaN(page) ? null : page;
}

function requireRecord(value: unknown, path: string): Record<string, unknown> {
    if (typeof value !== 'object' || value === null || Array.isArray(value)) {
        throw new Error(`${path} 不是对象`);
    }
    return value as Record<string, unknown>;
}

function requireString(value: unknown, path: string): string {
    if (typeof value !== 'string') throw new Error(`${path} 不是字符串`);
    return value;
}

function requireInteger(value: unknown, path: string): number {
    if (!Number.isInteger(value)) throw new Error(`${path} 不是整数`);
    return value as number;
}
