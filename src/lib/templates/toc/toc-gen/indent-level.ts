function getLeadingWhitespace(line: string): string {
    let end = 0;
    while (end < line.length && line[end].trim() === '') {
        end++;
    }
    return line.slice(0, end);
}

function findIndentUnit(lines: readonly string[]): string {
    for (const line of lines) {
        if (!line.trim()) continue;

        const prefix = getLeadingWhitespace(line);
        if (prefix.length > 0) {
            return prefix;
        }
    }

    return '';
}

export function createTocIndentLevelResolver(lines: readonly string[]): (whitespace: string) => number {
    const indentUnit = findIndentUnit(lines);

    return (whitespace: string) => {
        if (!indentUnit) return 0;

        let level = 0;
        let remaining = whitespace;
        while (remaining.startsWith(indentUnit)) {
            level++;
            remaining = remaining.slice(indentUnit.length);
        }

        return level;
    };
}
