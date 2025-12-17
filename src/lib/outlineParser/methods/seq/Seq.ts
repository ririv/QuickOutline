export function getLevelByStandardSeq(seq: string): number {
    let cleanSeq = seq;
    if (cleanSeq.endsWith(".")) {
        cleanSeq = cleanSeq.substring(0, cleanSeq.length - 1); // Remove trailing dot
    }
    
    let level = 1;
    if (!cleanSeq) {
        return level;
    } else {
        // Count dots. 1 -> level 1. 1.1 -> level 2.
        // Java: while(seq.contains(".")) { ... }
        // We can just split by dot.
        const parts = cleanSeq.split(".");
        // If "1", parts=["1"], length 1. level 1.
        // If "1.1", parts=["1", "1"], length 2. level 2.
        // If "1.1.1", parts=["1","1","1"], length 3. level 3.
        
        // Wait, let's verify Java logic:
        // while (seq.contains(".")) { seq = seq.replaceFirst("\\.", ""); level++; }
        // "1.1" -> contains dot -> remove dot -> "11", level=2. contains dot? No. Result 2.
        // "1.2.5" -> "12.5" (level 2) -> "125" (level 3). Result 3.
        // Logic matches split length.
        
        // However, we should be careful about "1..1" or similar if they exist, but standard seq usually is well formed.
        // Using split is safer/cleaner in JS.
        
        return parts.length;
    }
}
