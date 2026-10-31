package ravex.cmd.cmds;
import ravex.utility.network.NetworkUtility;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.manager.ModuleManager;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.client.Commands;
public class SayCmd extends Cmd {
    public SayCmd() {
        super("say", "Send a raw chat message");
    }
    @Override
    public void execute(String[] args) {
        String p = ModuleManager.get(Commands.class).prefix;
        if (args.length < 2) { CmdReg.print("§9[§bRaveX§9] §cUsage: " + p + "say <message>"); return; }
        String msg = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() != null) NetworkUtility.sendChat(msg);
    }
}
