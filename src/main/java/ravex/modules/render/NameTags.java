package ravex.modules.render;

import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.MultiSelectParameter;

public class NameTags extends Module {
    public final BooleanParameter armor = new BooleanParameter("Armor", true);
    public final BooleanParameter handItems = new BooleanParameter("HandItems", true);
    public final NumberParameter size = new NumberParameter("Size", 1.0, 0.5, 2.5, 0.1);
    public final BooleanParameter distScale = new BooleanParameter("DistScale", true);
    public final NumberParameter range = new NumberParameter("Range", 64.0, 5.0, 256.0, 1.0);
    public final BooleanParameter background = new BooleanParameter("Background", true);
    public final ColorParameter backgroundColor = new ColorParameter("BackgroundColor", 0x20000000);
    public final BooleanParameter customFont = new BooleanParameter("CustomFont", false);

    public final MultiSelectParameter entities = new MultiSelectParameter(
        "Entities",
        java.util.List.of("Players", "Monsters"),
        java.util.List.of("Players", "Monsters", "Passives")
    );

    private NameTags() {
        super("NameTags");
        backgroundColor.setVisible(background::getValue);
    }

    public boolean shouldDraw(net.minecraft.world.entity.Entity target) {
        if (!getEnabled()) return false;
        if (!(target instanceof net.minecraft.world.entity.LivingEntity le)) return false;
        if (target instanceof net.minecraft.world.entity.player.Player) {
            return entities.isSelected("Players");
        }
        if (ravex.utility.misc.MobUtility.isHostile(le)) {
            return entities.isSelected("Monsters");
        }
        return entities.isSelected("Passives");
    }

    public static double calculateScale(double distance, double scaleParam, boolean distScale) {
        if (!distScale) {
            return scaleParam;
        }


        double distFactor = 1.0;
        if (distance > 15.0) {
            distFactor = 1.0 + (distance - 15.0) * 0.015;
        } else if (distance < 15.0) {
            distFactor = 1.0 - (15.0 - distance) * 0.02;
        }
        if (distFactor < 0.5) distFactor = 0.5;
        if (distFactor > 1.5) distFactor = 1.5;
        return scaleParam * distFactor;
    }

    public static double[] calculateLayout(
        double distance,
        double scaleParam,
        boolean distScale,
        boolean showArmor,
        boolean showHands,
        boolean hasOwner,
        double tw,
        double ow,
        boolean hasMainHand,
        boolean hasOffHand,
        int armorCount,
        boolean alwaysShowSlots
    ) {
        double currentScale = calculateScale(distance, scaleParam, distScale);
        double is = 18.0;
        double gap = 3.0;
        double padding = 3.0;

        int topItemsCount = 0;
        if (alwaysShowSlots) {
            if (showHands) topItemsCount += 2;
            if (showArmor) topItemsCount += 4;
        } else {
            if (showHands && hasMainHand) topItemsCount++;
            if (showArmor) topItemsCount += armorCount;
            if (showHands && hasOffHand) topItemsCount++;
        }

        double topRowW = 0.0;
        if (topItemsCount > 0) {
            topRowW = topItemsCount * is + (topItemsCount - 1) * gap;
        }

        double bottomRowW = tw;
        double ownerW = hasOwner ? ow : 0.0;
        double totalW = Math.max(bottomRowW, Math.max(topRowW, ownerW));

        double totalH = 0.0;
        if (topItemsCount > 0) {
            totalH += is + gap;
        }
        if (tw > 0) {
            totalH += 9.0;
        }
        if (hasOwner) {
            totalH += gap + 9.0;
        }
        totalH += 2 * padding;

        double bgBottom = -1.5;
        double bgTop = bgBottom - totalH;
        double contentTop = bgTop + padding;

        double topRowY = 0.0;
        if (topItemsCount > 0) {
            topRowY = contentTop;
            contentTop += is + gap;
        }
        double mainRowY = contentTop;
        if (tw > 0) {
            contentTop += 9.0;
        }
        double ownerRowY = 0.0;
        if (hasOwner) {
            ownerRowY = contentTop + gap;
        }

        return new double[]{
            currentScale,
            totalW,
            totalH,
            topRowY,
            mainRowY,
            ownerRowY,
            0.0,
            tw,
            topRowW,
            0.0
        };
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(NameTags.class);
    }

    public static NameTags itz() {
        return ModuleManager.get(NameTags.class);
    }
}
