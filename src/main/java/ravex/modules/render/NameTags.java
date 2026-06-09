package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;

public class NameTags extends Module {
    public static final NameTags INSTANCE = new NameTags();

    public final BooleanParameter armor = new BooleanParameter("Armor", true);
    public final BooleanParameter handItems = new BooleanParameter("Hand Items", true);
    public final BooleanParameter distanceScaling = new BooleanParameter("Distance Scaling", true);
    public final NumberParameter scale = new NumberParameter("ScaleMultiplier", 1.0, 0.5, 3.0, 0.1);
    public final BooleanParameter background = new BooleanParameter("Background", true);
    public final ColorParameter backgroundColor = new ColorParameter("Background Color", 0xBB000000);
    public final BooleanParameter topLine = new BooleanParameter("Top Line", false);
    public final ColorParameter topLineColor = new ColorParameter("Top Line Color", 0xFFFF5555);

    private static boolean nativeAvailable = false;

    static {
        try {
            System.loadLibrary("ravex_nametags");
            nativeAvailable = true;
        } catch (UnsatisfiedLinkError e) {
            try {
                String libName = System.getProperty("os.name").toLowerCase().contains("win")
                        ? "ravex_nametags.dll" : "libravex_nametags.so";
                java.io.InputStream is = NameTags.class.getResourceAsStream(
                        "/assets/ravex/natives/" + libName);
                if (is != null) {
                    java.nio.file.Path tmp = java.nio.file.Files.createTempFile("ravex_nt", "");
                    java.nio.file.Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.load(tmp.toAbsolutePath().toString());
                    tmp.toFile().deleteOnExit();
                    nativeAvailable = true;
                }
            } catch (Throwable ignored) {}
        }
    }

    private NameTags() {
        super("NameTags", Category.RENDER);
        addParameter(armor);
        addParameter(handItems);
        addParameter(distanceScaling);
        addParameter(scale);
        addParameter(background);
        addParameter(backgroundColor);
        addParameter(topLine);
        addParameter(topLineColor);

        // Visibility triggers
        backgroundColor.setVisible(background::getValue);
        topLine.setVisible(background::getValue);
        topLineColor.setVisible(() -> background.getValue() && topLine.getValue());
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }

    public static native double[] nativeCalculateLayout(
        double distance,
        double scaleParam,
        boolean distanceScaling,
        boolean showArmor,
        boolean showHands,
        boolean hasOwner,
        double tw,
        double ow,
        boolean hasMainHand,
        boolean hasOffHand,
        int armorCount
    );

    // Java fallback method if native isn't available
    public static double[] javaFallbackCalculate(
        double distance,
        double scaleParam,
        boolean distanceScaling,
        boolean showArmor,
        boolean showHands,
        boolean hasOwner,
        double tw,
        double ow,
        boolean hasMainHand,
        boolean hasOffHand,
        int armorCount
    ) {
        double currentScale = scaleParam;
        if (distanceScaling) {
            currentScale = scaleParam * (distance * 0.15);
            if (currentScale < 0.5) currentScale = 0.5;
            if (currentScale > 3.0) currentScale = 3.0;
        }

        double is = 16.0;
        double gap = 2.0;
        double padding = 3.0;

        double armorW = 0.0;
        boolean hasArmor = showArmor && (armorCount > 0);
        if (hasArmor) {
            armorW = armorCount * is + (armorCount - 1) * gap;
        }

        double mainRowW = tw;
        if (showHands && hasMainHand) mainRowW += is + gap;
        if (showHands && hasOffHand) mainRowW += gap + is;

        double ownerW = hasOwner ? ow : 0.0;

        double totalW = Math.max(mainRowW, Math.max(armorW, ownerW));

        double mainRowH = 9.0;
        double textYOff = 0.0;
        if ((showHands && hasMainHand) || (showHands && hasOffHand)) {
            mainRowH = is;
            textYOff = (is - 9.0) / 2.0;
        }

        double totalH = 0.0;
        if (hasArmor) {
            totalH += is + gap;
        }
        boolean hasMainRow = (tw > 0);
        if (hasMainRow) {
            totalH += mainRowH;
        }
        if (hasOwner) {
            if (hasMainRow || hasArmor) {
                totalH += gap + 9.0;
            } else {
                totalH += 9.0;
            }
        }
        totalH += 2 * padding;

        double bgBottom = -2.0;
        double bgTop = bgBottom - totalH;
        double contentTop = bgTop + padding;

        double armorRowY = 0.0;
        if (hasArmor) {
            armorRowY = contentTop;
            contentTop += is + gap;
        }

        double mainRowY = 0.0;
        if (hasMainRow) {
            mainRowY = contentTop;
            contentTop += mainRowH;
        }

        double ownerRowY = 0.0;
        if (hasOwner) {
            if (hasMainRow || hasArmor) {
                ownerRowY = contentTop + gap;
            } else {
                ownerRowY = contentTop;
            }
        }

        return new double[]{
            currentScale,
            totalW,
            totalH,
            armorRowY,
            mainRowY,
            ownerRowY,
            textYOff,
            mainRowW,
            armorW,
            0.0
        };
    }
}
