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
            case TOGGLE_MODULE: return ravex.utility.misc.LanguageUtility.t("act_toggle") + data;
            case SEND_CHAT:     return ravex.utility.misc.LanguageUtility.t("act_chat") + (data.length() > 20 ? data.substring(0, 18) + ".." : data);
            case EXECUTE_COMMAND: return ravex.utility.misc.LanguageUtility.t("act_cmd") + (data.length() > 20 ? data.substring(0, 18) + ".." : data);
            case DELAY:         return ravex.utility.misc.LanguageUtility.t("act_delay") + data + "ms";
            default:            return ravex.utility.misc.LanguageUtility.t("act_unknown");
        }
    }
}
