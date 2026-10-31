package ravex.modules.movement;
import ravex.event.client.TickEvent;
import ravex.event.network.PacketEvent;
import ravex.event.Subscribe;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.network.NetworkUtility;
import ravex.utility.movement.MoveUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.PlayerUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import ravex.utility.misc.block.BlockUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "NoFall", category = "Movement")
public class NoFall {
    @Parameter(name = "Mode", modes = {"Vanilla", "NCP", "Grim", "UNCP", "Verus", "Items"})
    public String mode = "Vanilla";
    // grim fly trick needs an elytra, pulled automatically
    @Parameter(name = "AutoElytra", visible = "mode=Grim")
    public boolean autoElytra = true;
    // restore trades elytra protection for the chestplate
    @Parameter(name = "RestoreElytra", visible = "mode=Grim && autoElytra")
    public boolean restoreElytraEnabled = false;
    // fall back to a clutch when no elytra is found
    @Parameter(name = "FallbackItems", visible = "mode=Grim")
    public boolean fallbackItems = true;
    @Parameter(name = "UseWater", visible = "mode=Items")
    public boolean useWater = true;
    @Parameter(name = "UsePowderSnow", visible = "mode=Items")
    public boolean usePowderSnow = true;
    @Parameter(name = "UseCobweb", visible = "mode=Items")
    public boolean useCobweb = true;
    @Parameter(name = "Highlight", visible = "mode=Items")
    public boolean clutchHighlight = true;
    @Parameter(name = "HighlightColor", color = true, visible = "mode=Items")
    public int clutchColor = 0x6600E5FF;

    public static SilentRotationUtility silentRotation = new SilentRotationUtility();
    public static volatile net.minecraft.core.BlockPos clutchPlacedPos;
    public static volatile long clutchPlacedMs;

