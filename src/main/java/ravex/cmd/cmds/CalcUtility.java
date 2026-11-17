package ravex.cmd.cmds;

/**
 * Math expression evaluator: native-first (Rust core), with the Java
 * parser kept verbatim from the old CalcCmd as the fallback.
 * The equivalence gate is check/CalcPortCheck.java (two-phase:
 * java-fallback vs native).
 */
public final class CalcUtility {
    private CalcUtility() {
    }

    /**
     * @return the result (may be NaN/Infinity), or null if the expression
     *         is invalid (parse error).
     */
    public static Double eval(String expr) {
        if (expr == null) return null;
        double[] out = new double[1];
        if (CalcNative.isAvailable()) {
            try {
                if (CalcNative.nativeEval(expr, out)) return out[0];
                return null;
            } catch (Throwable ignored) {
                // fall back to the Java implementation below
            }
        }
        return javaEval(expr);
    }

    private static Double javaEval(String expr) {
        try {
            return evalExpr(expr, new int[]{0});
        } catch (Throwable t) {
            return null;
        }
    }

    // ---- verbatim parser from CalcCmd (fallback) ----

    private static double evalExpr(String expr, int[] pos) {
        double left = evalTerm(expr, pos);
        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op == '+') { pos[0]++; left += evalTerm(expr, pos); }
            else if (op == '-') { pos[0]++; left -= evalTerm(expr, pos); }
            else break;
        }
        return left;
    }

    private static double evalTerm(String expr, int[] pos) {
        double left = evalPow(expr, pos);
        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op == '*' || op == '\u00D7') { pos[0]++; left *= evalPow(expr, pos); }
            else if (op == '/' || op == '\u00F7') { pos[0]++; left /= evalPow(expr, pos); }
            else if (op == '%') { pos[0]++; left %= evalPow(expr, pos); }
            else break;
        }
        return left;
    }

    private static double evalPow(String expr, int[] pos) {
        double base = evalUnary(expr, pos);
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '^') {
            pos[0]++;
            return Math.pow(base, evalPow(expr, pos));
        }
        return base;
    }

    private static double evalUnary(String expr, int[] pos) {
        skipWS(expr, pos);
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '-') { pos[0]++; return -evalUnary(expr, pos); }
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '+') { pos[0]++; return evalUnary(expr, pos); }
        double v = evalAtom(expr, pos);
        skipWS(expr, pos);
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '!') {
            pos[0]++;
            long n = (long) v;
            double f = 1;
            for (long i = 2; i <= n; i++) f *= i;
            return f;
        }
        return v;
    }

    private static double evalAtom(String expr, int[] pos) {
        skipWS(expr, pos);
        if (pos[0] >= expr.length()) throw new RuntimeException("Unexpected end");

        char c = expr.charAt(pos[0]);
        if (c == '(') {
            pos[0]++; double v = evalExpr(expr, pos);
            skipWS(expr, pos);
            if (pos[0] < expr.length() && expr.charAt(pos[0]) == ')') pos[0]++;
            return v;
        }
        if (Character.isLetter(c)) {
            return evalFunc(expr, pos);
        }
        int start = pos[0];
        boolean hasDot = false;
        while (pos[0] < expr.length() && (Character.isDigit(expr.charAt(pos[0])) || (!hasDot && expr.charAt(pos[0]) == '.'))) {
            if (expr.charAt(pos[0]) == '.') hasDot = true;
            pos[0]++;
        }
        if (pos[0] == start) throw new RuntimeException("Expected number");
        return Double.parseDouble(expr.substring(start, pos[0]));
    }

    private static double evalFunc(String expr, int[] pos) {
        int start = pos[0];
        while (pos[0] < expr.length() && Character.isLetter(expr.charAt(pos[0]))) pos[0]++;
        String name = expr.substring(start, pos[0]);
        if (name.equals("pi")) return Math.PI;
        if (name.equals("e"))  return Math.E;
        skipWS(expr, pos);
        if (pos[0] >= expr.length() || expr.charAt(pos[0]) != '(')
            throw new RuntimeException("Expected '(' after function");
        pos[0]++;
        double result;
        switch (name) {
            case "sqrt": result = Math.sqrt(evalExpr(expr, pos)); break;
            case "cbrt": result = Math.cbrt(evalExpr(expr, pos)); break;
            case "sin":  result = Math.sin(evalExpr(expr, pos)); break;
            case "cos":  result = Math.cos(evalExpr(expr, pos)); break;
            case "tan":  result = Math.tan(evalExpr(expr, pos)); break;
            case "asin": result = Math.asin(evalExpr(expr, pos)); break;
            case "acos": result = Math.acos(evalExpr(expr, pos)); break;
            case "atan": result = Math.atan(evalExpr(expr, pos)); break;
            case "sinh": result = Math.sinh(evalExpr(expr, pos)); break;
            case "cosh": result = Math.cosh(evalExpr(expr, pos)); break;
            case "tanh": result = Math.tanh(evalExpr(expr, pos)); break;
            case "log":  result = Math.log10(evalExpr(expr, pos)); break;
            case "ln":   result = Math.log(evalExpr(expr, pos)); break;
            case "abs":  result = Math.abs(evalExpr(expr, pos)); break;
            case "floor": result = Math.floor(evalExpr(expr, pos)); break;
            case "ceil": result = Math.ceil(evalExpr(expr, pos)); break;
            case "round": result = Math.round(evalExpr(expr, pos)); break;
            case "sign": result = Math.signum(evalExpr(expr, pos)); break;
            case "exp":  result = Math.exp(evalExpr(expr, pos)); break;
            case "rad":  result = Math.toRadians(evalExpr(expr, pos)); break;
            case "deg":  result = Math.toDegrees(evalExpr(expr, pos)); break;
            case "min": {
                double a = evalExpr(expr, pos);
                skipWS(expr, pos);
                if (pos[0] < expr.length() && expr.charAt(pos[0]) == ',') pos[0]++;
                else throw new RuntimeException("Expected ',' in min(a,b)");
                result = Math.min(a, evalExpr(expr, pos));
                break;
            }
            case "max": {
                double a = evalExpr(expr, pos);
                skipWS(expr, pos);
                if (pos[0] < expr.length() && expr.charAt(pos[0]) == ',') pos[0]++;
                else throw new RuntimeException("Expected ',' in max(a,b)");
                result = Math.max(a, evalExpr(expr, pos));
                break;
            }
            default: throw new RuntimeException("Unknown function: " + name);
        }
        skipWS(expr, pos);
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == ')') pos[0]++;
        return result;
    }

    private static void skipWS(String expr, int[] pos) {
        while (pos[0] < expr.length() && expr.charAt(pos[0]) == ' ') pos[0]++;
    }
}
