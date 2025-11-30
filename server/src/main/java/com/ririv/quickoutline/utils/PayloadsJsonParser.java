package com.ririv.quickoutline.utils;

import com.google.gson.Gson;
import com.ririv.quickoutline.model.SectionConfig; // Added import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayloadsJsonParser {

    private static final Logger log = LoggerFactory.getLogger(PayloadsJsonParser.class);

    public record MdEditorContentPayloads(String html, String styles, SectionConfig header, SectionConfig footer){

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MdEditorContentPayloads(String html1, String styles1, SectionConfig header1, SectionConfig footer1))) {
                return false;
            }
            // Simplified equals for brevity, or rely on record's default implementation unless custom logic is needed
            // Actually, standard record equals is usually sufficient.
            // But if we keep this custom one:
            boolean h = (this.header == null && header1 == null) || (this.header != null && this.header.equals(header1));
            boolean f = (this.footer == null && footer1 == null) || (this.footer != null && this.footer.equals(footer1));
            return this.html.equals(html1) && this.styles.equals(styles1) && h && f;
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
