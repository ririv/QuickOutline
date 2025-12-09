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
        const element = document.createElement("span");
        element.className = this.displayMode ? "cm-math-block" : "cm-math-inline";
        // Don't force display: block. Let KaTeX or context decide.
        // If it's a block widget in CM, CM handles the block layout.
        if (this.displayMode) {
             element.style.minHeight = "1em";
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

export class TableWidget extends WidgetType {
    constructor(readonly rawText: string) { super(); }

    eq(other: TableWidget) { return other.rawText === this.rawText; }

    toDOM() {
        const table = document.createElement("table");
        table.className = "cm-table-widget";
        
        const rows = this.rawText.trim().split('\n');
        if (rows.length === 0) return table;

        // Helper to parse a row line into cells
        const parseRow = (line: string) => {
            // Remove leading/trailing pipes if exist, then split
            const content = line.trim().replace(/^\||\|$/g, '');
            return content.split('|');
        };

        // 1. Header
        const headerCells = parseRow(rows[0]);
        const thead = document.createElement("thead");
        const headerRow = document.createElement("tr");
        headerCells.forEach(cell => {
            const th = document.createElement("th");
            th.textContent = cell.trim();
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);

        // 2. Alignment Row (Optional: parse for style, currently skip visual rendering)
        // Usually the 2nd row is |---|---|
        let startBodyRow = 1;
        if (rows.length > 1 && /^[|\s-:]+$/.test(rows[1])) {
            startBodyRow = 2;
            // TODO: Parse alignment (:--) from rows[1] if needed
        }

        // 3. Body
        const tbody = document.createElement("tbody");
        for (let i = startBodyRow; i < rows.length; i++) {
            const cells = parseRow(rows[i]);
            const tr = document.createElement("tr");
            cells.forEach(cell => {
                const td = document.createElement("td");
                td.textContent = cell.trim();
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
        }
        table.appendChild(tbody);

        return table;
    }
}
