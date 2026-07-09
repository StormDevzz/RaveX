package ravex.event;

public interface Event {
    default boolean isCancellable() { return false; }
    default boolean isCancelled() { return false; }
    default void setCancelled(boolean cancelled) {}
}
