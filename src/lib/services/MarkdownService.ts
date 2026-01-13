import type MarkdownIt from 'markdown-it';
import { createMdParser, type MarkdownParserOptions } from '@/lib/editor/markdown-renderer';

class MarkdownService {
    private parser: MarkdownIt | null = null;
    private currentOptions: MarkdownParserOptions | null = null;

    private getParser(options: MarkdownParserOptions): MarkdownIt {
        // Recreate parser only if options have changed or it doesn't exist
        if (!this.parser || !this.areOptionsEqual(this.currentOptions, options)) {
            this.parser = createMdParser(options);
            this.currentOptions = { ...options };
        }
        return this.parser;
    }

    private areOptionsEqual(a: MarkdownParserOptions | null, b: MarkdownParserOptions): boolean {
        if (!a) return false;
        return a.enableIndentedCodeBlocks === b.enableIndentedCodeBlocks;
    }

    public compileHtml(content: string, options: MarkdownParserOptions = {}): string {
        const parser = this.getParser(options);
        return parser.render(content);
    }
}

export const markdownService = new MarkdownService();
