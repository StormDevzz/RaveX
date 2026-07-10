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
    public final NumberParameter range = new NumberParameter("Range", 64.0, 5.0, 256.0, 1.0);
    public final BooleanParameter background = new BooleanParameter("Background", true);
    public final ColorParameter backgroundColor = new ColorParameter("Background Color", 0xBB000000);
    public final BooleanParameter topLine = new BooleanParameter("Top Line", false);
    public final ColorParameter topLineColor = new ColorParameter("Top Line Color", 0xFFFF5555);
    public final BooleanParameter customFont = new BooleanParameter("Custom Font", false);

    private static boolean nativeAvailable = false;

    static {
        try {
            nativeAvailable = ravex.utility.misc.NativeLoader.loadLibrary("ravex_nametags");
        } catch (UnsatisfiedLinkError e) {

        }
    }

    private NameTags() {
        super("NameTags", Category.RENDER);
        addParameter(armor);
        addParameter(handItems);
        addParameter(distanceScaling);
        addParameter(scale);
        addParameter(range);
        addParameter(background);
        addParameter(backgroundColor);
        addParameter(topLine);
        addParameter(topLineColor);
        addParameter(customFont);


        backgroundColor.setVisible(background::getValue);
        topLine.setVisible(background::getValue);
        topLineColor.setVisible(() -> background.getValue() && topLine.getValue());
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }

        public static double nativeGetDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dX = x1 - x2;
        double dY = y1 - y2;
        double dZ = z1 - z2;
        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static boolean nativeIsWithinRange(double distance, double range) {
        return distance <= range;
    }

    public static double nativeCalculateScale(double distance, double scaleParam, boolean distanceScaling) {
        if (!distanceScaling) {
            return scaleParam;
        }


        double scale = distance * (scaleParam / 10.0);
        return Math.max(scale, scaleParam);
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
        double currentScale;
        if (isNativeAvailable()) {
            currentScale = nativeCalculateScale(distance, scaleParam, distanceScaling);
        } else {
            currentScale = scaleParam;
            if (distanceScaling) {
                currentScale = scaleParam * (distance * 0.15);
                if (currentScale < 0.5) currentScale = 0.5;
                if (currentScale > 3.0) currentScale = 3.0;
            }
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
