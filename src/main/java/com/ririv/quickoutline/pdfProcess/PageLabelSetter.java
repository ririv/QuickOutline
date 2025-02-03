package com.ririv.quickoutline.pdfProcess;

import java.io.IOException;
import java.util.List;

public interface PageLabelSetter<T> {

    void setPageLabels(String srcFilePath, String destFilePath, List<PageLabel> labelList) throws IOException;

    T mapPageLabelNumberingStyle(PageLabel.PageLabelNumberingStyle numberingStyle);
}
