package com.ririv.quickoutline.textProcess.methods.seq;

public interface Seq {

    default int getLevelByStandardSeq(String seq) {
        if (seq.endsWith(".")) {
            seq = seq.substring(0, seq.length() - 1); // 去掉末尾的 .
        }
        int level = 1;
        if (seq == null || seq.isEmpty()){
            return level;
        }
        else {
            while (seq.contains(".")) {
                seq = seq.replaceFirst("\\.", "");
                level++;
            }
            return level;
        }
    }
}
