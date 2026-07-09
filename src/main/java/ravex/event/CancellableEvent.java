package ravex.event;

public class CancellableEvent implements Event {
    private boolean cancelled;

    @Override
    public boolean isCancellable() { return true; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
