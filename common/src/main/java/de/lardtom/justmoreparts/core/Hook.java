package de.lardtom.justmoreparts.core;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class Hook<T> {
    private final List<T> listeners = new CopyOnWriteArrayList<>();

    public void register(T listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void fire(Consumer<T> invoker) {
        for (T listener : this.listeners) {
            invoker.accept(listener);
        }
    }

    public boolean isEmpty() {
        return this.listeners.isEmpty();
    }
}
