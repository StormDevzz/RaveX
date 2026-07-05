package ravex.modules.player;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import ravex.modules.Module;
import ravex.parameter.*;
import ravex.utility.player.InventoryUtility;
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
import java.util.function.Predicate;
public class GridBuilder extends Module {
    public static final GridBuilder INSTANCE = new GridBuilder();
    public final NumberParameter range = new NumberParameter("Range", 3.0, 1.0, 5.0, 1.0);
    public final NumberParameter placementLoops = new NumberParameter("Loops", 3.0, 1.0, 10.0, 1.0);
    public final NumberParameter tickDelay = new NumberParameter("TickDelay", 1.0, 0.0, 10.0, 1.0);
    public final ModeParameter orientation = new ModeParameter("Orientation", "Auto", List.of("Auto", "Horizontal", "Vertical"));
    public final ModeParameter pattern = new ModeParameter("Pattern", "Pillar", List.of("Full", "Pillar", "Hollow", "Cross", "Custom"));
    private boolean[][] grid = new boolean[5][5];
    private Path configPath;
    private boolean buildActive = false;
    private BlockPos center;
    private Direction playerDir;
    private float playerPitch;
    private int blockTicks = 0, loopCount = 0;
    private boolean wasUsePressed = false;

    private GridBuilder() {
        super("GridBuilder");
        configPath = Paths.get("RaveX", "gridbuilder-grid.json");
        setDefaultPillar();
        loadGridConfig();
    }
    private void setDefaultPillar() {
        for (int r = 0; r < 5; r++) for (int c = 0; c < 5; c++) grid[r][c] = false;
        grid[1][1] = grid[1][2] = grid[1][3] = true;
        grid[2][2] = grid[3][2] = true;
    }
    private void setPatternByName(String name) {
        if ("Pillar".equals(name)) { setDefaultPillar(); return; }
        for (int r = 0; r < 5; r++) for (int c = 0; c < 5; c++)
            grid[r][c] = switch (name) {
                case "Full" -> true;
                case "Hollow" -> r == 0 || r == 4 || c == 0 || c == 4;
                case "Cross" -> r == 2 || c == 2;
                default -> false;
            };
    }
    private void saveGridConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            JsonObject json = new JsonObject();
            for (int r = 0; r < 5; r++) for (int c = 0; c < 5; c++)
                json.addProperty("cell_" + r + "_" + c, grid[r][c]);
            Files.write(configPath, new GsonBuilder().setPrettyPrinting().create().toJson(json).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) { e.printStackTrace(); }
    }
    private void loadGridConfig() {
        try {
            if (Files.exists(configPath)) {
                JsonObject json = new Gson().fromJson(Files.readString(configPath), JsonObject.class);
                for (int r = 0; r < 5; r++) for (int c = 0; c < 5; c++)
                    if (json.has("cell_" + r + "_" + c)) grid[r][c] = json.get("cell_" + r + "_" + c).getAsBoolean();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
    @Override
    protected void onEnable() { buildActive = false; blockTicks = 0; loopCount = 0; wasUsePressed = false; loadGridConfig(); }
    @Override
    protected void onDisable() { saveGridConfig(); buildActive = false; blockTicks = 0; loopCount = 0; }
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
                blockTicks = 0; loopCount = 0;
                if (!buildActive) saveGridConfig();
            }
        }
        wasUsePressed = isUsePressed;
        if (!buildActive || center == null) { blockTicks = 0; loopCount = 0; return; }
        if (mc.player.isDeadOrDying()) { buildActive = false; return; }
        blockTicks++;
        if (blockTicks > tickDelay.getValue().intValue() * 5) { blockTicks = 0; loopCount++; }
        if (loopCount >= placementLoops.getValue().intValue()) { buildActive = false; loopCount = 0; return; }
        String pat = pattern.getValue();
        if (!"Custom".equals(pat)) setPatternByName(pat);
        boolean horizontal;
        String orient = orientation.getValue();
        if ("Auto".equals(orient)) horizontal = playerPitch > 40 || playerPitch < -40;
        else horizontal = "Horizontal".equals(orient);
        if (InventoryUtility.findSlot(mc.player, VALID_BLOCK, 0, 9) < 0) return;
        int delay = tickDelay.getValue().intValue();
        for (int row = 0; row < 5; row++) {
            if (blockTicks != delay * (row + 1)) continue;
            for (int col = 0; col < 5; col++) {
                if (!grid[row][col]) continue;
                BlockPos pos = getBuildPos(row, col, horizontal);
                if (pos != null && mc.level.getBlockState(pos).canBeReplaced()) {
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.DOWN, pos, false));
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
            }
        }
    }
    private BlockPos getBuildPos(int row, int col, boolean horizontal) {
        if (center == null) return null;
        if (horizontal) return new BlockPos(center.getX() + col - 2, center.getY(), center.getZ() + row - 2);
        return switch (playerDir) {
            case NORTH -> new BlockPos(center.getX() + col - 2, center.getY() + 2 - row, center.getZ());
            case SOUTH -> new BlockPos(center.getX() + 2 - col, center.getY() + 2 - row, center.getZ());
            case EAST  -> new BlockPos(center.getX(), center.getY() + 2 - row, center.getZ() + col - 2);
            case WEST  -> new BlockPos(center.getX(), center.getY() + 2 - row, center.getZ() + 2 - col);
            default -> null;
        };
    }
    private static final Predicate<net.minecraft.world.item.ItemStack> VALID_BLOCK = stack -> {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) return false;
        return true;
    };
}
