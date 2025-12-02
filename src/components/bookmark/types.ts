export interface Bookmark {
    id: string;
    title: string;
    page: string | null; // Changed to string to match backend model (flexibility) and frontend input
    level: number;
    children: Bookmark[];
    expanded?: boolean;
}
