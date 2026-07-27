package ravex.event;

import org.jetbrains.annotations.Contract;

public interface Event {
    @Contract(pure = true)
    default boolean isCancellable() { return false; }
    @Contract(pure = true)
    default boolean isCancelled() { return false; }
    default void setCancelled(boolean cancelled) {}
}
