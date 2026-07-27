package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.ArrayList;
import java.util.List;
import ravex.modules.Modules;
@Module(name = "NameTags", category = "Render")
public class NameTags {
    @Parameter(name = "Armor")
    public boolean armor = true;
    @Parameter(name = "HandItems")
    public boolean handItems = true;
    @Parameter(name = "Size", min = 0.5, max = 2.5, step = 0.1)
    public double size = 1.0;
    @Parameter(name = "DistScale")
    public boolean distScale = true;
    @Parameter(name = "Range", min = 5.0, max = 256.0, step = 1.0)
    public double range = 64.0;
    @Parameter(name = "Background")
    public boolean background = true;
    @Parameter(name = "BackgroundColor", color = true)
    public int backgroundColor = 0x20000000;
    @Parameter(name = "CustomFont")
    public boolean customFont = false;

    @Parameter(name = "Entities", options = {"Players", "Monsters", "Passives"})
    public List<String> entities = new ArrayList<>(List.of("Players", "Monsters"));

    public boolean shouldDraw(net.minecraft.world.entity.Entity target) {
        if (!Modules.enabled(NameTags.class)) return false;
        if (!(target instanceof net.minecraft.world.entity.LivingEntity le)) return false;
        if (target instanceof net.minecraft.world.entity.player.Player) {
            return entities.contains("Players");
        }
        if (ravex.utility.misc.MobUtility.isHostile(le)) {
            return entities.contains("Monsters");
        }
        return entities.contains("Passives");
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






}