package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.manager.ModuleManager;
import ravex.modules.client.Commands;
public class CalcCmd extends Cmd {
    public CalcCmd() {
        super("calc", "Calculate a math expression");
    }
    @Override
    public void execute(String[] args) {
        String p = ModuleManager.get(Commands.class).prefix;
        if (args.length < 2) { CmdReg.print("§9[§bRaveX§9] §cUsage: " + p + "calc <expression>   e.g. " + p + "calc 3*(4+2)/6"); return; }
        String expr = String.join("", java.util.Arrays.copyOfRange(args, 1, args.length)).replace(",", ".");
        try {
            Double boxed = CalcUtility.eval(expr);
            if (boxed == null) throw new NumberFormatException("invalid expression");
            double result = boxed;
            String resultStr = (result == Math.floor(result) && !Double.isInfinite(result))
                ? String.valueOf((long) result)
                : String.format("%.6f", result).replaceAll("0+$", "").replaceAll("\\.$", "");
            CmdReg.print("§9[§bRaveX§9] §e" + expr + " §7= §a" + resultStr);
        } catch (Exception e) {
            CmdReg.print("§9[§bRaveX§9] §cInvalid expression: §e" + expr);
        }
    }
}
