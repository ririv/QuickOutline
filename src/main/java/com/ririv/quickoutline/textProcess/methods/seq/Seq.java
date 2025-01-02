package com.ririv.quickoutline.textProcess.methods.seq;

public interface Seq {

    default int getLevelByStandardSeq(String seq) {
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
