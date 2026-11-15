package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import org.joml.Matrix4f;
import net.minecraft.client.Camera;
import java.util.ArrayList;
import java.util.List;
import ravex.utility.network.NetworkUtility;
import ravex.utility.render.Render3DUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.utility.misc.CombatUtility;
import ravex.utility.misc.GameModeUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.movement.MoveUtility;
import ravex.utility.player.PlayerUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;

@Module(name = "AutoCrystal", category = "Combat")
public class AutoCrystal {
    @Parameter(name = "PlaceRange", min = 1.0, max = 6.0, step = 0.1)
    public double placeRange = 4.3;
    @Parameter(name = "BreakRange", min = 1.0, max = 6.0, step = 0.1)
    public double breakRange = 4.5;
    @Parameter(name = "PlaceDelay", min = 0, max = 500, step = 10)
    public double placeDelay = 20;
    @Parameter(name = "BreakDelay", min = 0, max = 500, step = 10)
    public double breakDelay = 20;
    @Parameter(name = "MinDamage", min = 1.0, max = 20.0, step = 0.5)
    public double minDamage = 4.5;
    @Parameter(name = "MaxSelfDmg", min = 1.0, max = 20.0, step = 0.5)
    public double maxSelfDmg = 8.0;
    @Parameter(name = "AntiSuicide")
    public boolean antiSuicide = true;
    @Parameter(name = "AntiSuicideMinHP", min = 1.0, max = 20.0, step = 0.5, visible = "antiSuicide")
    public double antiSuicideMinHp = 8.0;
    @Parameter(name = "RotateMode", modes = {"None", "Grim", "Strict"})
    public String rotate = "Grim";
    @Parameter(name = "SwapMode", modes = {"Silent", "Strict", "None"})
    public String swapMode = "Silent";
    @Parameter(name = "Target", modes = {"Closest", "LowestHP", "HighestDamage"})
    public String targetMode = "Closest";
    @Parameter(name = "TargetType", modes = {"Players", "Monsters", "Passives", "All"})
    public String targetType = "Players";
    @Parameter(name = "RenderPlacement")
    public boolean renderPlacement = true;
    @Parameter(name = "RenderDamage")
    public boolean renderDamage = true;
    @Parameter(name = "ArmorBreaker")
    public boolean armorBreaker = true;
    @Parameter(name = "ArmorPercent", min = 1.0, max = 50.0, step = 1.0)
    public double armorPercent = 20.0;
    @Parameter(name = "PredictTicks", min = 0.0, max = 4.0, step = 0.1)
    public double predictTicks = 1.2;
    @Parameter(name = "SpawnBreak")
    public boolean spawnBreak = true;
    @Parameter(name = "TargetESP")
    public boolean targetEsp = true;
    @Parameter(name = "TargetESPMode", modes = {"Circle", "Square", "RaveXV1"}, visible = "targetEsp")
    public String targetEspMode = "Circle";
    @Parameter(name = "TargetESPColor", color = true, visible = "targetEsp")
    public int targetEspColor = 0xFF40A9F8;
    @Parameter(name = "TotemDetection")
    public boolean totemDetection = true;
    @Parameter(name = "PlaceMode", modes = {"Strict", "NCPStrict", "Grim"})
    public String placeMode = "Grim";
    @Parameter(name = "RotateSpeed", min = 10.0, max = 180.0, step = 5.0)
    public double rotateSpeed = 180.0;
    @Parameter(name = "RotateRandomize", min = 0.0, max = 3.0, step = 0.1)
    public double rotateRandomize = 0.0;
    @Parameter(name = "SafeBreak")
    public boolean antiSuicideCheckBreaking = true;
    @Parameter(name = "TotemBypass")
    public boolean antiSuicideIgnoreWithTotem = false;
    @Parameter(name = "TotemCheckTarget")
    public boolean totemCheckTarget = true;
    @Parameter(name = "TotemPopSwap")
    public boolean totemPopSwap = true;
    @Parameter(name = "TotemPopHP", min = 1.0, max = 20.0, step = 0.5)
    public double totemPopHp = 8.0;
    @Parameter(name = "PlaceWallRange", min = 1.0, max = 6.0, step = 0.1)
    public double placeWallRange = 3.0;
    @Parameter(name = "BreakWallRange", min = 1.0, max = 6.0, step = 0.1)
    public double breakWallRange = 3.0;
    @Parameter(name = "AirPlace")
    public boolean placeAirPlace = false;
    @Parameter(name = "PlaceUnderHP", min = 0.0, max = 36.0, step = 0.5)
    public double placeUnderHp = 10.0;
    @Parameter(name = "MultiPlace")
    public boolean placeMultiPlace = false;
    @Parameter(name = "SwapSwitchBack")
    public boolean swapSwitchBack = true;
    @Parameter(name = "SwapNoGap")
    public boolean swapNoGap = true;
    @Parameter(name = "SwapInventory")
    public boolean swapInventory = false;
    @Parameter(name = "BGBlockScanner")
    public boolean bgBlockScanner = true;
    @Parameter(name = "Suicide")
    public boolean suicide = false;
    @Parameter(name = "KBPrediction")
    public boolean kbPrediction = true;
    @Parameter(name = "CollateralPop")
    public boolean collateralPop = true;
    @Parameter(name = "StickyPlace")
    public boolean stickyPlace = true;
    @Parameter(name = "StickyMaxSelfDmg", min = 1.0, max = 20.0, step = 0.5, visible = "stickyPlace")
    public double stickyMaxSelfDmg = 9.5;
    public static BlockPos currentPlacementBlock = null;
    public static Vec3 currentRotationTarget = null;
    public static double currentTargetDamage = 0.0;
    public static double currentSelfDamage = 0.0;
    public static int currentTargetTotems = 0;
    private long lastPlaceTime = 0;
    private long lastBreakTime = 0;
    private int lastBreakId = -1;
    public static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    public LivingEntity currentTarget = null;
    private static float scanProgress = 0f;
    private static float prevScanProgress = 0f;
    private static float slowRotation = 0f;
    private static float prevSlowRotation = 0f;
    private static float circleStep = 0f;
    private static float prevCircleStep = 0f;
    private static float squareAngle = 0f;
    private static float prevSquareAngle = 0f;
    private static float squareSpeed = 0f;
    private static boolean squareFlipSpeed = false;
    private int originalSlot = -1;
    private int crystalSlot = -1;
    private BlockPos lastStickyPos = null;
    private double[] cachedBlockData = null;
    private long lastBlockScanTime = 0;

    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }

    public void onEnable() {
        lastPlaceTime = 0;
        lastBreakTime = 0;
        lastBreakId = -1;
        originalSlot = -1;
        crystalSlot = -1;
        silentRotation.reset();
        currentTarget = null;
        currentPlacementBlock = null;
        currentRotationTarget = null;
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        silentRotation.hasRotation = false;

        prevScanProgress = scanProgress;
        scanProgress += 0.05f;
        if (scanProgress >= 2.0f) {
            scanProgress -= 2.0f;
            prevScanProgress -= 2.0f;
        }

        prevSlowRotation = slowRotation;
        slowRotation += 2.0f;
        if (slowRotation >= 360.0f) {
            slowRotation -= 360.0f;
            prevSlowRotation -= 360.0f;
        }

        prevCircleStep = circleStep;
        circleStep += 0.15f;

        prevSquareAngle = squareAngle;
        squareAngle += squareSpeed;
        if (squareSpeed > 25) squareFlipSpeed = true;
        if (squareSpeed < -25) squareFlipSpeed = false;
        squareSpeed = squareFlipSpeed ? squareSpeed - 0.5f : squareSpeed + 0.5f;

        if (totemPopSwap) {
            double selfHp = EntityUtility.getHealthWithAbsorption(mc.getPlayer());
            if (selfHp <= totemPopHp) {
                if (!InventoryUtility.isOffhand(mc.getPlayer(), "totem_of_undying")) {
                    int totemSlot = InventoryUtility.findSlot(mc.getPlayer(), "totem_of_undying");
                    if (totemSlot != -1) {
                        InventoryUtility.swapToOffhand(mc, mc.getPlayer(), totemSlot);
                    }
                }
            }
        }
        if (GameModeUtility.isLocalSpectator()) {
            currentTarget = null;
            currentPlacementBlock = null;
            return;
        }

        boolean localCreative = GameModeUtility.isLocalCreative();
        boolean creativeSafe = localCreative;
        LivingEntity target = suicide ? mc.getPlayer() : findTarget(mc);
        currentTarget = target;
        if (target == null) {
            currentPlacementBlock = null;
            return;
        }
        Vec3 playerPos = mc.getPlayer().position();
        double pHp = EntityUtility.getHealth(mc.getPlayer());
        double pAbs = EntityUtility.getAbsorption(mc.getPlayer());
        Vec3 targetPos = target.position();
        double vx = target.getX() - target.xOld;
        double vy = target.getY() - target.yOld;
        double vz = target.getZ() - target.zOld;
        if (vx == 0 && vy == 0 && vz == 0 && target.getDeltaMovement() != null) {
            vx = target.getDeltaMovement().x;
            vy = target.getDeltaMovement().y;
            vz = target.getDeltaMovement().z;
        }
        Vec3 targetVelocity = new Vec3(vx, vy, vz);
        double tHp = EntityUtility.getHealth(target);
        double tAbs = EntityUtility.getAbsorption(target);
        double[] blockData = collectValidBlocks(mc, playerPos);
        double[] crystalData = collectCrystals(mc, playerPos);
        double[] pStats = CombatUtility.getEntityStats(mc.getPlayer());
        double[] tStats = CombatUtility.getEntityStats(target);
        tStats[11] = vx;
        tStats[12] = vy;
        tStats[13] = vz;
        boolean grimAC = rotate.equals("Grim") || placeMode.equals("Grim");
        boolean ncpBypass = rotate.equals("Strict") || placeMode.equals("Strict");

        double maxSelfDmgEffective = creativeSafe ? Double.MAX_VALUE : maxSelfDmg;
        boolean effectiveAntiSuicide = antiSuicide && !suicide && !creativeSafe;

        PlaceResult placeResult = findBestPlace(mc, playerPos, pHp, pAbs, pStats, targetPos, tHp, tAbs, tStats, blockData, crystalData, creativeSafe);
        boolean shouldPlace = placeResult.shouldPlace;
        double placeSelfDmg = placeResult.selfDmg;
        double placeTargetDmg = placeResult.targetDmg;
        BlockPos placePos = placeResult.pos;

        boolean ignoreSuicide = antiSuicideIgnoreWithTotem && pStats[14] > 0.0;
        boolean lowHealth = effectiveAntiSuicide && !ignoreSuicide && pHp + pAbs <= antiSuicideMinHp;
        if (lowHealth) {
            shouldPlace = false;
            if (antiSuicideCheckBreaking) {
                shouldPlace = false;
            }
        }
        if (shouldPlace && effectiveAntiSuicide && !ignoreSuicide) {
            if (pHp + pAbs - placeSelfDmg <= antiSuicideMinHp) {
                shouldPlace = false;
            }
        }
        if (shouldPlace && !creativeSafe && !suicide) {
            if (placeSelfDmg > maxSelfDmgEffective) {
                shouldPlace = false;
            }
        }

        BreakResult breakResult = findBestBreak(mc, playerPos, pHp, pAbs, pStats, crystalData, creativeSafe);
        boolean shouldBreak = breakResult.shouldBreak;
        double breakSelfDmg = breakResult.selfDmg;
        int breakEntityId = breakResult.entityId;
        Vec3 breakPos = breakResult.pos;

        if (shouldBreak && !creativeSafe && !suicide) {
            if (breakSelfDmg > maxSelfDmgEffective) {
                shouldBreak = false;
            }
            if (effectiveAntiSuicide && antiSuicideCheckBreaking && !ignoreSuicide) {
                if (pHp + pAbs - breakSelfDmg <= antiSuicideMinHp) {
                    shouldBreak = false;
                }
            }
        }

        if (shouldPlace) {
            currentPlacementBlock = placePos;
            currentTargetDamage = placeTargetDmg;
            currentSelfDamage = placeSelfDmg;
            currentTargetTotems = (int) tStats[14];
        } else {
            // Maintain lock on current spot during break/place delay cycles so AutoCrystal doesn't jump around
            if (currentPlacementBlock != null) {
                Vec3 blockCenter = PhysicUtility.centerOf(currentPlacementBlock).add(0, 0.5, 0);
                double distToPlayer = playerPos.distanceTo(blockCenter);
                double distToTarget = targetPos.distanceTo(blockCenter);
                double maxTargetDist = Math.max(placeRange, breakRange) + 2.0;
                BlockState state = mc.getLevel().getBlockState(currentPlacementBlock);
                if (distToPlayer > placeRange + 1.5 || distToTarget > maxTargetDist || (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK))) {
                    currentPlacementBlock = null;
                }
            }
        }

        long now = System.currentTimeMillis();
        Vec3 rotationTarget = null;
        if (shouldBreak) {
            net.minecraft.world.entity.Entity crystal = mc.getLevel().getEntity(breakEntityId);
            if (crystal instanceof EndCrystal) {
                rotationTarget = crystal.position().add(0, 1.0, 0);
            }
        }
        if (rotationTarget == null && shouldPlace) {
            rotationTarget = new Vec3(placePos.getX() + 0.5, placePos.getY() + 1.0, placePos.getZ() + 0.5);
        }
        if (rotationTarget != null && !rotate.equals("None")) {
            currentRotationTarget = rotationTarget;
            performRotation(mc, rotationTarget);
        } else {
            currentRotationTarget = null;
        }

        boolean isStrict = rotate.equals("Strict") || placeMode.equals("Strict");
        boolean checkAlign = !rotate.equals("None");
        boolean aligned = true;
        if (checkAlign && rotationTarget != null) {
            aligned = isRotationAligned(mc, rotationTarget);
        }
        int maxActions = isStrict ? 1 : 2;
        int actionsThisTick = 0;
        long minStrictDelay = 65L;
        long effectiveBreakDelay = isStrict ? Math.max((long) breakDelay, minStrictDelay) : (long) breakDelay;
        long effectivePlaceDelay = isStrict ? Math.max((long) placeDelay, minStrictDelay) : (long) placeDelay;

        if (shouldBreak && aligned) {
            if (now - lastBreakTime >= effectiveBreakDelay) {
                net.minecraft.world.entity.Entity crystal = mc.getLevel().getEntity(breakEntityId);
                if (crystal instanceof EndCrystal && crystal.isAlive() && !crystal.isRemoved()) {
                    EntityUtility.attack(mc, crystal);
                    EntityUtility.swingHand(mc);
                    lastBreakTime = now;
                    lastBreakId = breakEntityId;
                    actionsThisTick++;
                }
            }
        }

        boolean checkPlaceDelay = true;
        if (target != null && !isStrict) {
            double targetEffHp = EntityUtility.getHealthWithAbsorption(target);
            if (targetEffHp <= placeUnderHp) {
                checkPlaceDelay = false;
            }
        }
        if (shouldPlace && aligned && actionsThisTick < maxActions) {
            if (!checkPlaceDelay || now - lastPlaceTime >= effectivePlaceDelay) {
                boolean hasItem = switchToCrystal(mc);
                if (hasItem) {
                    Vec3 hitVec = new Vec3(placePos.getX() + 0.5, placePos.getY() + 1.0, placePos.getZ() + 0.5);
                    net.minecraft.core.Direction face = net.minecraft.core.Direction.UP;
                    BlockHitResult hitResult = new BlockHitResult(hitVec, face, placePos, false);
                    InteractionHand hand = getPlacementHand(mc);
                    int prevSelected = mc.getPlayer().getInventory().getSelectedSlot();
                    if (swapMode.equals("Silent") && hand == InteractionHand.MAIN_HAND && crystalSlot != -1) {
                        mc.getPlayer().getInventory().setSelectedSlot(crystalSlot);
                    }
                    mc.getGameMode().useItemOn(mc.getPlayer(), hand, hitResult);
                    SwingUtility.swing(mc.getPlayer(), hand);
                    if (swapMode.equals("Silent") && hand == InteractionHand.MAIN_HAND && crystalSlot != -1) {
                        mc.getPlayer().getInventory().setSelectedSlot(prevSelected);
                    }
                    if (hand == InteractionHand.MAIN_HAND && swapSwitchBack && originalSlot != -1) {
                        if (!swapMode.equals("Strict")) {
                            if (mc.getPlayer().connection != null) {
                                NetworkUtility.sendSetCarriedItem(originalSlot);
                            }
                            originalSlot = -1;
                        }
                    }
                    lastPlaceTime = now;
                    lastStickyPos = placePos;
                    actionsThisTick++;
                }
            }
        }

        boolean shouldPlace2 = !isStrict && placeMultiPlace;
        if (shouldPlace2) {
            if (lowHealth) {
                shouldPlace2 = false;
            } else if (effectiveAntiSuicide && !ignoreSuicide) {
                double selfDmg2 = calcSelfDamage(mc, playerPos, placePos, pHp, pAbs, pStats);
                if (pHp + pAbs - selfDmg2 <= antiSuicideMinHp) {
                    shouldPlace2 = false;
                }
            }
            if (shouldPlace2 && !creativeSafe && !suicide) {
                double selfDmg2 = calcSelfDamage(mc, playerPos, placePos, pHp, pAbs, pStats);
                if (selfDmg2 > maxSelfDmgEffective) {
                    shouldPlace2 = false;
                }
            }
        }
        if (shouldPlace2 && aligned && actionsThisTick < maxActions) {
            if (!checkPlaceDelay || now - lastPlaceTime >= effectivePlaceDelay) {
                boolean hasItem = switchToCrystal(mc);
                if (hasItem) {
                    Vec3 hitVec2 = new Vec3(placePos.getX() + 0.5, placePos.getY() + 1.0, placePos.getZ() + 0.5);
                    net.minecraft.core.Direction face = net.minecraft.core.Direction.UP;
                    BlockHitResult hitResult2 = new BlockHitResult(hitVec2, face, placePos, false);
                    InteractionHand hand2 = getPlacementHand(mc);
                    int prevSelected2 = mc.getPlayer().getInventory().getSelectedSlot();
                    if (swapMode.equals("Silent") && hand2 == InteractionHand.MAIN_HAND && crystalSlot != -1) {
                        mc.getPlayer().getInventory().setSelectedSlot(crystalSlot);
                    }
                    mc.getGameMode().useItemOn(mc.getPlayer(), hand2, hitResult2);
                    SwingUtility.swing(mc.getPlayer(), hand2);
                    if (swapMode.equals("Silent") && hand2 == InteractionHand.MAIN_HAND && crystalSlot != -1) {
                        mc.getPlayer().getInventory().setSelectedSlot(prevSelected2);
                    }
                    if (hand2 == InteractionHand.MAIN_HAND && swapSwitchBack && originalSlot != -1) {
                        if (!swapMode.equals("Strict")) {
                            if (mc.getPlayer().connection != null) {
                                NetworkUtility.sendSetCarriedItem(originalSlot);
                            }
                            originalSlot = -1;
                        }
                    }
                    lastPlaceTime = now;
                    actionsThisTick++;
                }
            }
        }
    }

    private PlaceResult findBestPlace(MinecraftWrapper mc, Vec3 playerPos, double pHp, double pAbs, double[] pStats, Vec3 targetPos, double tHp, double tAbs, double[] tStats, double[] blockData, double[] crystalData, boolean creativeSafe) {
        boolean shouldPlace = false;
        double bestScore = -Double.MAX_VALUE;
        double bestTargetDmg = 0;
        double bestSelfDmg = Double.MAX_VALUE;
        BlockPos bestPos = null;

        List<double[]> blocks = parseBlockData(blockData);
        List<double[]> crystals = parseCrystalData(crystalData);

        double selfDmgLimit = creativeSafe ? Double.MAX_VALUE : (stickyPlace && currentPlacementBlock != null ? stickyMaxSelfDmg : maxSelfDmg);

        for (double[] b : blocks) {
            Vec3 blockCenter = new Vec3(b[0] + 0.5, b[1] + 1.0, b[2] + 0.5);
            double distToPlayer = playerPos.distanceTo(blockCenter);
            double maxPlaceDist = placeRange;
            if (PhysicUtility.raycast(MoveUtility.eyePos(), blockCenter) != null) {
                maxPlaceDist = placeWallRange;
            }
            if (distToPlayer > maxPlaceDist) continue;

            BlockPos pos = new BlockPos((int) b[0], (int) b[1], (int) b[2]);
            BlockState state = mc.getLevel().getBlockState(pos);
            if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) continue;

            BlockState above = mc.getLevel().getBlockState(pos.above());
            boolean validSpace = above.isAir();
            if (validSpace && placeMode.equals("Strict")) {
                BlockState above2 = mc.getLevel().getBlockState(pos.above(2));
                validSpace = above2.isAir();
            }
            if (!validSpace) continue;

            // Check if crystal is already sitting on this block
            boolean occupied = false;
            for (double[] c : crystals) {
                if (Math.abs(c[1] - (b[0] + 0.5)) < 1.0 && Math.abs(c[2] - (b[1] + 1.0)) < 2.0 && Math.abs(c[3] - (b[2] + 0.5)) < 1.0) {
                    occupied = true;
                    break;
                }
            }
            if (occupied) continue;

            double targetDmg = calcTargetDamage(blockCenter, targetPos, tHp, tAbs, tStats);
            if (targetDmg < minDamage) continue;

            double selfDmg = calcSelfDamage(mc, playerPos, pos, pHp, pAbs, pStats);

            if (!creativeSafe && !suicide) {
                boolean ignoreSuicide = (antiSuicideIgnoreWithTotem && pStats[14] > 0.0) || pStats[14] > 0.0;
                if (antiSuicide && !ignoreSuicide && pHp + pAbs - selfDmg <= antiSuicideMinHp) continue;
                if (selfDmg > selfDmgLimit) continue;
            }

            // Score formula: balance target damage vs self damage
            double score = targetDmg * 1.5 - selfDmg;

            // Prioritize placing level with or slightly below opponent's feet ("по возможности ниже противника чуть")
            // Explosions calculate exposure from the feet upward, maximizing damage when placed at/below feet
            double dy = (b[1] + 1.0) - targetPos.y;
            if (dy <= 0.1 && dy >= -1.2) {
                // Optimal CPvP position: level with or just below feet
                score += 18.0;
            } else if (dy < -1.2 && dy >= -2.5) {
                score += 8.0;
            } else if (dy > 0.1) {
                // Crystal is higher than feet: heavily penalize so AutoCrystal strictly avoids higher spots
                score -= (10.0 + (dy * 15.0));
            }

            // Strong sticky bonus: commit to one single best spot ("в одном месте... ставил там и взрывал")
            if (currentPlacementBlock != null && pos.equals(currentPlacementBlock)) {
                score += 35.0;
            }

            if (!shouldPlace || score > bestScore) {
                shouldPlace = true;
                bestScore = score;
                bestTargetDmg = targetDmg;
                bestSelfDmg = selfDmg;
                bestPos = pos;
            }
        }

        return new PlaceResult(shouldPlace, bestPos, bestTargetDmg, bestSelfDmg);
    }

    private BreakResult findBestBreak(MinecraftWrapper mc, Vec3 playerPos, double pHp, double pAbs, double[] pStats, double[] blockData, boolean creativeSafe) {
        boolean shouldBreak = false;
        double bestSelfDmg = Double.MAX_VALUE;
        double bestScore = -Double.MAX_VALUE;
        int bestEntityId = -1;
        Vec3 bestPos = null;

        List<double[]> crystals = parseCrystalData(blockData);

        for (double[] c : crystals) {
            int entityId = (int) c[0];
            Vec3 crystalPos = new Vec3(c[1], c[2], c[3]);
            double dist = playerPos.distanceTo(crystalPos);
            double maxBreakDist = breakRange;
            if (PhysicUtility.raycast(MoveUtility.eyePos(), crystalPos) != null) {
                maxBreakDist = breakWallRange;
            }
            if (dist > maxBreakDist) continue;

            net.minecraft.world.entity.Entity entity = mc.getLevel().getEntity(entityId);
            if (!(entity instanceof EndCrystal)) continue;
            if (!entity.isAlive() || entity.isRemoved()) continue;

            double selfDmg = calcSelfDamage(mc, playerPos, new BlockPos((int) c[1], (int) c[2], (int) c[3]), pHp, pAbs, pStats);

            if (!creativeSafe && !suicide) {
                boolean ignoreSuicide = (antiSuicideIgnoreWithTotem && pStats[14] > 0.0) || pStats[14] > 0.0;
                if (antiSuicide && !ignoreSuicide && pHp + pAbs - selfDmg <= antiSuicideMinHp) continue;
                if (selfDmg > maxSelfDmg) continue;
            }

            // Prioritize breaking the crystal at our chosen single spot ("в одном месте... ставил там и взрывал")
            boolean isAtCurrentSpot = currentPlacementBlock != null
                && Math.abs(c[1] - (currentPlacementBlock.getX() + 0.5)) < 1.0
                && Math.abs(c[3] - (currentPlacementBlock.getZ() + 0.5)) < 1.0;

            double score = (isAtCurrentSpot ? 500.0 : 0.0) - selfDmg;

            if (!shouldBreak || score > bestScore) {
                shouldBreak = true;
                bestScore = score;
                bestSelfDmg = selfDmg;
                bestEntityId = entityId;
                bestPos = crystalPos;
            }
        }

        return new BreakResult(shouldBreak, bestEntityId, bestPos, bestSelfDmg);
    }

    private double calcTargetDamage(Vec3 explosionPos, Vec3 targetPos, double tHp, double tAbs, double[] tStats) {
        double dist = explosionPos.distanceTo(targetPos.add(0, 1.0, 0));
        if (dist > 12.0) return 0.0;
        double impact = Math.max(0.0, (1.0 - dist / 12.0));
        double raw = (impact * impact + impact) / 2.0 * 84.0 + 1.0;
        return applyArmor(raw, tStats);
    }

    private double calcSelfDamage(MinecraftWrapper mc, Vec3 playerPos, BlockPos explosionPos, double pHp, double pAbs, double[] pStats) {
        Vec3 explosionCenter = new Vec3(explosionPos.getX() + 0.5, explosionPos.getY() + 1.0, explosionPos.getZ() + 0.5);
        double dist = playerPos.distanceTo(explosionCenter);
        if (dist > 12.0) return 0.0;
        double impact = Math.max(0.0, (1.0 - dist / 12.0));
        double raw = (impact * impact + impact) / 2.0 * 84.0 + 1.0;

        // When line of sight to player eye is blocked by obsidian or terrain, self damage is reduced
        if (mc.getPlayer() != null && mc.getLevel() != null) {
            Vec3 rayStart = explosionCenter.add(0, 0.2, 0);
            Vec3 eye = MoveUtility.eyePos();
            if (PhysicUtility.raycast(rayStart, eye) != null) {
                raw *= 0.45;
            }
        }
        return applyArmor(raw, pStats);
    }

    private double applyArmor(double raw, double[] stats) {
        if (stats == null || stats.length < 10) return raw;
        double armor = stats[0];
        double toughness = stats[1];
        double blastEpf = stats[2];
        double protEpf = stats[3];
        double resistanceLevel = stats[4];

        // Vanilla armor reduction: damage *= (1 - armorReduction/25)
        double armorFactor = Math.max(armor / 5.0, armor - raw / (2.0 + toughness / 4.0));
        armorFactor = Math.min(20.0, armorFactor);
        double afterArmor = raw * (1.0 - armorFactor / 25.0);

        // Vanilla protection: blast protection (stats[2]) + general protection (stats[3]), capped at 20 EPF (80% reduction)
        double totalEpf = Math.min(20.0, blastEpf + protEpf);
        double afterEnch = afterArmor * (1.0 - totalEpf * 0.04);

        // Resistance effect: 20% per level, capped at 100%
        double resistance = Math.min(1.0, resistanceLevel * 0.2);
        return afterEnch * (1.0 - resistance);
    }

    private List<double[]> parseBlockData(double[] data) {
        List<double[]> result = new ArrayList<>();
        if (data == null) return result;
        for (int i = 0; i + 2 < data.length; i += 3) {
            result.add(new double[]{data[i], data[i + 1], data[i + 2]});
        }
        return result;
    }

    private List<double[]> parseCrystalData(double[] data) {
        List<double[]> result = new ArrayList<>();
        if (data == null) return result;
        for (int i = 0; i + 3 < data.length; i += 4) {
            result.add(new double[]{data[i], data[i + 1], data[i + 2], data[i + 3]});
        }
        return result;
    }

    private void performRotation(MinecraftWrapper mc, Vec3 target) {
        float[] targetAngles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), target);
        float currentYaw = silentRotation.initialized ? silentRotation.lastYaw : mc.getPlayer().getYRot();
        float currentPitch = silentRotation.initialized ? silentRotation.lastPitch : mc.getPlayer().getXRot();
        if (!silentRotation.initialized) {
            silentRotation.init(currentYaw, currentPitch);
        }

        float finalYaw;
        float finalPitch;

        if (rotate.equals("Strict")) {
            float gcd = RotationUtility.getGCD() * 0.15f;
            float maxSpeed = Math.min((float) rotateSpeed, 50.0f);
            float dYaw = RotationUtility.diffYaw(currentYaw, targetAngles[0]);
            float dPitch = RotationUtility.diffPitch(currentPitch, targetAngles[1]);

            float stepYaw = Math.abs(dYaw) > maxSpeed ? Math.signum(dYaw) * maxSpeed : dYaw * 0.72f;
            float stepPitch = Math.abs(dPitch) > maxSpeed ? Math.signum(dPitch) * maxSpeed : dPitch * 0.72f;

            if (rotateRandomize > 0.0) {
                stepYaw += (float) ((Math.random() - 0.5) * rotateRandomize * 0.25);
                stepPitch += (float) ((Math.random() - 0.5) * rotateRandomize * 0.25);
            }

            if (gcd > 0) {
                stepYaw = Math.round(stepYaw / gcd) * gcd;
                stepPitch = Math.round(stepPitch / gcd) * gcd;
            }

            finalYaw = currentYaw + stepYaw;
            finalPitch = RotationUtility.clampPitch(currentPitch + stepPitch);
        } else {
            float[] limited = AimUtility.limitAngles(currentYaw, targetAngles[0], currentPitch, targetAngles[1], (float) rotateSpeed);
            finalYaw = limited[0];
            finalPitch = limited[1];
            if (rotateRandomize > 0.0) {
                float[] rnd = AimUtility.randomize(finalYaw, finalPitch, rotateRandomize);
                finalYaw = rnd[0];
                finalPitch = rnd[1];
            }
        }

        silentRotation.set(finalYaw, finalPitch);
        silentRotation.lastYaw = finalYaw;
        silentRotation.lastPitch = finalPitch;

        mc.getPlayer().yHeadRot = finalYaw;
        mc.getPlayer().yBodyRot = finalYaw;
    }

    private LivingEntity findTarget(MinecraftWrapper mc) {
        LivingEntity closest = null;
        double bestMetric = Double.MAX_VALUE;
        double maxDist = Math.max(placeRange, breakRange) + 2.0;
        String mode = targetMode;
        String typeFilter = targetType;
        for (net.minecraft.world.entity.Entity e : mc.getLevel().entitiesForRendering()) {
            if (!(e instanceof LivingEntity le)) continue;
            if (EntityUtility.isSelf(le)) continue;
            if (EntityUtility.isDead(le)) continue;
            if (typeFilter.equals("Players")) {
                if (!EntityUtility.isPlayer(le)) continue;
            } else if (typeFilter.equals("Monsters")) {
                if (!EntityUtility.isHostile(le)) continue;
            } else if (typeFilter.equals("Passives")) {
                if (EntityUtility.isPlayer(le) || EntityUtility.isHostile(le)) continue;
            }
            if (le instanceof net.minecraft.world.entity.player.Player player &&
                    (GameModeUtility.isCreative(player) || GameModeUtility.isSpectator(player))) {
                continue;
            }
            double vx = le.getX() - le.xOld;
            double vy = le.getY() - le.yOld;
            double vz = le.getZ() - le.zOld;
            Vec3 predPos = le.position().add(vx * predictTicks, vy * predictTicks, vz * predictTicks);
            double dist = mc.getPlayer().position().distanceTo(predPos);
            if (dist > maxDist) continue;
            double metric = switch (mode) {
                case "Closest" -> dist;
                case "LowestHP" -> EntityUtility.getHealth(le);
                case "HighestDamage" -> -calcQuickDamage(mc, le);
                default -> dist;
            };
            if (metric < bestMetric) {
                bestMetric = metric;
                closest = le;
            }
        }
        return closest;
    }

    private double[] collectValidBlocks(MinecraftWrapper mc, Vec3 playerPos) {
        long now = System.currentTimeMillis();
        if (bgBlockScanner && cachedBlockData != null && now - lastBlockScanTime < 150) {
            return cachedBlockData;
        }
        List<Double> data = new ArrayList<>();
        int r = (int) Math.ceil(placeRange) + 1;
        BlockPos origin = BlockPos.containing(playerPos);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = mc.getLevel().getBlockState(pos);
                    if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK)) {
                        BlockState above = mc.getLevel().getBlockState(pos.above());
                        boolean validSpace = above.isAir();
                        if (validSpace && placeMode.equals("Strict")) {
                            BlockState above2 = mc.getLevel().getBlockState(pos.above(2));
                            validSpace = above2.isAir();
                        }
                        if (validSpace) {
                            data.add((double) pos.getX());
                            data.add((double) pos.getY());
                            data.add((double) pos.getZ());
                        }
                    }
                }
            }
        }
        if (Modules.enabled(ravex.modules.combat.BasePlace.class) && Modules.get(ravex.modules.combat.BasePlace.class).autoCrystalSync && ravex.modules.combat.BasePlace.lastPlacedBase != null) {
            long msLimit = (long) (Modules.get(ravex.modules.combat.BasePlace.class).syncPredictTicks * 50);
            if (System.currentTimeMillis() - ravex.modules.combat.BasePlace.lastPlacedTime <= msLimit) {
                BlockPos predictedPos = ravex.modules.combat.BasePlace.lastPlacedBase;
                double dist = Math.sqrt(predictedPos.distToCenterSqr(mc.getPlayer().getX(), mc.getPlayer().getEyeY(), mc.getPlayer().getZ()));
                if (dist <= placeRange) {
                    boolean alreadyAdded = false;
                    for (int i = 0; i < data.size(); i += 3) {
                        if (data.get(i) == predictedPos.getX() && data.get(i + 1) == predictedPos.getY() && data.get(i + 2) == predictedPos.getZ()) {
                            alreadyAdded = true;
                            break;
                        }
                    }
                    if (!alreadyAdded) {
                        data.add((double) predictedPos.getX());
                        data.add((double) predictedPos.getY());
                        data.add((double) predictedPos.getZ());
                    }
                }
            }
        }
        double[] arr = new double[data.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = data.get(i);
        if (bgBlockScanner) {
            cachedBlockData = arr;
            lastBlockScanTime = now;
        } else {
            cachedBlockData = null;
        }
        return arr;
    }

    private double[] collectCrystals(MinecraftWrapper mc, Vec3 playerPos) {
        List<Double> data = new ArrayList<>();
        for (net.minecraft.world.entity.Entity e : mc.getLevel().entitiesForRendering()) {
            if (!(e instanceof EndCrystal)) continue;
            if (mc.getPlayer().distanceTo(e) > breakRange + 2.0) continue;
            data.add((double) e.getId());
            data.add(e.getX());
            data.add(e.getY());
            data.add(e.getZ());
        }
        double[] arr = new double[data.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = data.get(i);
        return arr;
    }

    private boolean switchToCrystal(MinecraftWrapper mc) {
        if (InventoryUtility.isOffhand(mc.getPlayer(), "end_crystal")) {
            crystalSlot = -1;
            return true;
        }
        if (InventoryUtility.isHolding(mc.getPlayer(), "end_crystal")) {
            crystalSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
            return true;
        }
        String mode = swapMode;
        if (mode.equals("None")) return false;
        if (swapNoGap && mc.getPlayer().isUsingItem()) {
            var usingItem = mc.getPlayer().getUseItem();
            if (InventoryUtility.isGoldenApple(usingItem) || InventoryUtility.isEnchantedGoldenApple(usingItem)) {
                return false;
            }
        }
        int slot = InventoryUtility.findHotbarSlot(mc.getPlayer(), "end_crystal");
        if (slot != -1) {
            originalSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
            crystalSlot = slot;
            if (mode.equals("Strict")) {
                InventoryUtility.selectSlot(mc.getPlayer(), slot);
                NetworkUtility.sendSetCarriedItem(slot);
            } else {
                InventoryUtility.silentSelectSlot(mc.getPlayer(), slot);
            }
            return true;
        }
        if (swapInventory) {
            slot = InventoryUtility.findSlot(mc.getPlayer(), "end_crystal", 9, 36);
            if (slot != -1) {
                int targetHotbarSlot = 0;
                InventoryUtility.handleInventoryClick(mc, mc.getPlayer(), slot, targetHotbarSlot, InventoryUtility.SWAP);
                originalSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
                crystalSlot = targetHotbarSlot;
                if (mode.equals("Strict")) {
                    InventoryUtility.selectSlot(mc.getPlayer(), targetHotbarSlot);
                    NetworkUtility.sendSetCarriedItem(targetHotbarSlot);
                } else {
                    InventoryUtility.silentSelectSlot(mc.getPlayer(), targetHotbarSlot);
                }
                return true;
            }
        }
        crystalSlot = -1;
        return false;
    }

    private InteractionHand getPlacementHand(MinecraftWrapper mc) {
        if (InventoryUtility.isOffhand(mc.getPlayer(), "end_crystal")) {
            return InteractionHand.OFF_HAND;
        }
        return InteractionHand.MAIN_HAND;
    }

    private long currentPlaceDelay = 0;
    private long currentBreakDelay = 0;

    private boolean isRotationAligned(MinecraftWrapper mc, Vec3 target) {
        if (rotate.equals("None")) return true;
        float tolerance = rotate.equals("Strict") ? 30.0f : 45.0f;
        return silentRotation.isRotationAligned(mc, target, tolerance);
    }

    private double calcQuickDamage(MinecraftWrapper mc, LivingEntity target) {
        Vec3 playerPos = mc.getPlayer().position();
        Vec3 targetPos = target.position();
        Vec3 crystalPos = targetPos.add(0, 1, 0);
        double dist = playerPos.distanceTo(crystalPos);
        if (dist > 12.0) return 0;
        double impact = Math.max(0, (1.0 - dist / 12.0));
        double raw = (impact * impact + impact) / 2.0 * 84.0 + 1.0;
        return applyArmor(raw, CombatUtility.getEntityStats(target));
    }

    public LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    public void onDisable() {
        silentRotation.reset();
        currentTarget = null;
        currentPlacementBlock = null;
        currentRotationTarget = null;
        originalSlot = -1;
        crystalSlot = -1;
        lastStickyPos = null;
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!event.isReceive()) return;
        if (!spawnBreak) return;
        if (!(event.getPacket() instanceof ClientboundAddEntityPacket packet)) return;
        if (packet.getType() != EntityType.END_CRYSTAL) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        if (GameModeUtility.isLocalSpectator()) return;

        boolean localCreative = GameModeUtility.isLocalCreative();
        boolean creativeSafe = localCreative;
        double distSq = mc.getPlayer().distanceToSqr(packet.getX(), packet.getY(), packet.getZ());

        Vec3 crystalVec = PhysicUtility.vec3(packet.getX(), packet.getY(), packet.getZ());
        Vec3 eyeVec = MoveUtility.eyePos();
        double maxBreakRange = breakRange;
        boolean visible = PhysicUtility.raycast(eyeVec, crystalVec) == null;
        if (!visible) {
            maxBreakRange = breakWallRange;
        }
        if (distSq > maxBreakRange * maxBreakRange) return;

        boolean isStrict = rotate.equals("Strict") || placeMode.equals("Strict");
        if (isStrict) return;

        if (swapNoGap && mc.getPlayer().isUsingItem()) {
            var usingItem = mc.getPlayer().getUseItem();
            if (InventoryUtility.isGoldenApple(usingItem) || InventoryUtility.isEnchantedGoldenApple(usingItem)) {
                return;
            }
        }

        long now = System.currentTimeMillis();
        if (now - lastBreakTime < (long) breakDelay) return;

        double pHp = EntityUtility.getHealth(mc.getPlayer());
        double pAbs = EntityUtility.getAbsorption(mc.getPlayer());
        double[] pStats = CombatUtility.getEntityStats(mc.getPlayer());

        if (!creativeSafe && !suicide) {
            boolean effectiveAntiSuicide = antiSuicide;
            boolean ignoreSuicide = antiSuicideIgnoreWithTotem && pStats[14] > 0.0;
            if (effectiveAntiSuicide && antiSuicideCheckBreaking && !ignoreSuicide) {
                if (pHp + pAbs <= antiSuicideMinHp) {
                    return;
                }
            }

            double selfDmg = calcSelfDamage(mc, mc.getPlayer().position(), new BlockPos((int) packet.getX(), (int) packet.getY(), (int) packet.getZ()), pHp, pAbs, pStats);
            if (selfDmg > maxSelfDmg) {
                return;
            }

            if (effectiveAntiSuicide && antiSuicideCheckBreaking && !ignoreSuicide) {
                if (pHp + pAbs - selfDmg <= antiSuicideMinHp) {
                    return;
                }
            }

            LivingEntity target = currentTarget != null ? currentTarget : findTarget(mc);
            if (target != null) {
                double targetEffHp = EntityUtility.getHealthWithAbsorption(target);
                double[] tStats = CombatUtility.getEntityStats(target);
                double tDmg = calcTargetDamage(new Vec3(packet.getX(), packet.getY(), packet.getZ()), target.position(), EntityUtility.getHealth(target), EntityUtility.getAbsorption(target), tStats);
                boolean forceArmorBreak = armorBreaker && (tStats[7] <= armorPercent || tStats[8] <= armorPercent || tStats[9] <= armorPercent || tStats[10] <= armorPercent) && targetEffHp <= 28.0;
                double reqMinDmg = forceArmorBreak ? Math.min(1.0, minDamage) : minDamage;
                if (tDmg < reqMinDmg) {
                    return;
                }
            }
        }

        var existing = mc.getLevel().getEntity(packet.getId());
        net.minecraft.world.entity.Entity crystal = existing != null ? existing : new EndCrystal(EntityType.END_CRYSTAL, mc.getLevel());
        if (existing == null) {
            crystal.setId(packet.getId());
            crystal.setPos(packet.getX(), packet.getY(), packet.getZ());
        }

        if (rotate.equals("Grim")) {
            Vec3 rotPos = new Vec3(packet.getX(), packet.getY() + 1.0, packet.getZ());
            performRotation(mc, rotPos);
            NetworkUtility.sendRot(silentRotation.yaw, silentRotation.pitch, mc.getPlayer().onGround(), mc.getPlayer().horizontalCollision);
        }

        NetworkUtility.sendInteractAttack(crystal, false);
        NetworkUtility.sendSwing(InteractionHand.MAIN_HAND);
        lastBreakTime = now;
        lastBreakId = packet.getId();
    }

    public void render(Matrix4f modelViewMatrix, Camera camera, float tickDelta) {
        if (!targetEsp) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;

        var target = currentTarget;
        if (target == null || EntityUtility.isDead(target)) return;

        if (targetEspMode.equals("RaveXV1")) {
            float progressVal = prevScanProgress + (scanProgress - prevScanProgress) * tickDelta;
            float rotation = prevSlowRotation + (slowRotation - prevSlowRotation) * tickDelta;
            Render3DUtility.renderRaveXESP(
                modelViewMatrix,
                camera,
                target,
                targetEspColor,
                progressVal,
                rotation,
                tickDelta
            );
        } else if (targetEspMode.equals("Circle")) {
            Render3DUtility.renderCircleESP(
                modelViewMatrix,
                camera,
                target,
                targetEspColor,
                circleStep,
                prevCircleStep,
                tickDelta
            );
        } else if (targetEspMode.equals("Square")) {
            float squareRot = prevSquareAngle + (squareAngle - prevSquareAngle) * tickDelta;
            Render3DUtility.renderSquareESP(
                modelViewMatrix,
                camera,
                target,
                targetEspColor,
                squareRot,
                tickDelta
            );
        }
    }

    private static class PlaceResult {
        final boolean shouldPlace;
        final BlockPos pos;
        final double targetDmg;
        final double selfDmg;
        PlaceResult(boolean shouldPlace, BlockPos pos, double targetDmg, double selfDmg) {
            this.shouldPlace = shouldPlace;
            this.pos = pos;
            this.targetDmg = targetDmg;
            this.selfDmg = selfDmg;
        }
    }

    private static class BreakResult {
        final boolean shouldBreak;
        final int entityId;
        final Vec3 pos;
        final double selfDmg;
        BreakResult(boolean shouldBreak, int entityId, Vec3 pos, double selfDmg) {
            this.shouldBreak = shouldBreak;
            this.entityId = entityId;
            this.pos = pos;
            this.selfDmg = selfDmg;
        }
    }
}
