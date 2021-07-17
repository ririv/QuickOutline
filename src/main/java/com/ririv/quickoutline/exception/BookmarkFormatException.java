package com.ririv.quickoutline.exception;

public class BookmarkFormatException extends RuntimeException {
    int index;

    public BookmarkFormatException(String message,int index) {
        super(message);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