    private int uncpCounter = 0;
    private boolean clutchDone = false;
    private int clutchRotationHold = 0;
    // grim clamps the fall to one block while gliding
    private boolean grimArmed = false;
    private int grimFlyCountdown = -1;
    private int grimJumpTicks = 0;
    private boolean grimPressingJump = false;
    private boolean grimSwappedElytra = false;
    private int grimElytraHotbarSlot = -1;
    private boolean grimLoggedElytraMissing = false;
    private int grimFlySent = 0;

    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(NoFall.class) || !event.isSend()) return;
        var packet = event.getPacket();
        if (!NetworkUtility.isMovePacket(packet)) return;
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;

        if ("Grim".equals(mode)) {
            boolean elytra = InventoryUtility.isWearingElytra(player);
            if (elytra) {
                // fly packet must leave before the move packet
                if (grimArmed && player.onGround()) {
                    grimSendFlyPacket(player);
                }
            } else if (PlayerUtility.getFallDistance(player) > 0.5) {
                // no elytra: lie about being grounded
                NetworkUtility.setMoveOnGround(packet, true);
            }
            return;
        }
        if ("UNCP".equals(mode) || "Items".equals(mode)) return;

        // the server accumulates the fall from every move packet,
        // so the ground lie has to cover the whole fall
        if (PlayerUtility.getFallDistance(player) <= 2.0) return;

        NetworkUtility.setMoveOnGround(packet, true);
    }

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!Modules.enabled(NoFall.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;

        String modeVal = mode;
        if ("Grim".equals(modeVal)) {
            grimTick(mc, player);
        } else if ("Items".equals(modeVal)) {
            itemsTick(mc, player);
        } else if ("UNCP".equals(modeVal) && PlayerUtility.getFallDistance(player) > 0.5) {
            PlayerUtility.setFallDistance(player, 0);
            MoveUtility.setMotion(
                PlayerUtility.getDeltaMovement(player).x * 0.98,
                Math.min(PlayerUtility.getDeltaMovement(player).y, 0.0) * 0.5,
                PlayerUtility.getDeltaMovement(player).z * 0.98
            );
            uncpCounter++;
            if (uncpCounter % 3 == 0) {
                NetworkUtility.sendMoveRelative(
                    player.getX(), player.getY(), player.getZ(),
                    true, player.horizontalCollision
                );
            }
        }
    }

    // after the fall, fly then jump back to a clean state
    private void grimTick(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
        if (mc.getOptions() == null) return;
        if (!player.onGround() && PlayerUtility.getFallDistance(player) >= 3.0) {
            grimArmed = true;
            if (autoElytra) tryAutoElytra(mc, player);
        }
        boolean elytra = InventoryUtility.isWearingElytra(player);
        if (!elytra) {
            // every packet apply sees a fall under three blocks
            if (!player.onGround() && !player.isFallFlying()
                    && !player.isInWater() && !player.isInLava()) {
                var motion = PlayerUtility.getDeltaMovement(player);
                if (motion.y < -2.6) {
                    MoveUtility.setMotion(motion.x, -2.6, motion.z);
                }
            }
            if (fallbackItems) {
                itemsTick(mc, player);
            }
            return;
        }
        if (grimFlyCountdown > 0) {
            grimFlyCountdown--;
            if (grimFlyCountdown == 0) {
                grimPressingJump = true;
                grimJumpTicks = 0;
            }
        }
        if (grimPressingJump) {
            grimJumpTicks++;
            mc.getOptions().keyJump.setDown(true);
            if (grimJumpTicks >= 9) {
                grimPressingJump = false;
                grimJumpTicks = 0;
                mc.getOptions().keyJump.setDown(physicalJump(mc));
                if (!player.onGround()) {
                    grimArmed = false;
                    if (autoElytra) restoreElytra(mc, player);
                }
                grimFlyCountdown = -1;
            }
        }
    }

    private void grimSendFlyPacket(net.minecraft.client.player.LocalPlayer player) {
        if (grimFlyCountdown < 0) {
            grimFlyCountdown = 10;
            grimFlySent = 0;
            boolean elytra = InventoryUtility.isWearingElytra(player);
            System.out.println("[NoFall] grim: landing, elytra=" + elytra);
        }
        grimFlySent++;
        NetworkUtility.sendStartFallFlying();
    }

    // the elytra stays on, that is what keeps the clamp
    private void tryAutoElytra(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
        if (grimSwappedElytra) return;
        if (InventoryUtility.isWearingElytra(player)) return;
        if (grimLoggedElytraMissing && !InventoryUtility.hasElytra(player)) return;
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.isItem(player.getInventory().getItem(i), "elytra")) {
                mc.getGameMode().handleInventoryMouseClick(player.containerMenu.containerId, 6, i,
                    net.minecraft.world.inventory.ClickType.SWAP, player);
                grimSwappedElytra = true;
                grimElytraHotbarSlot = i;
                System.out.println("[NoFall] grim: elytra equipped from hotbar slot " + i);
                return;
            }
        }
        if (!player.containerMenu.getCarried().isEmpty()) return;
        for (int i = 9; i < 36; i++) {
            if (InventoryUtility.isItem(player.getInventory().getItem(i), "elytra")) {
                var gm = mc.getGameMode();
                int containerId = player.containerMenu.containerId;
                int containerSlot = ravex.utility.player.InventoryUtility.inventorySlotToContainerSlot(i);
                gm.handleInventoryMouseClick(containerId, containerSlot, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
                gm.handleInventoryMouseClick(containerId, 6, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
                gm.handleInventoryMouseClick(containerId, containerSlot, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
                grimSwappedElytra = true;
                grimElytraHotbarSlot = -1;
                System.out.println("[NoFall] grim: elytra equipped from inventory slot " + i);
                return;
            }
        }
        if (!grimLoggedElytraMissing) {
            grimLoggedElytraMissing = true;
            System.out.println("[NoFall] grim: no elytra found, wear one or use Items mode");
        }
    }

    private void restoreElytra(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
        if (!grimSwappedElytra || !restoreElytraEnabled) return;
        if (grimElytraHotbarSlot < 0) return;
        grimSwappedElytra = false;
        if (InventoryUtility.isWearingElytra(player)) {
            mc.getGameMode().handleInventoryMouseClick(player.containerMenu.containerId, 6, grimElytraHotbarSlot,
                net.minecraft.world.inventory.ClickType.SWAP, player);
        }
        grimElytraHotbarSlot = -1;
    }

    private static boolean physicalJump(MinecraftWrapper mc) {
        long win = mc.getWindowHandle();
        if (win == 0L) return false;
        return org.lwjgl.glfw.GLFW.glfwGetKey(win, org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE)
            == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    // real clutch: soft block under the landing spot
    private void itemsTick(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
        if (mc.getOptions() == null || mc.getLevel() == null) return;
        if (clutchRotationHold > 0 && --clutchRotationHold == 0) {
            silentRotation.reset();
        }
        if (PlayerUtility.isOnGround(player) || PlayerUtility.getFallDistance(player) < 3.0) {
            clutchDone = false;
            return;
        }
        if (clutchDone) return;
        var landing = findLandingCell(mc, player);
        if (landing == null) return;
        double gap = player.getY() - landing.getY();
        // 2.8 gap keeps the bucket ray inside the 4.5 reach
        if (gap < 0 || gap > 2.8) return;
        int slot = findClutchSlot(player);
        if (slot == -1) return;
        // aim at the support block or the water flies past
        var groundCenter = net.minecraft.world.phys.Vec3.atCenterOf(BlockUtility.below(landing));
        float[] ang = ravex.utility.player.rotation.RotationUtility.anglesTo(player.getEyePosition(), groundCenter);
        silentRotation.set(ang[0], ang[1]);
        clutchRotationHold = 6;
        NetworkUtility.sendRot(ang[0], ang[1], player.onGround(), player.horizontalCollision);

        var item = InventoryUtility.getItem(player, slot);
        boolean bucket = InventoryUtility.isItem(item, "water_bucket")
            || InventoryUtility.isItem(item, "powder_snow_bucket");
        if (bucket) {
            // fluid buckets need plain use, not useOn
            int prev = InventoryUtility.getSelectedSlot(player);
            InventoryUtility.swapToSlot(player, slot, "Normal");
            boolean used = mc.getGameMode().useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            InventoryUtility.swapBackSlot(player, prev, "Normal");
            if (used) {
                markClutchPlaced(landing);
            }
        } else {
            BlockUtility.PlaceConfig cfg = new BlockUtility.PlaceConfig();
            cfg.restoreSlot = true;
            cfg.swapMode = "Normal";
            // placeBlock would re aim and break the ray
            cfg.rotate = false;
            if (BlockUtility.placeBlock(mc, landing, slot, cfg)) {
                markClutchPlaced(landing);
            }
        }
    }

    private void markClutchPlaced(net.minecraft.core.BlockPos landing) {
        clutchDone = true;
        clutchRotationHold = 5;
        clutchPlacedPos = landing;
        clutchPlacedMs = System.currentTimeMillis();
    }

    // first open cell above the ground in the player column.
    private net.minecraft.core.BlockPos findLandingCell(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
        var level = mc.getLevel();
        if (level == null) return null;
        int x = (int) Math.floor(player.getX());
        int z = (int) Math.floor(player.getZ());
        int minY = BlockUtility.getMinY(level);
        for (int y = (int) Math.floor(player.getY()) - 1; y > minY; y--) {
            var p = BlockUtility.pos(x, y, z);
            if (BlockUtility.hasCollision(level, p)) {
                return BlockUtility.above(p);
            }
        }
        return null;
    }

    private int findClutchSlot(net.minecraft.client.player.LocalPlayer player) {
        if (useWater) {
            int s = InventoryUtility.findHotbarSlot(player, "water_bucket");
            if (s != -1) return s;
        }
        if (usePowderSnow) {
            int s = InventoryUtility.findHotbarSlot(player, "powder_snow_bucket");
            if (s != -1) return s;
        }
        if (useCobweb) {
            int s = InventoryUtility.findHotbarSlot(player, "cobweb");
            if (s != -1) return s;
        }
        return -1;
    }

    public void onDisable() {
        uncpCounter = 0;
        clutchDone = false;
        clutchRotationHold = 0;
        grimArmed = false;
        grimFlyCountdown = -1;
        grimJumpTicks = 0;
        grimPressingJump = false;
        silentRotation.reset();
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player != null) {
            restoreElytra(mc, player);
        }
        grimSwappedElytra = false;
        grimElytraHotbarSlot = -1;
        if (mc.getOptions() != null) {
            mc.getOptions().keyJump.setDown(physicalJump(mc));
        }
    }
}
