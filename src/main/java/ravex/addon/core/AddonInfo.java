package ravex.addon.core;

public class AddonInfo {
    private final String name;
    private final String description;
    private final String version;
    private final String author;
    private final String mainClass;

    public AddonInfo(String name, String description, String version, String author, String mainClass) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.author = author;
        this.mainClass = mainClass;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public String getMainClass() { return mainClass; }
}
