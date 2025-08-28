package com.ririv.quickoutline.event;

import com.google.common.eventbus.EventBus;
import jakarta.inject.Singleton;

/**
 * A Guice-managed singleton wrapper for the Guava EventBus.
 * This allows the rest of the application to use a consistent, injectable event bus
 * while leveraging the power and safety of Guava's implementation.
 */
@Singleton
public class AppEventBus {

    private final EventBus guavaEventBus = new EventBus("QuickOutline-EventBus");

    /**
     * Registers all subscriber methods on the given object.
     * @param object The object whose subscriber methods should be registered.
     */
    public void register(Object object) {
        guavaEventBus.register(object);
    }

    /**
     * Unregisters all subscriber methods on the given object.
     * @param object The object whose subscriber methods should be unregistered.
     */
    public void unregister(Object object) {
        guavaEventBus.unregister(object);
    }

    /**
     * Posts an event to all registered subscribers.
     * @param event The event to post.
     */
    public void post(Object event) {
        guavaEventBus.post(event);
    }
}