export interface Bookmark {
    id: string;
    title: string;
    page: number | null; // Changed to allow null, matching Java Integer
    level: number;
    children: Bookmark[];
}
