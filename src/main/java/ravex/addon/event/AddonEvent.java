package ravex.addon.event;

public class AddonEvent {
    private final String name;
    private boolean cancelled = false;

    public AddonEvent(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
