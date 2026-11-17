import ravex.cmd.cmds.CalcUtility;
import ravex.utility.nativelib.NativeLibraryUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * Equivalence check for the calc expression evaluator.
 * TWO PHASES (the real gate):
 *  Phase A: loading DISABLED -> CalcUtility goes through the Java parser
 *  Phase B: loading ENABLED  -> CalcUtility goes through the Rust core
 *  Every result (ok flag + value) of phase A must equal phase B.
 *
 * Value comparison: exact (Double.compare) first, then a 1e-9 relative
 * tolerance for libm ULP differences in transcendental functions.
 * NaN == NaN counts as equal (Double.compare).
 *
 * Run (after ./gradlew build):
 *   javac -cp build/classes/java/main -d build/check check/CalcPortCheck.java
 *   java -cp build/classes/java/main:build/resources/main:build/check CalcPortCheck
 */
public final class CalcPortCheck {
    static int passed = 0, failed = 0;

    record Rec(boolean ok, String bits, double v) {}

    public static void main(String[] args) {
        String[] cases = {
            // арифметика и приоритеты
            "3*(4+2)/6", "2+2*2", "(2+2)*2", "2^3^2", "100-30-20", "100/5/2",
            "2^10", "2^0.5", "2^-1", "-2^2", "(-2)^2", "5-(-3)", "+7", "--5", "++2",
            "1+2-3+4-5", "2*3+4*5", "(1+2)*(3+4)", "10%3", "-10%3", "10.5%3", "1%0.3",
            "5%2", "7%", "0.1+0.2", "1/3", "10/4", "7.5*2",
            // юникодные операторы
            "2×3", "8÷2", "2×3÷4", "6÷2×3",
            // пробелы/запятые как точки (замена в execute — тут сырой парсер)
            " 7 + 2 ", "3.5 + 1.5", "  ( 1 + 2 ) * 3  ",
            // факториал: переполнение long, усечение, 0!
            "5!", "0!", "1!", "5.9!", "(-3)!", "-3!", "3!", "3+2x", "5!!", "20!", "21!", "170!", "171!",
            "2^2!", "5!^2", "0.999!", "0.1!",
            // числа с точкой/без, экзотика формата
            "3.", ".5", "-.5", "1..2", "0.5", "123456789012345678901", "007", "0",
            // мусор в хвосте — Java терпит
            "3e5", "2pi", "1é2", "(2+3))", "((1+2)*4", "(2+3)x", "1.5.5",
            // константы
            "pi", "e", "pi*2", "e^2", "pi/2", "-pi", "PIE",
            // функции одной переменной
            "sqrt(16)", "sqrt(2)", "sqrt(-1)", "cbrt(27)", "cbrt(-8)",
            "sin(pi/2)", "sin(0)", "cos(0)", "tan(pi/4)", "tan(0)",
            "asin(1)", "acos(-1)", "atan(1)", "atan(0)",
            "sinh(1)", "cosh(0)", "tanh(0)",
            "log(100)", "log(1)", "ln(e)", "ln(1)", "ln(0)",
            "abs(-5)", "abs(5)", "abs(-0)",
            "floor(2.7)", "floor(-2.7)", "ceil(2.1)", "ceil(-2.1)",
            "round(2.5)", "round(-2.5)", "round(2.4)", "round(NaNx)", "round(1e19)",
            "sign(-3)", "sign(3)", "sign(0)", "exp(0)", "exp(1)", "exp(-1)",
            "rad(180)", "deg(pi)", "deg(0)", "rad(0)",
            "1/0", "0/0", "-1/0", "log(0)",
            // min/max и вложенность
            "min(3,7)", "max(2,9)", "min(3 7)", "max(2,9,3)", "min(min(1,2),max(3,4))",
            "max(sqrt(2),1)", "min(-5,5)", "max(0,0)",
            // скобки
            "()", "(", "((((1))))", "(()", "((2+3))",
            // ошибки парсинга
            "", " ", "   ", "..", "...", "x", "+", "*", "ф", "sqrt", "SQRT(4)",
            "sqrt()", "sqrt(2 2)", "sin sin", "foo(1)", "min()", "%2", "^2", "!", ")( ",
        };

        List<Rec> fallback = new ArrayList<>(), nativeOut = new ArrayList<>();

        // ---- ФАЗА A: нативка ЗАПРЕЩЕНА -> чистый Java-fallback ----
        NativeLibraryUtility.deferLoading();
        if (ravex.cmd.cmds.CalcNative.isAvailable()) {
            System.out.println("!!! native loaded despite deferLoading — фаза A невалидна");
            System.exit(1);
        }
        for (String s : cases) {
            Double v = CalcUtility.eval(s);
            fallback.add(new Rec(v != null, v == null ? "null" : Double.toString(v), v == null ? Double.NaN : v));
        }
        System.out.println("фаза A (java-fallback): " + fallback.size() + " выражений");

        // ---- ФАЗА B: нативка ВКЛЮЧЕНА ----
        NativeLibraryUtility.enableLoading();
        boolean nativeOK = ravex.cmd.cmds.CalcNative.isAvailable();
        System.out.println("native available: " + nativeOK);
        if (!nativeOK) { System.out.println("!!! NATIVE NOT AVAILABLE"); System.exit(1); }
        for (String s : cases) {
            Double v = CalcUtility.eval(s);
            nativeOut.add(new Rec(v != null, v == null ? "null" : Double.toString(v), v == null ? Double.NaN : v));
        }

        // ---- сравнение A vs B ----
        for (int i = 0; i < fallback.size(); i++) {
            Rec a = fallback.get(i), b = nativeOut.get(i);
            boolean ok = a.ok() == b.ok();
            if (ok && a.ok()) {
                if (Double.compare(a.v(), b.v()) == 0) {
                    // точное совпадение бит
                } else if (isFinite(a.v()) && isFinite(b.v())) {
                    double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(a.v()), Math.abs(b.v())));
                    ok = Math.abs(a.v() - b.v()) <= tol;
                } else {
                    ok = false; // Infinity/NaN без точного совпадения
                }
            }
            if (ok) passed++;
            else {
                failed++;
                System.out.println("FAIL in=<" + cases[i] + ">");
                System.out.println("   java:   " + (a.ok() ? a.bits() : "(ошибка)"));
                System.out.println("   native: " + (b.ok() ? b.bits() : "(ошибка)"));
            }
        }

        System.out.println();
        System.out.println("РЕЗУЛЬТАТ: " + passed + "/" + (passed + failed) + " PASSED");
        if (failed > 0) System.exit(1);
    }

    static boolean isFinite(double d) {
        return !Double.isNaN(d) && !Double.isInfinite(d);
    }
}
