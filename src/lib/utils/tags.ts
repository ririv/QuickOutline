// Identity function for tagged template literals
const raw = (strings: TemplateStringsArray, ...values: any[]) => String.raw({ raw: strings }, ...values);

// Helper to enable CSS syntax highlighting in editors (e.g., via vscode-styled-components extension)
export const css = raw;

// Helper to enable HTML syntax highlighting in editors (e.g., via es6-string-html extension)
export const html = raw;
