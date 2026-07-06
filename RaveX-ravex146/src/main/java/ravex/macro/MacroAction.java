package ravex.macro;

public class MacroAction {
    public enum Type {
        TOGGLE_MODULE,
        SEND_CHAT,
        EXECUTE_COMMAND,
        DELAY
    }

    private Type type;
    private String data;

    public MacroAction() {}

    public MacroAction(Type type, String data) {
        this.type = type;
        this.data = data;
    }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getDisplayString() {
        switch (type) {
            case TOGGLE_MODULE: return "Toggle: " + data;
            case SEND_CHAT:     return "Chat: " + (data.length() > 20 ? data.substring(0, 18) + ".." : data);
            case EXECUTE_COMMAND: return "Cmd: " + (data.length() > 20 ? data.substring(0, 18) + ".." : data);
            case DELAY:         return "Delay: " + data + "ms";
            default:            return "Unknown";
        }
    }
}
