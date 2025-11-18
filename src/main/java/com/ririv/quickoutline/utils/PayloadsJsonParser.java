package com.ririv.quickoutline.utils;

import com.google.gson.Gson;

public class PayloadsJsonParser {

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
        return gson.fromJson(json, MdEditorContentPayloads.class);
    }
}
