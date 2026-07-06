package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
public class TimeCmd extends Cmd {
    public TimeCmd() {
        super("time", "Show current local time");
    }
    @Override
    public void execute(String[] args) {
        java.time.LocalTime time = java.time.LocalTime.now();
        String timeStr = String.format("%02d:%02d:%02d", time.getHour(), time.getMinute(), time.getSecond());
        CmdReg.print("§5[RaveX] §7Local time: §e" + timeStr);
    }
}
