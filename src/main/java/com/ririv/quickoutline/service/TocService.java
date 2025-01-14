package com.ririv.quickoutline.service;

import com.ririv.quickoutline.pdfProcess.TocProcessor;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextTocExtractor;

import java.io.IOException;

public class TocService {

   public String extract(String pdfPath){
       try {
              TocProcessor tocProcessor = new ItextTocExtractor(pdfPath);
              return String.join("\n", tocProcessor.extract());
         } catch (IOException e) {
              e.printStackTrace();
              return "";
       }
   }
   public String extract(String pdfPath, int startPageNum, int endPageNum){
         try {
              TocProcessor tocProcessor = new ItextTocExtractor(pdfPath);
              return tocProcessor.extract(startPageNum, endPageNum).toString();
            } catch (IOException e) {
              e.printStackTrace();
              return "";
         }
   }

}
