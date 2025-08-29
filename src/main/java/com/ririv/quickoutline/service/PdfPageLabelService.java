package com.ririv.quickoutline.service;

import com.itextpdf.kernel.pdf.PageLabelNumberingStyle;
import com.ririv.quickoutline.pdfProcess.PageLabel;
import com.ririv.quickoutline.pdfProcess.PageLabelSetter;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextPageLabelSetter;

import java.io.IOException;
import java.util.List;

public class PdfPageLabelService {
    private final PageLabelSetter<PageLabelNumberingStyle> pageLabelSetter = new ItextPageLabelSetter();

    public void setPageLabels(String srcFilePath, String destFilePath, List<PageLabel> labelList) throws IOException{
        pageLabelSetter.setPageLabels(srcFilePath, destFilePath, labelList);
    }

    public String[] getPageLabels(String string) throws IOException {
        return pageLabelSetter.getPageLabels(string);
    }
}
