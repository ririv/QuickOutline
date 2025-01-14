package com.ririv.quickoutline.pdfProcess;

import java.util.List;

public interface TocProcessor {

    List<String> extract();
    List<String> extract(int startPageNum, int endPageNum);

}
