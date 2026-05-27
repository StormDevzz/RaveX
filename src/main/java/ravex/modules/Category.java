package ravex.modules;

public enum Category {
    COMBAT("Combat"),
    RENDER("Render"),
    PLAYER("Player"),
    MOVEMENT("Movement"),
    MISC("Misc"),
    WORLD("World");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
