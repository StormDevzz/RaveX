package ravex.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.util.List;

public class Trigger extends Module {
    public static final Trigger INSTANCE = new Trigger();

    public final NumberParameter range = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.1);
    public final NumberParameter cooldown = new NumberParameter("Cooldown", 0.9, 0.0, 1.0, 0.05);
    public final NumberParameter cps = new NumberParameter("CPS", 10, 1, 20, 1);
    public final NumberParameter randomization = new NumberParameter("Randomization", 0.0, 0.0, 5.0, 0.5);
    public final NumberParameter fov = new NumberParameter("FOV", 180, 10, 180, 5);
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter monsters = new BooleanParameter("Monsters", true);
    public final BooleanParameter passives = new BooleanParameter("Passives", false);
    public final BooleanParameter invisibles = new BooleanParameter("Invisibles", true);
    public final BooleanParameter throughWalls = new BooleanParameter("Through Walls", true);
    public final BooleanParameter weaponOnly = new BooleanParameter("Weapon Only", false);
    public final BooleanParameter raytrace = new BooleanParameter("Raytrace", true);
    public final ModeParameter swingMode = new ModeParameter("Swing", "Client",
            List.of("Client", "Server", "Off"));
    public final ModeParameter clickMode = new ModeParameter("Click Mode", "Hold",
            List.of("Hold", "Toggle"));

    private boolean toggled = false;
    private long lastAttackTime = 0;

    private Trigger() {
        super("Trigger", Category.COMBAT);
        addParameter(range);
        addParameter(cooldown);
        addParameter(cps);
        addParameter(randomization);
        addParameter(fov);
        addParameter(players);
        addParameter(monsters);
        addParameter(passives);
        addParameter(invisibles);
        addParameter(throughWalls);
        addParameter(weaponOnly);
        addParameter(raytrace);
        addParameter(swingMode);
        addParameter(clickMode);
    }

    @Override
    protected void onDisable() {
        toggled = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        String clickModeVal = clickMode.getValue();
        boolean shouldAttack;

        if (clickModeVal.equals("Toggle")) {
            if (mc.options.keyAttack.consumeClick()) toggled = !toggled;
            shouldAttack = toggled;
        } else {
            shouldAttack = mc.options.keyAttack.isDown();
        }

        if (!shouldAttack) return;

        if (weaponOnly.getValue()) {
            var held = mc.player.getMainHandItem().getItem();
            if (!isWeapon(held)) return;
        }

        if (!(mc.hitResult instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof LivingEntity target)) return;
        if (!target.isAlive() || target == mc.player) return;
        if (target instanceof net.minecraft.world.entity.decoration.ArmorStand) return;

        double dist = mc.player.distanceTo(target);
        if (dist > range.getValue()) return;

        if (!invisibles.getValue() && target.isInvisible()) return;

        if (!players.getValue() && target instanceof Player) return;
        if (!monsters.getValue() && (target instanceof Monster || target instanceof EnderDragon || target instanceof WitherBoss)) return;
        if (!passives.getValue() && isPassive(target)) return;

        if (!throughWalls.getValue() && !mc.player.hasLineOfSight(target)) return;
        if (raytrace.getValue()) {
            var result = mc.player.pick(20.0, 0.0f, false);
            if (!(result instanceof EntityHitResult entityHit) || entityHit.getEntity() != target) return;
        }

        if (isOnCooldown()) return;
        if (mc.player.getAttackStrengthScale(0.0f) < cooldown.getValue().floatValue()) return;

        float[] angles = calculateAngles(mc, target);
        float diffYaw = net.minecraft.util.Mth.wrapDegrees(angles[0] - mc.player.getYRot());
        float diffPitch = net.minecraft.util.Mth.wrapDegrees(angles[1] - mc.player.getXRot());
        if (Math.abs(diffYaw) > fov.getValue() || Math.abs(diffPitch) > fov.getValue()) return;

        attack(mc, target);
        lastAttackTime = System.currentTimeMillis();
    }

    private boolean isOnCooldown() {
        long interval = 1000L / (long) cps.getValue().doubleValue();
        double r = randomization.getValue();
        if (r > 0.01) {
            interval += (long) ((Math.random() - 0.5) * r * 100.0);
        }
        return System.currentTimeMillis() - lastAttackTime < interval;
    }

    private void attack(Minecraft mc, LivingEntity target) {
        mc.gameMode.attack(mc.player, target);
        String sMode = swingMode.getValue();
        if (sMode.equals("Client")) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        } else if (sMode.equals("Server")) {
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        }
    }

    private float[] calculateAngles(Minecraft mc, LivingEntity target) {
        var eyePos = mc.player.getEyePosition();
        var targetPos = target.position();
        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y + target.getEyeHeight(target.getPose()) * 0.75 - eyePos.y;
        double dz = targetPos.z - eyePos.z;
        double dh = Math.sqrt(dx * dx + dz * dz);
        return new float[]{
            (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0),
            (float) -Math.toDegrees(Math.atan2(dy, dh))
        };
    }

    private boolean isWeapon(net.minecraft.world.item.Item item) {
        return item == Items.WOODEN_SWORD || item == Items.STONE_SWORD ||
               item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD ||
               item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD ||
               item == Items.WOODEN_AXE || item == Items.STONE_AXE ||
               item == Items.IRON_AXE || item == Items.GOLDEN_AXE ||
               item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE ||
               item == Items.MACE;
    }

    private boolean isPassive(LivingEntity e) {
        if (e instanceof Player || e instanceof Monster || e instanceof EnderDragon || e instanceof WitherBoss) return false;
        if (e instanceof net.minecraft.world.entity.animal.Animal) return true;
        if (e instanceof net.minecraft.world.entity.npc.villager.AbstractVillager) return true;
        if (e instanceof net.minecraft.world.entity.ambient.AmbientCreature) return true;
        return false;
    }
}
