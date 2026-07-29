package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.mcwrapper.MinecraftWrapper;
public class SayCmd extends Cmd {
    public SayCmd() {
        super("say", "Send a raw chat message");
    }
    @Override
    public void execute(String[] args) {
        if (args.length < 2) { CmdReg.print("§c[RaveX] Usage: .say <message>"); return; }
        String msg = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        var mc = MinecraftWrapper.getInstance();
        if (mc.player != null) mc.player.connection.sendChat(msg);
    }
}
