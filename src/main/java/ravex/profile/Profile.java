package ravex.profile;

import java.util.HashMap;
import java.util.Map;

public class Profile {
    private String name;
    private Map<String, Boolean> moduleStates;
    private Map<String, Map<String, Object>> moduleParameters;
    private Map<String, Integer> moduleKeyBinds;

    public Profile() {
        this("", new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public Profile(String name, Map<String, Boolean> moduleStates, Map<String, Map<String, Object>> moduleParameters, Map<String, Integer> moduleKeyBinds) {
        this.name = name;
        this.moduleStates = moduleStates;
        this.moduleParameters = moduleParameters;
        this.moduleKeyBinds = moduleKeyBinds;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, Boolean> getModuleStates() { return moduleStates; }
    public void setModuleStates(Map<String, Boolean> moduleStates) { this.moduleStates = moduleStates; }
    public Map<String, Map<String, Object>> getModuleParameters() { return moduleParameters; }
    public void setModuleParameters(Map<String, Map<String, Object>> moduleParameters) { this.moduleParameters = moduleParameters; }
    public Map<String, Integer> getModuleKeyBinds() { return moduleKeyBinds; }
    public void setModuleKeyBinds(Map<String, Integer> moduleKeyBinds) { this.moduleKeyBinds = moduleKeyBinds; }
}
