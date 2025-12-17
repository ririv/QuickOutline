import { processText, Method, containsChinese } from "./index";
import { CnSeqParser } from "./methods/seq/CnSeqParser";
import type { Bookmark } from "../../components/bookmark/types";

// Simple test runner helper
function test(name: string, fn: () => void) {
    console.log(`\n--- Running ${name} ---`);
    try {
        fn();
        console.log(`✅ ${name} passed`);
    } catch (e) {
        console.error(`❌ ${name} failed`);
        console.error(e);
        // process.exit(1); 
    }
}

function assert(condition: boolean, message: string) {
    if (!condition) {
        throw new Error(message);
    }
}

function printTree(node: Bookmark, depth = 0) {
    console.log("  ".repeat(depth) + `- ${node.title} (L${node.level})
`);
    if (node.children) {
        node.children.forEach(c => printTree(c, depth + 1));
    }
}

// Tests

test("Debug: Chinese Detection", () => {
    const text = "第1章 绪论 1";
    assert(containsChinese(text), "Should detect Chinese");
    console.log("Chinese detection works.");
});

test("Debug: CnSeqParser Single Line", () => {
    const parser = new CnSeqParser();
    const line = "第1章 绪论 1";
    const result = parser.parseLine(line, []);
    console.log("Parsed Line:", result);
    // expect level 1 for "1"
    assert(result.level === 1, `Expected level 1, got ${result.level}`);
});


test("CnSeqParser: Chinese format with digits", () => {
    const text = `
第1章 绪论 1
1.1 研究背景 2
1.1.1 细节 3
第2章 结论 10
`;
    console.log("Input text:", JSON.stringify(text));
    const root = processText(text); // Auto-detect Chinese
    
    console.log("Parsed Tree:");
    printTree(root);
    
    assert(root.children.length === 2, `Should have 2 chapters, but got ${root.children.length}`);
    assert(root.children[0].level === 1, "Chap 1 Level");
    
    // "1.1" -> L2
    assert(root.children[0].children.length === 1, "Chap 1 children");
    assert(root.children[0].children[0].level === 2, "Section 1.1 level");
    
    // "1.1.1" -> L3
    assert(root.children[0].children[0].children.length === 1, "Detail level");
});