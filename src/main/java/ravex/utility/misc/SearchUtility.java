package ravex.utility.misc;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SearchUtility {
    private static final String EN_KEYS = "`qwertyuiop[]asdfghjkl;'zxcvbnm,./";
    private static final String RU_KEYS = "ёйцукенгшщзхъфывапролджэячсмитьбю.";
    private static final String[] TRANSLIT_FROM = {
        "а","б","в","г","д","е","ё","ж","з","и","й","к","л","м","н","о","п","р","с","т","у","ф","х","ц","ч","ш","щ","ъ","ы","ь","э","ю","я",
        "і","ї","є","ґ","ў"
    };
    private static final String[] TRANSLIT_TO = {
        "a","b","v","g","d","e","e","zh","z","i","i","k","l","m","n","o","p","r","s","t","u","f","h","c","ch","sh","sh","","y","","e","yu","ya",
        "i","i","ye","g","u"
    };

    static {
        if (EN_KEYS.length() != RU_KEYS.length()) {
            throw new IllegalStateException("layout map size mismatch: en=" + EN_KEYS.length() + " ru=" + RU_KEYS.length());
        }
    }

    private SearchUtility() {
    }

    public static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    public static String stripDiacritics(String s) {
        if (s == null || s.isEmpty()) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.replace("ß", "ss").replace("æ", "ae").replace("Æ", "ae")
            .replace("ø", "o").replace("Ø", "o").replace("å", "a").replace("Å", "a")
            .replace("đ", "d").replace("ł", "l").replace("ı", "i")
            .replace("ş", "s").replace("ț", "t");
    }

    public static String mapKeys(String s, boolean ruToEn) {
        if (s == null || s.isEmpty()) return "";
        String from = ruToEn ? RU_KEYS : EN_KEYS;
        String to = ruToEn ? EN_KEYS : RU_KEYS;
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = Character.toLowerCase(s.charAt(i));
            int idx = from.indexOf(c);
            out.append(idx >= 0 ? to.charAt(idx) : c);
        }
        return out.toString();
    }

    public static String translit(String s) {
        if (s == null || s.isEmpty()) return "";
        String lower = lower(s);
        StringBuilder out = new StringBuilder(lower.length());
        int i = 0;
        while (i < lower.length()) {
            boolean hit = false;
            for (int j = 0; j < TRANSLIT_FROM.length; j++) {
                if (lower.startsWith(TRANSLIT_FROM[j], i)) {
                    out.append(TRANSLIT_TO[j]);
                    i += TRANSLIT_FROM[j].length();
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                out.append(lower.charAt(i));
                i++;
            }
        }
        return out.toString();
    }

    public static String swapShaScha(String s) {
        if (s == null || s.isEmpty()) return s;
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == 'ш') chars[i] = 'щ';
            else if (chars[i] == 'щ') chars[i] = 'ш';
        }
        return new String(chars);
    }

    public static List<String> queryVariants(String query) {
        List<String> variants = new ArrayList<>();
        String q = lower(query);
        if (q.isEmpty()) return variants;
        addVariant(variants, q);
        addVariant(variants, stripDiacritics(q));
        addVariant(variants, mapKeys(q, true));
        addVariant(variants, mapKeys(q, false));
        addVariant(variants, translit(q));
        addVariant(variants, translit(mapKeys(q, true)));
        addVariant(variants, stripDiacritics(mapKeys(q, true)));
        String swapped = swapShaScha(q);
        if (!swapped.equals(q)) {
            addVariant(variants, mapKeys(swapped, true));
            addVariant(variants, translit(mapKeys(swapped, true)));
            addVariant(variants, stripDiacritics(mapKeys(swapped, true)));
        }
        return variants;
    }

    private static void addVariant(List<String> list, String v) {
        String s = lower(v);
        if (!s.isEmpty() && !list.contains(s)) list.add(s);
    }

    public static boolean matches(String name, String query) {
        return findMatch(name, query) != null;
    }

    public static int[] findMatch(String text, String query) {
        if (text == null || query == null || query.isEmpty()) return new int[]{0, 0};
        String lower = lower(text);
        for (String variant : queryVariants(query)) {
            int idx = lower.indexOf(variant);
            if (idx >= 0) return new int[]{idx, variant.length()};
        }
        return null;
    }
}
