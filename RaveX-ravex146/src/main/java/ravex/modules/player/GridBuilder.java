package ravex.modules.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class GridBuilder extends Module {
    public static final GridBuilder INSTANCE = new GridBuilder();

    public final NumberParameter range = new NumberParameter("Range", 3.0, 1.0, 5.0, 1.0);
    public final NumberParameter placementLoops = new NumberParameter("Loops", 3.0, 1.0, 10.0, 1.0);
    public final NumberParameter tickDelay = new NumberParameter("Tick Delay", 1.0, 0.0, 10.0, 1.0);
    public final ModeParameter orientation = new ModeParameter("Orientation", "Auto", List.of("Auto", "Horizontal", "Vertical"));
    public final ModeParameter pattern = new ModeParameter("Pattern", "Pillar", List.of("Full", "Pillar", "Hollow", "Cross", "Custom"));

    private boolean[][] grid = new boolean[5][5];
    private Path configPath;

    private boolean buildActive = false;
    private BlockPos center;
    private Direction playerDir;
    private float playerPitch;
    private int blockTicks = 0;
    private int loopCount = 0;
    private boolean wasUsePressed = false;

    private GridBuilder() {
        super("GridBuilder", Category.PLAYER);
        addParameter(range);
        addParameter(placementLoops);
        addParameter(tickDelay);
        addParameter(orientation);
        addParameter(pattern);
        configPath = Paths.get("RaveX", "gridbuilder-grid.json");
        setDefaultPillar();
        loadGridConfig();
    }

    private void setDefaultPillar() {
        grid[0] = new boolean[]{false, false, false, false, false};
        grid[1] = new boolean[]{false, true, true, true, false};
        grid[2] = new boolean[]{false, false, true, false, false};
        grid[3] = new boolean[]{false, false, true, false, false};
        grid[4] = new boolean[]{false, false, false, false, false};
    }

    private void setPatternByName(String name) {
        for (int r = 0; r < 5; r++)
            for (int c = 0; c < 5; c++)
                grid[r][c] = switch (name) {
                    case "Full" -> true;
                    case "Hollow" -> r == 0 || r == 4 || c == 0 || c == 4;
                    case "Cross" -> r == 2 || c == 2;
                    default -> false;
                };
        if ("Pillar".equals(name)) setDefaultPillar();
    }

    private void saveGridConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            JsonObject json = new JsonObject();
            for (int r = 0; r < 5; r++)
                for (int c = 0; c < 5; c++)
                    json.addProperty("cell_" + r + "_" + c, grid[r][c]);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(configPath, gson.toJson(json).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadGridConfig() {
        try {
            if (Files.exists(configPath)) {
                JsonObject json = new Gson().fromJson(Files.readString(configPath), JsonObject.class);
                for (int r = 0; r < 5; r++)
                    for (int c = 0; c < 5; c++)
                        if (json.has("cell_" + r + "_" + c))
                            grid[r][c] = json.get("cell_" + r + "_" + c).getAsBoolean();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onEnable() {
        buildActive = false;
        blockTicks = 0;
        loopCount = 0;
        wasUsePressed = false;
        loadGridConfig();
    }

    @Override
    protected void onDisable() {
        saveGridConfig();
        buildActive = false;
        blockTicks = 0;
        loopCount = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean isUsePressed = mc.options.keyUse.isDown();
        if (isUsePressed && !wasUsePressed) {
            HitResult hit = mc.getCameraEntity().pick(range.getValue(), 0, false);
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                center = ((BlockHitResult) hit).getBlockPos();
                playerDir = mc.player.getDirection();
                playerPitch = mc.player.getXRot();
                buildActive = !buildActive;
                blockTicks = 0;
                loopCount = 0;
                if (!buildActive) saveGridConfig();
            }
        }
        wasUsePressed = isUsePressed;

        if (!buildActive || center == null) {
            blockTicks = 0;
            loopCount = 0;
            return;
        }

        if (mc.player.isDeadOrDying()) {
            buildActive = false;
            return;
        }

        blockTicks++;
        if (blockTicks > tickDelay.getValue().intValue() * 5) {
            blockTicks = 0;
            loopCount++;
        }
        if (loopCount >= placementLoops.getValue().intValue()) {
            buildActive = false;
            loopCount = 0;
            return;
        }

        String pat = pattern.getValue();
        if (!"Custom".equals(pat)) setPatternByName(pat);

        boolean horizontal;
        String orient = orientation.getValue();
        if ("Auto".equals(orient)) {
            horizontal = playerPitch > 40 || playerPitch < -40;
        } else {
            horizontal = "Horizontal".equals(orient);
        }

        if (!findValidBlock(mc)) return;

        int delay = tickDelay.getValue().intValue();
        for (int row = 0; row < 5; row++) {
            if (blockTicks != delay * (row + 1)) continue;
            for (int col = 0; col < 5; col++) {
                if (!grid[row][col]) continue;
                BlockPos pos = getBuildPos(row, col, horizontal);
                if (pos != null && mc.level.getBlockState(pos).canBeReplaced()) {
                    Vec3 hitVec = Vec3.atCenterOf(pos);
                    BlockHitResult bhr = new BlockHitResult(hitVec, Direction.DOWN, pos, false);
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
            }
        }
    }

    private BlockPos getBuildPos(int row, int col, boolean horizontal) {
        if (center == null) return null;
        if (horizontal) {
            return new BlockPos(center.getX() + col - 2, center.getY(), center.getZ() + row - 2);
        }
        return switch (playerDir) {
            case NORTH -> new BlockPos(center.getX() + col - 2, center.getY() + 2 - row, center.getZ());
            case SOUTH -> new BlockPos(center.getX() + 2 - col, center.getY() + 2 - row, center.getZ());
            case EAST  -> new BlockPos(center.getX(), center.getY() + 2 - row, center.getZ() + col - 2);
            case WEST  -> new BlockPos(center.getX(), center.getY() + 2 - row, center.getZ() + 2 - col);
            default -> null;
        };
    }

    private boolean findValidBlock(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (isValidBlock(stack)) {
                mc.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }

    private boolean isValidBlock(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem bi)) return false;
        if (stack.getItem() instanceof BedItem) return false;
        if (stack.getItem() instanceof SolidBucketItem) return false;
        if (stack.getItem() instanceof ScaffoldingBlockItem) return false;
        if (stack.getItem() instanceof DoubleHighBlockItem) return false;
        if (stack.getItem() instanceof StandingAndWallBlockItem) return false;
        if (stack.getItem() instanceof PlaceOnWaterBlockItem) return false;
        Block b = bi.getBlock();
        if (b instanceof VegetationBlock) return false;
        if (b instanceof TorchBlock) return false;
        if (b instanceof DiodeBlock) return false;
        if (b instanceof RedStoneWireBlock) return false;
        if (b instanceof FenceBlock) return false;
        if (b instanceof WallBlock) return false;
        if (b instanceof FenceGateBlock) return false;
        if (b instanceof FallingBlock) return false;
        if (b instanceof BaseRailBlock) return false;
        if (b instanceof SignBlock) return false;
        if (b instanceof BellBlock) return false;
        if (b instanceof CarpetBlock) return false;
        if (b instanceof ConduitBlock) return false;
        if (b instanceof CoralFanBlock) return false;
        if (b instanceof CoralWallFanBlock) return false;
        if (b instanceof BaseCoralFanBlock) return false;
        if (b instanceof BaseCoralWallFanBlock) return false;
        if (b instanceof TripWireHookBlock) return false;
        if (b instanceof PointedDripstoneBlock) return false;
        if (b instanceof TripWireBlock) return false;
        if (b instanceof PressurePlateBlock) return false;
        if (b instanceof FaceAttachedHorizontalDirectionalBlock) return false;
        if (b instanceof ShulkerBoxBlock) return false;
        if (b instanceof AmethystClusterBlock) return false;
        if (b instanceof BuddingAmethystBlock) return false;
        if (b instanceof ChorusFlowerBlock) return false;
        if (b instanceof ChorusPlantBlock) return false;
        if (b instanceof LanternBlock) return false;
        if (b instanceof CandleBlock) return false;
        if (b instanceof CakeBlock) return false;
        if (b instanceof SugarCaneBlock) return false;
        if (b instanceof SporeBlossomBlock) return false;
        if (b instanceof KelpBlock) return false;
        if (b instanceof GlowLichenBlock) return false;
        if (b instanceof CactusBlock) return false;
        if (b instanceof BambooStalkBlock) return false;
        if (b instanceof FlowerPotBlock) return false;
        if (b instanceof LadderBlock) return false;
        return true;
    }
}
