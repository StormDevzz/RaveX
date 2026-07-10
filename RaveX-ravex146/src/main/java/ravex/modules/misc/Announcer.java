package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.ModeParameter;
import java.util.List;

public class Announcer extends Module {
    public static final Announcer INSTANCE = new Announcer();

    public final NumberParameter interval = new NumberParameter("Interval", 10, 10, 300, 10);
    public final BooleanParameter announceWalk = new BooleanParameter("Walk", true);
    public final BooleanParameter announceEat = new BooleanParameter("Eat", true);
    public final BooleanParameter announceHit = new BooleanParameter("Hit", true);
    public final ModeParameter mode = new ModeParameter("Mode", "Periodic", List.of("Periodic", "Milestone"));

    private double lastX, lastZ;
    private double blocksWalked;
    private int foodEaten;
    private int hitsDealt;
    private int tickCounter;
    private int lastFoodLevel;

    private Announcer() {
        super("Announcer", Category.MISC);
        addParameter(interval);
        addParameter(announceWalk);
        addParameter(announceEat);
        addParameter(announceHit);
        addParameter(mode);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            lastX = mc.player.getX();
            lastZ = mc.player.getZ();
            lastFoodLevel = mc.player.getFoodData().getFoodLevel();
        }
        blocksWalked = 0; foodEaten = 0; hitsDealt = 0; tickCounter = 0;
    }

    public void onHit() {
        if (getEnabled() && announceHit.getValue()) {
            hitsDealt++;
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || p.connection == null) return;

        tickCounter++;

        if (announceWalk.getValue()) {
            double dx = p.getX() - lastX;
            double dz = p.getZ() - lastZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.1) blocksWalked += dist;
            lastX = p.getX(); lastZ = p.getZ();
        }

        if (announceEat.getValue()) {
            int cur = p.getFoodData().getFoodLevel();
            if (cur > lastFoodLevel) foodEaten++;
            lastFoodLevel = cur;
        }

        String modeStr = mode.getValue();

        if (modeStr.equals("Periodic")) {
            int intervalTicks = interval.getValue().intValue() * 20;
            if (tickCounter >= intervalTicks) {
                announce();
                tickCounter = 0;
            }
        } else {
            checkMilestone(100);  checkMilestone(500);
            checkMilestone(1000); checkMilestone(2500);
            checkMilestone(5000); checkMilestone(10000);

            if (foodEaten == 10 || foodEaten == 25 || foodEaten == 50 || foodEaten == 100) {
                p.connection.sendChat("I just ate " + foodEaten + " times, damn I'm hungry af");
                foodEaten = 0;
            }
            if (hitsDealt == 100 || hitsDealt == 500 || hitsDealt == 1000 || hitsDealt == 5000) {
                p.connection.sendChat("I dealt " + hitsDealt + " hits, stop moving!");
                hitsDealt = 0;
            }
        }
    }

    private void checkMilestone(int target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        if (blocksWalked >= target && blocksWalked - 50 < target) {
            mc.player.connection.sendChat("I walked " + target + " blocks already");
            blocksWalked = 0;
        }
    }

    private void announce() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        if (blocksWalked < 0.5 && foodEaten == 0 && hitsDealt == 0) return;

        StringBuilder sb = new StringBuilder("[Announcer] ");
        boolean added = false;
        if (announceWalk.getValue() && blocksWalked >= 1.0) {
            sb.append("Walked ").append(String.format("%.0f", blocksWalked)).append("b. ");
            added = true;
        }
        if (announceEat.getValue() && foodEaten > 0) {
            sb.append("Ate ").append(foodEaten).append("x. ");
            added = true;
        }
        if (announceHit.getValue() && hitsDealt > 0) {
            sb.append("Hit ").append(hitsDealt).append("x. ");
            added = true;
        }

        if (added) {
            mc.player.connection.sendChat(sb.toString());
        }

        blocksWalked = 0;
        foodEaten = 0;
        hitsDealt = 0;
    }
}
