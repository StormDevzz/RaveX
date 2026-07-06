package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
public class CalcCmd extends Cmd {
    public CalcCmd() {
        super("calc", "Calculate a math expression");
    }
    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            CmdReg.print("§c[RaveX] Usage: .calc <expression>   e.g. .calc 3*(4+2)/6");
            return;
        }
        String expr = String.join("", java.util.Arrays.copyOfRange(args, 1, args.length)).replace(",", ".");
        try {
            double result = evalExpr(expr, new int[]{0});
            String resultStr = (result == Math.floor(result) && !Double.isInfinite(result))
                ? String.valueOf((long) result)
                : String.format("%.6f", result).replaceAll("0+$", "").replaceAll("\\.$", "");
            CmdReg.print("§a[RaveX] §e" + expr + " §7= §a" + resultStr);
        } catch (Exception e) {
            CmdReg.print("§c[RaveX] Invalid expression: §e" + expr);
        }
    }
    private double evalExpr(String expr, int[] pos) {
        double left = evalTerm(expr, pos);
        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op == '+') { pos[0]++; left += evalTerm(expr, pos); }
            else if (op == '-') { pos[0]++; left -= evalTerm(expr, pos); }
            else break;
        }
        return left;
    }
    private double evalTerm(String expr, int[] pos) {
        double left = evalPow(expr, pos);
        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op == '*') { pos[0]++; left *= evalPow(expr, pos); }
            else if (op == '/') { pos[0]++; left /= evalPow(expr, pos); }
            else break;
        }
        return left;
    }
    private double evalPow(String expr, int[] pos) {
        double base = evalUnary(expr, pos);
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '^') {
            pos[0]++;
            return Math.pow(base, evalPow(expr, pos));
        }
        return base;
    }
    private double evalUnary(String expr, int[] pos) {
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '-') { pos[0]++; return -evalAtom(expr, pos); }
        return evalAtom(expr, pos);
    }
    private double evalAtom(String expr, int[] pos) {
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '(') {
            pos[0]++; double v = evalExpr(expr, pos);
            if (pos[0] < expr.length() && expr.charAt(pos[0]) == ')') pos[0]++;
            return v;
        }
        int start = pos[0];
        while (pos[0] < expr.length() && (Character.isDigit(expr.charAt(pos[0])) || expr.charAt(pos[0]) == '.')) pos[0]++;
        return Double.parseDouble(expr.substring(start, pos[0]));
    }
}
