package com.ririv.quickoutline.exception;

public class NoOutlineException extends RuntimeException  {
    public NoOutlineException(){
        super("The doc has no outline");
    }
}
