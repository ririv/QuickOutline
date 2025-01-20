package com.ririv.quickoutline.pdfProcess;

import java.util.List;

public interface TocExtractor {

    List<String> extract();
    List<String> extract(int startPageNum, int endPageNum);

}
