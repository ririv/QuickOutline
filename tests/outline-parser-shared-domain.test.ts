import { describe, expect, test } from 'bun:test';

import {
    createBookmark,
    withBookmarkIdGenerator,
} from '../packages/document/outline-parser/src/bookmarkUtils';
import { detectPageOffset } from '../packages/document/outline-parser/src/pageOffset';
import {
    fromRustBookmark,
    parseRustOutlineDocumentJson,
    toRustBookmark,
} from '../packages/document/outline-parser/src/rustBookmark';

describe('跨端目录领域逻辑', () => {
    test('桌面目录与 Rust 目录格式可以往返转换', () => {
        const bookmark = createTestBookmark('chapter-1', ' 第一章 ', '7');
        const rustBookmark = toRustBookmark(bookmark);

        expect(rustBookmark).toEqual({
            id: 'chapter-1',
            title: ' 第一章 ',
            pageNum: 7,
            level: 1,
            children: [],
        });
        expect(fromRustBookmark(rustBookmark)).toEqual({
            id: 'chapter-1',
            title: ' 第一章 ',
            pageNum: '7',
            level: 1,
            children: [],
        });
    });

    test('保持桌面端原有的页码转换规则', () => {
        const bookmark = createTestBookmark('chapter-1', '第一章', '7.5');
        expect(toRustBookmark(bookmark).pageNum).toBe(7);
    });

    test('校验 WASM 返回的目录文档', () => {
        const document = parseRustOutlineDocumentJson(JSON.stringify({
            pageCount: 12,
            outline: {
                id: 'root',
                title: 'Outlines',
                pageNum: null,
                level: 0,
                children: [],
            },
        }));
        expect(document.pageCount).toBe(12);
        expect(document.outline.id).toBe('root');
    });

    test('复用桌面端的页码偏移自动探测规则', () => {
        expect(detectPageOffset(['封面', '1', '2'])).toBe(1);
        expect(detectPageOffset(['1', '2'])).toBe(0);
        expect(detectPageOffset(['封面', '目录'])).toBe(0);
    });
});

function createTestBookmark(id: string, title: string, page: string) {
    return withBookmarkIdGenerator(() => id, () => createBookmark(title, page, 1));
}
