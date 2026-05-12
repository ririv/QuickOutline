// Ported from StringConstants.java
// Updated regex to be compatible with JavaScript/TypeScript

export const NORM_SPACE = " ";
export const TWO_NORM_SPACE = NORM_SPACE + NORM_SPACE;
export const FOUR_NORM_SPACE = NORM_SPACE + NORM_SPACE + NORM_SPACE + NORM_SPACE;
export const INDENT_UNIT = "    ";
export const NORM_DOT = ".";

// Regex patterns
// Java: "[\\s　]" -> JS: /[\s\u3000]/ (matches standard whitespace and full-width space)
export const ONE_SPACE_REGEX = /[\s\u3000]/; 
export const ONE_SPACE_REGEX_GLOBAL = /[\s\u3000]/g;

// Java: "[.．]" -> JS: /[.．]/
export const ONE_DOT_REGEX = /[.．]/;
export const ONE_DOT_REGEX_GLOBAL = /[.．]/g;
