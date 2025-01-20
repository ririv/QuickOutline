package com.ririv.quickoutline.service;

import com.ririv.quickoutline.pdfProcess.TocExtractor;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextTocExtractor;

import java.io.IOException;

public class TocService {

   public String extract(String pdfPath){
       try {
              TocExtractor tocExtractor = new ItextTocExtractor(pdfPath);
              return String.join("\n", tocExtractor.extract());
         } catch (IOException e) {
              e.printStackTrace();
              return "";
       }
   }
   public String extract(String pdfPath, int startPageNum, int endPageNum){
         try {
              TocExtractor tocExtractor = new ItextTocExtractor(pdfPath);
              return tocExtractor.extract(startPageNum, endPageNum).toString();
            } catch (IOException e) {
              e.printStackTrace();
              return "";
         }
   }

}
