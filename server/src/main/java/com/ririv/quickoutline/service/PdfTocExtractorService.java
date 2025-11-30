package com.ririv.quickoutline.service;

import com.ririv.quickoutline.pdfProcess.TocExtractor;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextTocExtractor;

public class PdfTocExtractorService {

   public String extract(String pdfPath){
       try {
           TocExtractor tocExtractor = new ItextTocExtractor(pdfPath);
           return String.join("\n", tocExtractor.extract());
       } catch (Exception e) { // Catch broader exceptions during processing
           e.printStackTrace();
           return "";
       }
   }

}
