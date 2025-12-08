import { BlockContext, type LeafBlockParser, Line, type MarkdownConfig, LeafBlock } from '@lezer/markdown';
import { tags } from '@lezer/highlight';

// Define a named LeafBlockParser class for BlockMath
class BlockMathLeafParser implements LeafBlockParser {
    nextLine(cx: BlockContext, line: Line, leaf: LeafBlock): boolean {
        if (line.text.slice(line.pos).trim().startsWith('$$')) {
            // Found closing fence
            // We stop the block *after* this line (consuming the closing $$).
            // Returning false typically stops the block.
            // Does it consume the current line?
            // For Paragraphs, returning false stops *before* the line if it matches something else?
            // But here we are inside our own parser.
            // Let's try returning false.
            return false; 
        }
        return true; // Continue consuming lines
    }
    
    finish(cx: BlockContext, leaf: LeafBlock): boolean {
        // When finished, register the element.
        // leaf.start is the start of the block.
        // leaf.content is the text? No, we add element to cx.
        cx.addLeafElement(leaf, cx.elt("BlockMath", leaf.start, cx.parsedPos, [
            // We could add children like markers here if we tracked them
        ]));
        return true;
    }
}

export const MathExtension: MarkdownConfig = {
    defineNodes: [
        { name: "BlockMath", block: true, style: tags.special(tags.content) },
        { name: "DisplayMath", style: tags.special(tags.content) }, // Inline display math
        { name: "InlineMath", style: tags.special(tags.content) }
    ],
    parseBlock: [{
        name: "BlockMath",
        // We use 'leaf' instead of 'parse' to handle this as a Paragraph-like block.
        leaf(cx: BlockContext, leaf: LeafBlock): LeafBlockParser | null {
            const match = /^(\s*)\$\$/.exec(leaf.content);
            if (match) {
                if (leaf.content.trim().startsWith('$$')) {
                     return new BlockMathLeafParser();
                }
            }
            return null;
        }
    }],
    parseInline: [{
        name: "InlineMath",
        parse(cx, next, pos) {
            if (next != 36) return -1; // '$'
            
            // Check for DisplayMath $$...$$ (Inline context)
            if (cx.char(pos + 1) == 36) {
                let end = pos + 2;
                while (end < cx.end) {
                    if (cx.char(end) == 36 && cx.char(end + 1) == 36) {
                        cx.addElement(cx.elt("DisplayMath", pos, end + 2));
                        return end + 2;
                    }
                    end++;
                }
                // If unclosed, treat as plain text or let it fall through? 
                // Or treat as InlineMath start if we want to support $ inside $$? Unlikely.
                return -1; 
            }
            
            // InlineMath $...$
            let end = pos + 1;
            while (end < cx.end) {
                if (cx.char(end) == 36) {
                    cx.addElement(cx.elt("InlineMath", pos, end + 1));
                    return end + 1;
                }
                end++;
            }
            return -1;
        }
    }]
};