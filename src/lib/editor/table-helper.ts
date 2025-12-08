import { EditorView, type KeyBinding, keymap } from '@codemirror/view';
import { EditorState, type StateCommand, Text, Transaction } from '@codemirror/state';

// --- Table Helpers ---

// Check if a line is a table row (contains |)
function isTableRow(text: string): boolean {
    return text.trim().startsWith('|') || (text.includes('|') && text.trim().length > 0);
}

// Check if a line is a separator row (| --- |)
function isSeparatorRow(text: string): boolean {
    return /^[\s|:-]+$/.test(text) && text.includes('-');
}

// Count columns in a row
function countColumns(text: string): number {
    // Simple split by |, filtering empty strings caused by leading/trailing pipes
    return text.split('|').length - 2; 
}

// Command: Insert 2x2 Table
export const insertTable: StateCommand = ({ state, dispatch }) => {
    const tableTemplate = 
`| Header 1 | Header 2 |
|----------|----------|
|          |          |
`;
    const from = state.selection.main.from;
    dispatch(state.update({
        changes: { from, insert: tableTemplate },
        selection: { anchor: from + 2 }, // Put cursor in first cell
        scrollIntoView: true
        // Note: The original guide mentioned cursor in first cell which is like 2 chars after first pipe,
        // but it's simpler to just put it at "H".
        // For "| H | H |", cursor at H is from + 2
    }));
    return true;
};

// Command: Handle Enter in Table (Auto insert separator or new row)
export const handleTableEnter: StateCommand = ({ state, dispatch }) => {
    const pos = state.selection.main.head;
    const line = state.doc.lineAt(pos);
    const text = line.text;

    if (!isTableRow(text)) return false;

    // Case 1: Header Row -> Insert Separator + First Data Row
    // Heuristic: If current line is not a separator, and next line is not a separator,
    // AND current line contains content that looks like a header (not empty cells only).
    const nextLineNo = line.number + 1;
    const nextLine = nextLineNo <= state.doc.lines ? state.doc.line(nextLineNo) : null;
    
    // Check if it's potentially a header row (first row of a table, no separator below it yet)
    // A simple heuristic: if it contains text that isn't just spaces within pipes.
    const isLikelyHeader = !isSeparatorRow(text) && !/^[\s|]+$/.test(text) && text.includes('|');

    if (isLikelyHeader && (!nextLine || !isSeparatorRow(nextLine.text))) {
        // Count columns from current line
        const colCount = countColumns(text);
        if (colCount > 0) {
            let separator = "\n|";
            for(let i=0; i<colCount; i++) separator += "---";
            let nextRow = "\n|";
            for(let i=0; i<colCount; i++) nextRow += "   |"; // Three spaces for better visual alignment
            
            dispatch(state.update({
                changes: { from: line.to, insert: separator + nextRow },
                selection: { anchor: line.to + separator.length + 4 }, // Cursor in first cell of next row
                scrollIntoView: true
            }));
            return true;
        }
    }

    // Case 2: Data Row -> Insert New Empty Row
    // If currently in a table structure, Enter creates a new row.
    if (isTableRow(text)) {
         const colCount = countColumns(text);
         if (colCount > 0) {
             let nextRow = "\n|";
             for(let i=0; i<colCount; i++) nextRow += "   |";
             
             dispatch(state.update({
                 changes: { from: line.to, insert: nextRow },
                 selection: { anchor: line.to + nextRow.indexOf(' |') + 1 }, // Cursor in first cell of new row
                 scrollIntoView: true
             }));
             return true;
         }
    }

    return false;
};

