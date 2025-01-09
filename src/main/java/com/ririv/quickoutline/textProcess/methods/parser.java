package com.ririv.quickoutline.textProcess.methods;

import com.ririv.quickoutline.model.Bookmark;

import java.util.ArrayList;
import java.util.List;

public interface parser {

    // linearBookmarkList 传递是为了可以使该行根据上文判断该行level
    Bookmark parseLine(int offset, String line, List<Bookmark> linearBookmarkList);

    default List<Bookmark> parse(List<String> text, int offset){
        List<Bookmark> linearBookmarkList = new ArrayList<>();
        for (String line : text) {
            var current = parseLine(offset, line, linearBookmarkList);
            linearBookmarkList.add(current);
        }
        return linearBookmarkList;
    }
}
