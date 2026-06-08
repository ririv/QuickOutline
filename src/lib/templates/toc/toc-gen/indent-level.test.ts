import { createTocIndentLevelResolver } from "./indent-level";

function test(name: string, fn: () => void) {
    try {
        fn();
        console.log(`ok - ${name}`);
    } catch (error) {
        throw new Error(`${name}: ${error instanceof Error ? error.message : String(error)}`);
    }
}

function assertEqual(actual: number, expected: number) {
    if (actual !== expected) {
        throw new Error(`expected ${expected}, got ${actual}`);
    }
}

test("uses the first indented line as the indent unit", () => {
    const resolveIndentLevel = createTocIndentLevelResolver([
        "Chapter 1 ... 1",
        "    Section 1.1 ... 2",
        "        Detail 1.1.1 ... 3"
    ]);

    assertEqual(resolveIndentLevel(""), 0);
    assertEqual(resolveIndentLevel("    "), 1);
    assertEqual(resolveIndentLevel("        "), 2);
});

test("keeps two-space indented content consistent with indent parsing", () => {
    const resolveIndentLevel = createTocIndentLevelResolver([
        "Chapter 1 ... 1",
        "  Section 1.1 ... 2",
        "    Detail 1.1.1 ... 3"
    ]);

    assertEqual(resolveIndentLevel(""), 0);
    assertEqual(resolveIndentLevel("  "), 1);
    assertEqual(resolveIndentLevel("    "), 2);
});

test("supports tab indented content", () => {
    const resolveIndentLevel = createTocIndentLevelResolver([
        "Chapter 1 ... 1",
        "\tSection 1.1 ... 2",
        "\t\tDetail 1.1.1 ... 3"
    ]);

    assertEqual(resolveIndentLevel(""), 0);
    assertEqual(resolveIndentLevel("\t"), 1);
    assertEqual(resolveIndentLevel("\t\t"), 2);
});
