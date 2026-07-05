package ravex.modules;
public enum Category {
    COMBAT("Combat"),
    RENDER("Render"),
    PLAYER("Player"),
    MOVEMENT("Movement"),
    MISC("Misc"),
    WORLD("World"),
    CLIENT("Client"),
    HUD("HUD"),
    CUSTOM("Custom");
    private final String displayName;
    Category(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return displayName;
    }
}
