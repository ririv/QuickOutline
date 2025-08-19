package com.ririv.quickoutline.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AppEventBus {

    private static final AppEventBus INSTANCE = new AppEventBus();
    private final Map<Class<?>, List<Consumer<?>>> subscribers = new HashMap<>();

    private AppEventBus() {}

    public static AppEventBus getInstance() {
        return INSTANCE;
    }

    public <T> void subscribe(Class<T> eventType, Consumer<T> subscriber) {
        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(subscriber);
    }

    public <T> void unsubscribe(Class<T> eventType, Consumer<T> subscriber) {
        List<Consumer<?>> eventSubscribers = subscribers.get(eventType);
        if (eventSubscribers != null) {
            eventSubscribers.remove(subscriber);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        List<Consumer<?>> eventSubscribers = subscribers.get(event.getClass());
        if (eventSubscribers != null) {
            for (Consumer<?> subscriber : new ArrayList<>(eventSubscribers)) {
                ((Consumer<T>) subscriber).accept(event);
            }
        }
    }
}
