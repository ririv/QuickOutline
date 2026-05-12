/// <reference types="node" />

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

function getArgValue(args: string[], name: string) {
  const index = args.indexOf(name);
  return index >= 0 ? args[index + 1] : undefined;
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

async function readStdin() {
  const chunks: Buffer[] = [];
  for await (const chunk of process.stdin) {
    chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
  }
  return Buffer.concat(chunks).toString('utf8');
}

async function main() {
  const [mode, ...args] = process.argv.slice(2);
  const input = await readStdin();

  switch (mode) {
    case 'parse': {
      const tree = processText(input, parseMethod(getArgValue(args, '--method')));
      process.stdout.write(JSON.stringify(toRustBookmark(toBookmarkData(tree))));
      return;
    }
    case 'serialize': {
      const tree = JSON.parse(input) as RustBookmark;
      process.stdout.write(serializeBookmarkTree(toBookmarkDataForSerialize(tree)));
      return;
    }
    default:
      throw new Error(`Unsupported mode: ${mode || '<empty>'}`);
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
});

