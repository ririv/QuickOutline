import { WidgetType, EditorView } from '@codemirror/view';
import katex from 'katex';
import 'katex/dist/katex.min.css';

export class BulletWidget extends WidgetType {
    eq(other: BulletWidget) { return true; } // All bullets are the same, reuse DOM

    ignoreEvent() { return false; }

    toDOM() {
        const span = document.createElement("span");
        span.className = "cm-bullet-widget";
        span.textContent = "•";
        span.style.paddingRight = "0.5em";
        span.style.color = "#555";
        span.style.fontWeight = "bold";
        return span;
    }
}

export class OrderedListWidget extends WidgetType {
    constructor(readonly number: string) { super(); }
    
    eq(other: OrderedListWidget) { return other.number === this.number; }
    
    toDOM() {
        const span = document.createElement("span");
        span.className = "cm-ordered-widget";
        span.textContent = this.number;
        span.style.paddingRight = "0.5em";
        span.style.color = "#555";
        span.style.fontWeight = "bold";
        // Use monospaced or aligned font if possible to avoid wobble
        return span;
    }
}

export class HorizontalRuleWidget extends WidgetType {
    toDOM() {
        const hr = document.createElement("hr");
        hr.className = "cm-hr-widget";
        hr.style.border = "none";
        hr.style.borderTop = "2px solid #eee"; // Thicker line
        hr.style.margin = "1em 0";
        return hr;
    }
}

export class CheckboxWidget extends WidgetType {
    checked: boolean;
    
    constructor(checked: boolean) {
        super();
        this.checked = checked;
    }
    
    eq(other: CheckboxWidget) { return other.checked === this.checked; }
    
    toDOM(view: EditorView) {
        const wrap = document.createElement("span");
        wrap.className = "cm-checkbox-widget";
        wrap.style.paddingRight = "0.5em";
        wrap.style.cursor = "pointer";
        
        const input = document.createElement("input");
        input.type = "checkbox";
        input.checked = this.checked;
        input.style.cursor = "pointer";
        
        input.onclick = (e) => {
            e.preventDefault(); 
            const pos = view.posAtDOM(wrap);
            if (pos == null) return;

            const line = view.state.doc.lineAt(pos);
            const relativePos = pos - line.from;
            const lineText = line.text;
            
            const openBracketIndex = lineText.indexOf('[', relativePos);
            if (openBracketIndex === -1) return;
            
            const toggleCharPos = line.from + openBracketIndex + 1;
            const charToSet = this.checked ? " " : "x"; 
            
            view.dispatch({
                changes: { from: toggleCharPos, to: toggleCharPos + 1, insert: charToSet }
            });
        };
        
        wrap.appendChild(input);
        return wrap;
    }
}

export class ImageWidget extends WidgetType {
    url: string;
    alt: string;
    
    constructor(url: string, alt: string) { 
        super(); 
        this.url = url;
        this.alt = alt;
    }

    eq(other: ImageWidget) { return other.url === this.url && other.alt === this.alt; }

    toDOM() {
        const img = document.createElement("img");
        img.src = this.url;
        img.alt = this.alt;
        img.className = "cm-image-preview";
        img.style.maxWidth = "100%";
        img.style.maxHeight = "400px";
        img.style.display = "block";
        img.style.margin = "0.5em auto";
        img.style.borderRadius = "4px";
        return img;
    }
}

export class MathWidget extends WidgetType {
    formula: string;
    displayMode: boolean;

    constructor(formula: string, displayMode: boolean) { 
        super(); 
        this.formula = formula;
        this.displayMode = displayMode;
    }

    eq(other: MathWidget) { return other.formula === this.formula && other.displayMode === this.displayMode; }

    toDOM() {
        const tagName = this.displayMode ? "div" : "span";
        const element = document.createElement(tagName);
        element.className = this.displayMode ? "cm-math-block" : "cm-math-inline";
        if (this.displayMode) {
                element.style.display = "block";
                element.style.textAlign = "center";
                element.style.margin = "1em 0";
        }
        try {
            katex.render(this.formula, element, {
                displayMode: this.displayMode,
                throwOnError: false
            });
        } catch (e) {
            element.textContent = this.formula;
            element.style.color = "red";
        }
        return element;
    }
}
