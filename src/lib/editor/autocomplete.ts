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
    const pos = context.pos;
    const doc = context.state.doc;
    const line = doc.lineAt(pos);
    const rel = pos - line.from;
    const s = line.text;

    // Find last '(' before cursor and check pattern [...](|here)
    const openParenIdx = s.lastIndexOf('(', rel);
    if (openParenIdx < 0) return null;

    // Check for ']' immediately before '('
    const rb = openParenIdx - 1;
    if (rb < 0 || s[rb] !== ']') return null;

    // Check for corresponding '[' before ']
    const lb = s.lastIndexOf('[', rb);
    if (lb < 0) return null;

    const closeParenIdx = s.indexOf(')', rel);
    // Determine bounds of the URL part
    const urlFrom = line.from + openParenIdx + 1;
    const urlTo = closeParenIdx >= 0 ? line.from + closeParenIdx : pos;

    // Ensure cursor is within the URL part
    if (pos < urlFrom || pos > urlTo) return null;

    // Only trigger if the URL part typed so far starts with '#'
    const typed = doc.sliceString(urlFrom, pos);
    if (!typed.startsWith('#')) return null;

        // If explicitly requested or triggering after '#', show completions
        // We want to complete the slug after the '#'
        if (context.explicit || typed.startsWith('#')) {
            const options = getHeadings(context.state).map(h => ({
                label: h.label,
                detail: h.detail, // e.g., "H1"
                apply: h.apply, 
                type: h.detail // Use "H1", "H2" etc as custom type for icon styling
            }));
    
            return {
                from: urlFrom, 
                to: urlTo,
                options: options,
                filter: false 
            };
        }    
    return null;
}
