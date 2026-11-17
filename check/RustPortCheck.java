import ravex.utility.misc.SearchNative;
import ravex.utility.misc.SearchUtility;
import ravex.utility.nativelib.NativeLibraryUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * Equivalence check: Rust-native search vs Java fallback in SearchUtility.
 * Recreated 2026-11-16 after the disk migration (original was lost with /tmp).
 *
 * TWO PHASES (the real gate):
 *  Phase A: loading DISABLED -> SearchUtility goes through javaBuildVariants/java*
 *  Phase B: loading ENABLED  -> SearchUtility goes through SearchNative (Rust core)
 *  Every observable result of phase A must equal phase B.
 *
 * Run (after ./gradlew build):
 *   javac -cp build/classes/java/main -d build/check check/RustPortCheck.java
 *   java -cp build/classes/java/main:build/resources/main:build/check RustPortCheck
 */
public final class RustPortCheck {
    static int passed = 0, failed = 0;

    record Rec(String fn, String in, String out) {}

    public static void main(String[] args) {
        String[] cases = {
            // базовые смешанные
            "Hello World", "ПРИВЕТ МИР", "привет мир", "Привет Мир", "MiXeD CaSe ТеКсТ",
            "", " ", "   ", "\t\n", "a", "Z", "я", "Я", "ё", "Ё", "й", "Й",
            // цифры/символы/юникод
            "1234567890", "!@#$%^&*()", "тест-тест_тест", "C:\\Users\\тест", "test.zip",
            "привет123", "123привет", "ωΣΨ", "日本語", "İstanbul", "ß STRASSE", "ÇÉÀÜÑ",
            "café", "naïve", "résumé", "Zürich", "São Paulo", "Москва́", "deja vu",
            // keyboard layout (набрано не в той раскладке)
            "ghbdtn", "ghbdtnvbh", "vjz", "uktq", "рщыу", "Rfnz", "Гб", "[eq",
            "yfcnz", "ytcrj", "hjccbz", "vjle", "руру", "qwerty", "йцукен",
            "поаигк", "Виктор", "нщг ыкуьз", "ещезг", "вщыекш", "kufts",
            // ша/ща и шипящие
            "шапка", "щапка", "ща", "шя", "шью", "щью", "защита", "зашита",
            "пуещ", "ещ", "ш", "щ", "ШЩ", "ШЩ ", "ШшЩщ", " borsh ", "борщ",
            // транслит целиком
            "privet", "Привет", "dosvidaniya", "До свидания", "Zhukovsky", "Жуковский",
            "chay", "чай", "schastye", "счастье", "shchi", "щи", "yolka", "ёлка",
            "tsar", "царь", "moy", "мой", "pod", "под", "elektr", "электр",
            // длинные и реальные строки
            "В лесу родилась ёлочка, в лесу она росла",
            "The quick brown fox jumps over the lazy dog",
            "Съешь же ещё этих мягких французских булок, да выпей чаю",
            "RaveX utility client for Minecraft 1.21.11 with Rust core",
            "Сортировка: КареплавающаяУтилита Utility 2026",
        };

        List<Rec> fallback = new ArrayList<>(), nativeOut = new ArrayList<>();

        // ---- ФАЗА A: нативка ЗАПРЕЩЕНА -> чистый Java-fallback ----
        NativeLibraryUtility.deferLoading();
        if (SearchNative.isAvailable()) {
            System.out.println("!!! native loaded despite deferLoading — фаза A невалидна");
            System.exit(1);
        }
        for (String s : cases) {
            fallback.add(new Rec("lower", s, SearchUtility.lower(s)));
            fallback.add(new Rec("stripDiacritics", s, SearchUtility.stripDiacritics(s)));
            fallback.add(new Rec("mapKeys(true)", s, SearchUtility.mapKeys(s, true)));
            fallback.add(new Rec("mapKeys(false)", s, SearchUtility.mapKeys(s, false)));
            fallback.add(new Rec("translit", s, SearchUtility.translit(s)));
            fallback.add(new Rec("swapShaScha", s, SearchUtility.swapShaScha(s)));
            fallback.add(new Rec("queryVariants", s, String.join("\n", SearchUtility.queryVariants(s))));
        }
        System.out.println("фаза A (java-fallback): " + fallback.size() + " записей");

        // ---- ФАЗА B: нативка ВКЛЮЧЕНА ----
        NativeLibraryUtility.enableLoading();
        boolean nativeOK = SearchNative.isAvailable();
        System.out.println("native available: " + nativeOK);
        if (!nativeOK) { System.out.println("!!! NATIVE NOT AVAILABLE"); System.exit(1); }
        for (String s : cases) {
            nativeOut.add(new Rec("lower", s, SearchUtility.lower(s)));
            nativeOut.add(new Rec("stripDiacritics", s, SearchUtility.stripDiacritics(s)));
            nativeOut.add(new Rec("mapKeys(true)", s, SearchUtility.mapKeys(s, true)));
            nativeOut.add(new Rec("mapKeys(false)", s, SearchUtility.mapKeys(s, false)));
            nativeOut.add(new Rec("translit", s, SearchUtility.translit(s)));
            nativeOut.add(new Rec("swapShaScha", s, SearchUtility.swapShaScha(s)));
            nativeOut.add(new Rec("queryVariants", s, String.join("\n", SearchUtility.queryVariants(s))));
        }

        // ---- сравнение A vs B ----
        for (int i = 0; i < fallback.size(); i++) {
            Rec a = fallback.get(i), b = nativeOut.get(i);
            boolean ok = a.out() == null ? b.out() == null : a.out().equals(b.out());
            if (ok) passed++;
            else {
                failed++;
                System.out.println("FAIL [" + a.fn() + "] in=<" + a.in() + ">");
                System.out.println("   java:   <" + a.out() + ">");
                System.out.println("   native: <" + b.out() + ">");
            }
        }

        System.out.println();
        System.out.println("РЕЗУЛЬТАТ: " + passed + "/" + (passed + failed) + " PASSED");
        if (failed > 0) System.exit(1);
    }
}
