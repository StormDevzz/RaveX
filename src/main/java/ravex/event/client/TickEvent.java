package ravex.event.client;

import ravex.event.Event;

public abstract class TickEvent implements Event {
    public static class Client extends TickEvent {}
}
