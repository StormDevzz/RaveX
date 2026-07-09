package ravex.event.client;

import ravex.event.Event;

public class SoundEvent implements Event {
    public enum Type { ENABLE, DISABLE, SETTINGS_OPEN, SETTINGS_CLOSE, GUI_OPEN, GUI_CLOSE, FAILURE }

    private final Type type;
    private final float volume;

    public SoundEvent(Type type) { this(type, 1.0f); }
    public SoundEvent(Type type, float volume) { this.type = type; this.volume = volume; }

    public Type getType() { return type; }
    public float getVolume() { return volume; }
}
