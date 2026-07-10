package ravex.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.util.ArrayList;
import java.util.List;

public class Breaker extends Module {
    public static final Breaker INSTANCE = new Breaker();

    public final NumberParameter range = new NumberParameter("Break Range", 4.5, 1.0, 6.0, 0.1);
    public final NumberParameter crystalRange = new NumberParameter("Crystal Range", 5.0, 1.0, 6.0, 0.1);
    public final NumberParameter minDamage = new NumberParameter("Min Damage", 4.0, 1.0, 20.0, 0.5);
    public final NumberParameter maxSelfDmg = new NumberParameter("Max Self Dmg", 8.0, 1.0, 20.0, 0.5);
    public final NumberParameter selfDamageWeight = new NumberParameter("Self Dmg Weight", 1.2, 0.0, 5.0, 0.1);
    public final BooleanParameter antiSuicide = new BooleanParameter("Anti Suicide", true);
    public final NumberParameter antiSuicideMinHp = new NumberParameter("Anti Suicide Min HP", 6.0, 1.0, 20.0, 0.5);
    public final ModeParameter rotate = new ModeParameter("Rotate", "Silent", List.of("Silent", "Normal", "None"));
    public final BooleanParameter syncPacketMine = new BooleanParameter("Sync PacketMine", false) {
        @Override
        public void setValue(Boolean val) {
            if (val) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                boolean packetMineEnabled = ravex.modules.ModuleManager.INSTANCE.getByName("PacketMine").getEnabled();
                if (!packetMineEnabled) {
                    if (mc.player != null) {
                        mc.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§7[§cBreaker§7] §cPlease enable PacketMine module first!"),
                            false
                        );
                    }
                    super.setValue(false);
                    return;
                }
            }
            super.setValue(val);
        }
    };
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3F00FFFF);

    public static float silentYaw = 0;
    public static float silentPitch = 0;
    public static boolean hasSilentRotations = false;

    public static BlockPos currentMiningBlock = null;

    private static boolean nativeAvailable = false;
    static {
        nativeAvailable = ravex.utility.misc.NativeLoader.loadLibrary("ravex_breaker");
    }

    private Breaker() {
        super("Breaker", Category.COMBAT);
        addParameter(range);
        addParameter(crystalRange);
        addParameter(minDamage);
        addParameter(maxSelfDmg);
        addParameter(selfDamageWeight);
        addParameter(antiSuicide);
        addParameter(antiSuicideMinHp);
        addParameter(rotate);
        addParameter(syncPacketMine);
        addParameter(render);
        addParameter(color);
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (currentMiningBlock != null && mc.gameMode != null && !syncPacketMine.getValue()) {
            mc.gameMode.stopDestroyBlock();
        }
        currentMiningBlock = null;
        hasSilentRotations = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            currentMiningBlock = null;
            hasSilentRotations = false;
            return;
        }

        hasSilentRotations = false;

        if (syncPacketMine.getValue()) {
            boolean packetMineEnabled = ravex.modules.ModuleManager.INSTANCE.getByName("PacketMine").getEnabled();
            if (!packetMineEnabled) {
                syncPacketMine.setValue(false);
                mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§7[§cBreaker§7] §cPacketMine was disabled, Sync PacketMine turned off!"),
                    false
                );
            }
        }

        if (!nativeAvailable) return;

        Player target = findTarget(mc);
        if (target == null) {
            if (currentMiningBlock != null) {
                if (!syncPacketMine.getValue()) {
                    mc.gameMode.stopDestroyBlock();
                }
                currentMiningBlock = null;
            }
            return;
        }

        List<BlockPos> solid = collectSolidBlocks(mc, target);
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos tPos = target.blockPosition();
        int tx = tPos.getX();
        int ty = tPos.getY();
        int tz = tPos.getZ();

        for (BlockPos pos : solid) {
            BlockState state = mc.level.getBlockState(pos);
            if (state.getDestroySpeed(mc.level, pos) > 0.0f) {
                int px = pos.getX();
                int py = pos.getY();
                int pz = pos.getZ();

                boolean isTargetCover = false;


                if (px == tx && py == ty - 1 && pz == tz) {
                    isTargetCover = true;
                }

                else if (py == ty) {
                    int dx = Math.abs(px - tx);
                    int dz = Math.abs(pz - tz);
                    if (dx <= 1 && dz <= 1 && (dx + dz > 0)) {
                        isTargetCover = true;
                    }
                }

                else if (py == ty + 1) {
                    int dx = Math.abs(px - tx);
                    int dz = Math.abs(pz - tz);
                    if (dx <= 1 && dz <= 1 && (dx + dz > 0)) {
                        isTargetCover = true;
                    }
                }

                else if (px == tx && py == ty + 2 && pz == tz) {
                    isTargetCover = true;
                }

                if (isTargetCover) {
                    candidates.add(pos);
                }
            }
        }

        if (candidates.isEmpty()) {
            if (currentMiningBlock != null) {
                if (!syncPacketMine.getValue()) {
                    mc.gameMode.stopDestroyBlock();
                }
                currentMiningBlock = null;
            }
            return;
        }

        BlockPos targetPos = null;
        if (currentMiningBlock != null) {
            double dist = Vec3.atCenterOf(currentMiningBlock).distanceTo(mc.player.getEyePosition());
            if (dist <= range.getValue() && candidates.contains(currentMiningBlock)) {
                targetPos = currentMiningBlock;
            }
        }

        if (targetPos == null) {
            double[] solidData = flatten(solid);
            double[] candData = flatten(candidates);

            double[] result = nativeCalculateBreaker(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                mc.player.getHealth(), mc.player.getAbsorptionAmount(), getEntityStats(mc.player),
                target.getX(), target.getY(), target.getZ(),
                target.getHealth(), target.getAbsorptionAmount(), getEntityStats(target),
                solidData,
                candData,
                range.getValue(),
                crystalRange.getValue(),
                minDamage.getValue(),
                maxSelfDmg.getValue(),
                selfDamageWeight.getValue(),
                antiSuicide.getValue(),
                antiSuicideMinHp.getValue()
            );

            if (result == null || result[0] < 0.5) {
                if (currentMiningBlock != null) {
                    if (!syncPacketMine.getValue()) {
                        mc.gameMode.stopDestroyBlock();
                    }
                    currentMiningBlock = null;
                }
                return;
            }

            targetPos = new BlockPos((int) result[1], (int) result[2], (int) result[3]);
        }

        if (!syncPacketMine.getValue()) {
            String rotMode = rotate.getValue();
            if (rotMode.equals("Normal")) {
                rotateTo(mc, Vec3.atCenterOf(targetPos));
            } else if (rotMode.equals("Silent")) {
                silentYaw = calculateYaw(mc, Vec3.atCenterOf(targetPos));
                silentPitch = calculatePitch(mc, Vec3.atCenterOf(targetPos));
                hasSilentRotations = true;
            }
        }

        if (syncPacketMine.getValue()) {
            if (!ravex.modules.exploit.PacketMine.INSTANCE.isTargetBlock(targetPos)) {

                ravex.modules.exploit.PacketMine.miningBlocks.removeIf(m -> !m.done);

                String name = mc.level.getBlockState(targetPos).getBlock().getName().getString();
                long breakMs = ravex.modules.exploit.PacketMine.INSTANCE.calcBreakTime(mc, targetPos);
                ravex.modules.exploit.PacketMine.miningBlocks.add(
                    new ravex.modules.exploit.PacketMine.MiningBlock(targetPos, breakMs, name)
                );
            }
            currentMiningBlock = targetPos;
        } else {
            if (currentMiningBlock == null || !currentMiningBlock.equals(targetPos)) {
                if (currentMiningBlock != null) {
                    mc.gameMode.stopDestroyBlock();
                }
                currentMiningBlock = targetPos;
                mc.gameMode.startDestroyBlock(targetPos, Direction.UP);
            } else {
                mc.gameMode.continueDestroyBlock(targetPos, Direction.UP);
            }
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private Player findTarget(Minecraft mc) {
        Player closest = null;
        double bestDist = Double.MAX_VALUE;
        double maxDist = range.getValue() + 3.0;
        for (Player p : mc.level.players()) {
            if (p == mc.player || p.isDeadOrDying()) continue;
            double dist = mc.player.distanceTo(p);
            if (dist <= maxDist && dist < bestDist) {
                bestDist = dist;
                closest = p;
            }
        }
        return closest;
    }

    private List<BlockPos> collectSolidBlocks(Minecraft mc, Player target) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos tPos = target.blockPosition();
        int r = 2;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = tPos.offset(dx, dy, dz);
                    if (mc.level.isLoaded(pos)) {
                        BlockState state = mc.level.getBlockState(pos);
                        if (!state.isAir() && !state.liquid()) {
                            found.add(pos);
                        }
                    }
                }
            }
        }
        return found;
    }

    private double[] flatten(List<BlockPos> list) {
        double[] arr = new double[list.size() * 3];
        for (int i = 0; i < list.size(); i++) {
            BlockPos p = list.get(i);
            arr[i * 3] = p.getX();
            arr[i * 3 + 1] = p.getY();
            arr[i * 3 + 2] = p.getZ();
        }
        return arr;
    }

    private float calculateYaw(Minecraft mc, Vec3 target) {
        Vec3 eyes = mc.player.getEyePosition();
        double dx = target.x - eyes.x;
        double dz = target.z - eyes.z;
        return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
    }

    private float calculatePitch(Minecraft mc, Vec3 target) {
        Vec3 eyes = mc.player.getEyePosition();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        return (float) -Math.toDegrees(Math.atan2(dy, dist));
    }

    private void rotateTo(Minecraft mc, Vec3 target) {
        Vec3 eyes = mc.player.getEyePosition();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        mc.player.setYRot(targetYaw);
        mc.player.setXRot(targetPitch);
    }

    private double[] getEntityStats(Player player) {
        int protectionEpf = 0;
        int blastProtectionEpf = 0;
        net.minecraft.world.entity.EquipmentSlot[] armorSlots = {
            net.minecraft.world.entity.EquipmentSlot.FEET,
            net.minecraft.world.entity.EquipmentSlot.LEGS,
            net.minecraft.world.entity.EquipmentSlot.CHEST,
            net.minecraft.world.entity.EquipmentSlot.HEAD
        };
        for (net.minecraft.world.entity.EquipmentSlot slot : armorSlots) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty()) continue;
            ItemEnchantments enchants = armor.get(DataComponents.ENCHANTMENTS);
            if (enchants != null) {
                for (var enchantment : enchants.keySet()) {
                    String id = enchantment.getRegisteredName().toLowerCase();
                    int level = enchants.getLevel(enchantment);
                    if (id.contains("blast_protection")) {
                        blastProtectionEpf += level * 2;
                    } else if (id.equals("minecraft:protection") || id.endsWith(":protection")) {
                        protectionEpf += level;
                    }
                }
            }
        }

        int totems = 0;
        if (player.getMainHandItem().getItem() == Items.TOTEM_OF_UNDYING) totems++;
        if (player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) totems++;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() == Items.TOTEM_OF_UNDYING) {
                totems++;
            }
        }

        double[] stats = new double[15];
        stats[0] = player.getArmorValue();
        var attrToughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        stats[1] = attrToughness != null ? attrToughness.getValue() : 0.0;
        stats[2] = blastProtectionEpf;
        stats[3] = protectionEpf;

        var resEffect = player.getEffect(MobEffects.RESISTANCE);
        stats[4] = resEffect != null ? resEffect.getAmplifier() + 1 : 0;

        var weakEffect = player.getEffect(MobEffects.WEAKNESS);
        stats[5] = weakEffect != null ? weakEffect.getAmplifier() + 1 : 0;

        var strEffect = player.getEffect(MobEffects.STRENGTH);
        stats[6] = strEffect != null ? strEffect.getAmplifier() + 1 : 0;

        int idx = 7;
        for (net.minecraft.world.entity.EquipmentSlot slot : armorSlots) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty()) {
                stats[idx++] = 0.0;
            } else if (!armor.isDamageableItem()) {
                stats[idx++] = 100.0;
            } else {
                double dur = (1.0 - (double) armor.getDamageValue() / armor.getMaxDamage()) * 100.0;
                stats[idx++] = dur;
            }
        }

        Vec3 motion = player.getDeltaMovement();
        if (motion != null) {
            stats[11] = motion.x;
            stats[12] = motion.y;
            stats[13] = motion.z;
        } else {
            stats[11] = 0.0;
            stats[12] = 0.0;
            stats[13] = 0.0;
        }
        stats[14] = totems;

        return stats;
    }

    private static native double[] nativeCalculateBreaker(
        double playerX, double playerY, double playerZ,
        double playerHp, double playerAbs, double[] playerStats,
        double targetX, double targetY, double targetZ,
        double targetHp, double targetAbs, double[] targetStats,
        double[] solidBlocksData,
        double[] breakableCandidatesData,
        double breakRange,
        double crystalPlaceRange,
        double minTargetDmg,
        double maxSelfDmg,
        double selfDmgWeight,
        boolean antiSuicide,
        double antiSuicideMinHp
    );
}
