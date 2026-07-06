package ravex.cmd.core;
public abstract class Cmd {
    private final String name;
    private final String description;
    private final String[] aliases;

    public Cmd(String name, String description, String... aliases) {
        this.name = name;
        this.description = description;
        this.aliases = aliases;
    }

    public abstract void execute(String[] args);

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String[] getAliases() { return aliases; }
}
