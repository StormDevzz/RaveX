package ravex.event;

public class EventBusHolder {
    private static final EventBus INSTANCE = new EventBus();

    public static EventBus get() {
        return INSTANCE;
    }

    private EventBusHolder() {}
}
