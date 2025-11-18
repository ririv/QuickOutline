package com.ririv.quickoutline.utils;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayloadsJsonParser {

    private static final Logger log = LoggerFactory.getLogger(PayloadsJsonParser.class);

    public record MdEditorContentPayloads(String html, String styles){

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MdEditorContentPayloads(String html1, String styles1))) {
                return false;
            }
            return this.html.equals(html1) && this.styles.equals(styles1);
        }
    }

    public static MdEditorContentPayloads parseJson(String json) {
        Gson gson = new Gson();
        MdEditorContentPayloads payloads = gson.fromJson(json, MdEditorContentPayloads.class);
        if (payloads.html == null ) {
            log.error("html is null");
        }
        if (payloads.styles == null ) {
            log.error("styles is null");
        }
        return payloads;
    }
}
