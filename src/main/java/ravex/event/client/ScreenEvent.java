package ravex.event.client;

import ravex.event.Event;

public class ScreenEvent implements Event {
    public enum ScreenAction { OPEN, CLOSE, SWITCH }
    private final ScreenAction action;
    private final net.minecraft.client.gui.screens.Screen screen;

    public ScreenEvent(ScreenAction action, net.minecraft.client.gui.screens.Screen screen) {
        this.action = action;
        this.screen = screen;
    }

    public ScreenAction getAction() { return action; }
    public net.minecraft.client.gui.screens.Screen getScreen() { return screen; }
}
