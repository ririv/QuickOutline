package com.ririv.quickoutline.event;

import java.util.ArrayList;
import java.util.List;

public class AppEventBus {

    private final List<Object> subscribers = new ArrayList<>();

    public void register(Object object) {
        subscribers.add(object);
    }

    public void unregister(Object object) {
        subscribers.remove(object);
    }

    public void post(Object event) {
        // This is a simplified implementation. A real implementation would use reflection to call the correct methods on the subscribers.
    }
}
