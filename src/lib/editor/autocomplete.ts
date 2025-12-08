import { CompletionContext, type CompletionResult } from "@codemirror/autocomplete";
import { syntaxTree } from "@codemirror/language";
import { EditorState } from "@codemirror/state";

// Helper to slugify heading text
function slugify(text: string): string {
    return text
        .toLowerCase()
        .replace(/[^\w\u4e00-\u9fa5\s-]/g, '') // Keep letters, numbers, chinese, spaces, hyphens
        .trim()
        .replace(/\s+/g, '-');
}

// Collect all headings in the document
function getHeadings(state: EditorState) {
    const headings: { label: string, detail: string, apply: string }[] = [];
    
    syntaxTree(state).iterate({
        enter: (node) => {
            if (node.name.startsWith('ATXHeading') || node.name === 'SetextHeading') {
                const text = state.sliceDoc(node.from, node.to);
                // Remove # marks and newlines
                const cleanText = text.replace(/^#+\s+/, '').replace(/\n/g, '').trim();
                const level = node.name.startsWith('ATXHeading') 
                    ? node.name.replace('ATXHeading', 'H') 
                    : (node.name === 'SetextHeading1' ? 'H1' : 'H2');
                
                const slug = slugify(cleanText);
                
                headings.push({
                    label: cleanText,
                    detail: level,
                    apply: `#${slug}` // Insert #slug
                });
            }
        }
    });
    return headings;
}

// The completion source function
export function linkHeadingCompletion(context: CompletionContext): CompletionResult | null {
    // We want to trigger when cursor is after # inside a link url: [Link](#...)
    
    // Pattern: 
    // 1. We are inside a Link url part: `[text](...)`
    // 2. The content starts with #
    
    // Regex explanation:
    // Match `](` followed by `#` and then optional word characters (including Chinese, hyphens).
    // Note: We don't try to match the full `[text]` part backwards as it's complex and `matchBefore` is limited.
    // Matching `](` is usually sufficient context.
    
    const before = context.matchBefore(/]\(#[\w\u4e00-\u9fa5-]*$/);
    
    if (!before) return null;
    
    // Calculate start of completion (the # character)
    const hashIndex = before.text.indexOf('#');
    const from = before.from + hashIndex; // position of #
    
    if (context.explicit || before) {
        return {
            from: from,
            options: getHeadings(context.state),
            filter: true // Fuzzy filter by default
        };
    }
    
    return null;
}
