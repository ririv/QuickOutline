export interface Bookmark {
    id: string;
    title: string;
    page: number;
    level: number;
    children: Bookmark[];
}
