package ravex.cmd.cmds;
import net.minecraft.client.Minecraft;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
public class SayCmd extends Cmd {
    public SayCmd() {
        super("say", "Send a raw chat message");
    }
    @Override
    public void execute(String[] args) {
        if (args.length < 2) { CmdReg.print("§c[RaveX] Usage: .say <message>"); return; }
        String msg = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.connection.sendChat(msg);
    }
}