// Command: Tab Navigation in Table
export const tableTab: StateCommand = ({ state, dispatch }) => {
    const pos = state.selection.main.head;
    const line = state.doc.lineAt(pos);
    const text = line.text;

    if (!isTableRow(text)) return false;

    const relativePos = pos - line.from;
    const parts = text.split('|'); // Split by pipes
    let currentCellIndex = -1;
    let currentCellStart = -1;
    
    // Find which cell the cursor is in
    let currentPipePos = -1;
    for (let i = 0; i < parts.length; i++) {
        currentPipePos += parts[i].length + 1; // +1 for the pipe itself
        if (pos <= line.from + currentPipePos) { // Cursor is before or at this pipe
            currentCellIndex = i;
            currentCellStart = line.from + currentPipePos - parts[i].length; // Approximate start of cell content
            break;
        }
    }
    
    // If cursor is to the right of last pipe, or no cells found:
    if (currentCellIndex === -1 && pos > line.from + text.lastIndexOf('|')) {
        currentCellIndex = parts.length - 1; // Assume last cell
    }

    // Navigate to next cell if possible
    if (currentCellIndex < parts.length - 2) { // -2 because first and last parts are empty due to leading/trailing pipes
        // Jump to next cell
        // A simpler way: just find the next pipe after current pos
        const nextPipeRelative = text.indexOf('|', relativePos + 1);
        if (nextPipeRelative !== -1) {
            // Check if there is another pipe after this one (ensuring we land in a cell, not outside)
            if (text.indexOf('|', nextPipeRelative + 1) !== -1) {
                dispatch(state.update({
                    selection: { anchor: line.from + nextPipeRelative + 2 }, // Jump after pipe and a space
                    scrollIntoView: true
                }));
                return true;
            }
        }
    }

    // If at end of current row, jump to next row's first cell
    const nextLineNo = line.number + 1;
    if (nextLineNo <= state.doc.lines) {
        const nextLine = state.doc.line(nextLineNo);
        if (isTableRow(nextLine.text)) {
            const firstPipe = nextLine.text.indexOf('|');
            if (firstPipe !== -1) {
                dispatch(state.update({
                    selection: { anchor: nextLine.from + firstPipe + 2 }, // After first pipe and space
                    scrollIntoView: true
                }));
                return true;
            }
        } else if (isSeparatorRow(nextLine.text)) { // Skip separator row if it's the next line
            const thirdLineNo = nextLineNo + 1;
            if (thirdLineNo <= state.doc.lines) {
                const thirdLine = state.doc.line(thirdLineNo);
                if (isTableRow(thirdLine.text)) {
                    const firstPipe = thirdLine.text.indexOf('|');
                    if (firstPipe !== -1) {
                         dispatch(state.update({
                            selection: { anchor: thirdLine.from + firstPipe + 2 },
                            scrollIntoView: true
                        }));
                        return true;
                    }
                }
            }
        }
    }
    
    // If at end of table, create new row
    // Use the same logic as handleTableEnter for creating new row
    if (isTableRow(text)) {
        const colCount = countColumns(text);
        if (colCount > 0) {
            let newRow = "\n|";
            for(let i=0; i<colCount; i++) newRow += "   |";
            
            dispatch(state.update({
                changes: { from: line.to, insert: newRow },
                selection: { anchor: line.to + newRow.indexOf(' |') + 1 }, // Cursor in first cell of new row
                scrollIntoView: true
            }));
            return true;
        }
    }

    // If we are in a table row but didn't match specific navigation logic (edge case?), 
    // consume the event to prevent inserting a Tab character.
    return true; 
};

// Command: Shift-Tab Navigation in Table (Previous cell)
export const tableShiftTab: StateCommand = ({ state, dispatch }) => {
    const pos = state.selection.main.head;
    const line = state.doc.lineAt(pos);
    const text = line.text;

    if (!isTableRow(text)) return false; // Let default shift-tab work (unindent)

    const relativePos = pos - line.from;
    
    // Find previous pipe before cursor
    // Search backward from relativePos - 1
    const prevPipeRelative = text.lastIndexOf('|', relativePos - 1);
    
    if (prevPipeRelative !== -1) {
        // Jump to previous cell start?
        // Actually we want to jump to the cell BEFORE this pipe.
        // Find pipe before THAT pipe.
        const prevPrevPipeRelative = text.lastIndexOf('|', prevPipeRelative - 1);
        if (prevPrevPipeRelative !== -1) {
             dispatch(state.update({
                selection: { anchor: line.from + prevPrevPipeRelative + 2 }, // Jump after pipe and space
                scrollIntoView: true
            }));
            return true;
        }
    }

    // If at start of current row, jump to previous row's last cell
    const prevLineNo = line.number - 1;
    if (prevLineNo >= 1) {
        let prevLine = state.doc.line(prevLineNo);
        
        // Skip separator row if needed
        if (isSeparatorRow(prevLine.text)) {
             if (prevLineNo - 1 >= 1) {
                 prevLine = state.doc.line(prevLineNo - 1);
             } else {
                 // Top of table, do nothing or consume
                 return true;
             }
        }

        if (isTableRow(prevLine.text)) {
            // Find last cell: find second to last pipe
            const lastPipe = prevLine.text.lastIndexOf('|');
            if (lastPipe !== -1) {
                const secondLastPipe = prevLine.text.lastIndexOf('|', lastPipe - 1);
                 if (secondLastPipe !== -1) {
                    dispatch(state.update({
                        selection: { anchor: prevLine.from + secondLastPipe + 2 },
                        scrollIntoView: true
                    }));
                    return true;
                }
            }
        }
    }
    
    return true; // Consume Shift-Tab in table to prevent unindent
};


export const tableKeymap: KeyBinding[] = [
    { key: "Mod-Alt-t", run: insertTable, preventDefault: true },
    { key: "Enter", run: handleTableEnter }, // Don't prevent default globally, run will return false if not handled
    { key: "Tab", run: tableTab, preventDefault: true }, // Prevent default tab
    { key: "Shift-Tab", run: tableShiftTab, preventDefault: true }, // Prevent default shift-tab
];
