package ravex.macro;

import java.util.ArrayList;
import java.util.List;

public class Macro {
    private String name;
    private int keyBind;
    private List<MacroAction> actions;

    public Macro() {
        this("", -1, new ArrayList<>());
    }

    public Macro(String name, int keyBind, List<MacroAction> actions) {
        this.name = name;
        this.keyBind = keyBind;
        this.actions = actions;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getKeyBind() { return keyBind; }
    public void setKeyBind(int keyBind) { this.keyBind = keyBind; }
    public List<MacroAction> getActions() { return actions; }
    public void setActions(List<MacroAction> actions) { this.actions = actions; }
}
