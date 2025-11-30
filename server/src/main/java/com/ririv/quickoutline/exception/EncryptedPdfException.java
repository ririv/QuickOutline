package com.ririv.quickoutline.exception;

public class EncryptedPdfException extends RuntimeException{
    public EncryptedPdfException(){
        super("The doc is encrypted");
    }
}
