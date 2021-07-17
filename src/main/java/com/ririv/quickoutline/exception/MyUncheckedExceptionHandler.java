package com.ririv.quickoutline.exception;

public class MyUncheckedExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Runnable after;

    public MyUncheckedExceptionHandler(Runnable after) {

        this.after = after;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        after.run();

    }
}