package com.ririv.quickoutline.textProcess.methods;

import com.ririv.quickoutline.model.Bookmark;

import java.util.List;

public interface LineProcessor {

    // linearBookmarkList 传递是为了可以使该行根据上文判断该行level
    Bookmark processLine(int offset, String line, List<Bookmark> linearBookmarkList);
}
