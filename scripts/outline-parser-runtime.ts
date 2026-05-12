import { Method, processText, serializeBookmarkTree } from 'outline-parser';
import { toBookmarkData } from 'outline-parser/bookmarkUtils';
import type { BookmarkData } from 'outline-parser/bookmark';

type RustBookmark = Omit<BookmarkData, 'pageNum' | 'children'> & {
  pageNum: number | null;
  children: RustBookmark[];
};

function parseMethod(value: string | undefined) {
  switch (value || 'seq') {
    case 'indent':
      return Method.INDENT;
    case 'seq':
      return Method.SEQ;
    default:
      throw new Error(`Invalid method: ${value}`);
  }
}

function toRustBookmark(bookmark: BookmarkData): RustBookmark {
  const pageNum = bookmark.pageNum == null || bookmark.pageNum === ''
    ? null
    : Number.parseInt(bookmark.pageNum, 10);

  if (Number.isNaN(pageNum)) {
    throw new Error(`Invalid page number: ${bookmark.pageNum}`);
  }

  return {
    id: bookmark.id,
    title: bookmark.title,
    pageNum,
    level: bookmark.level,
    children: bookmark.children.map(toRustBookmark),
  };
}

function toBookmarkDataForSerialize(bookmark: RustBookmark): BookmarkData {
  return {
    id: bookmark.id,
    title: bookmark.title,
    pageNum: bookmark.pageNum == null ? null : String(bookmark.pageNum),
    level: bookmark.level,
    children: bookmark.children.map(toBookmarkDataForSerialize),
  };
}

export function parseOutline(text: string, method?: string): string {
  const tree = processText(text, parseMethod(method));
  return JSON.stringify(toRustBookmark(toBookmarkData(tree)));
}

export function serializeOutline(input: string): string {
  const tree = JSON.parse(input) as RustBookmark;
  return serializeBookmarkTree(toBookmarkDataForSerialize(tree));
}
