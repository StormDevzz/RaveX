package ravex.manager;

import ravex.cmd.core.CmdReg;

public class CmdManager {
    public static final CmdManager INSTANCE = new CmdManager();

    private CmdManager() {}

    public boolean processCommand(String message) {
        return CmdReg.INSTANCE.process(message);
    }
}
