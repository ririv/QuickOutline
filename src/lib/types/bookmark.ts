export interface BookmarkData {
    id: string;
    title: string;
    pageNum: string | null; 
    level: number;
    children: BookmarkData[];
}

export interface BookmarkUI extends Omit<BookmarkData, 'children'> {
    expanded?: boolean;
    children: BookmarkUI[];
}