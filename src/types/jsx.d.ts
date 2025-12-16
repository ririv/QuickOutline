// src/types/jsx.d.ts

export {}; // Ensure this file is treated as a module

declare global {
  namespace JSX {
    // The return type of our createElement function (string)
    type Element = string;

    // Intrinsic elements (HTML tags)
    interface IntrinsicElements {
      [elemName: string]: any;
      // Define Fragment to accept children
      Fragment: {};
    }

    // Property name to use for element attributes
    interface ElementAttributesProperty {
      props: {};
    }

    // Property name to use for element children
    interface ElementChildrenAttribute {
      children: {};
    }
  }
}
