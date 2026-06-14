package ravex.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;

public class AutoBrew extends Module {
    public static final AutoBrew INSTANCE = new AutoBrew();

    public final NumberParameter range = new NumberParameter("Range", 5.0, 1.0, 6.0, 0.5);
    public final BooleanParameter autoFuel = new BooleanParameter("Auto Fuel", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3FCC44FF);

    public static BlockPos currentTarget = null;

    private static final java.util.Set<net.minecraft.world.item.Item> INGREDIENTS = java.util.Set.of(
        Items.NETHER_WART, Items.GLOWSTONE_DUST, Items.REDSTONE, Items.GUNPOWDER,
        Items.FERMENTED_SPIDER_EYE, Items.BLAZE_POWDER, Items.GHAST_TEAR,
        Items.MAGMA_CREAM, Items.SUGAR, Items.RABBIT_FOOT, Items.GLISTERING_MELON_SLICE,
        Items.GOLDEN_CARROT, Items.SPIDER_EYE, Items.PUFFERFISH, Items.PHANTOM_MEMBRANE,
        Items.DRAGON_BREATH, Items.TURTLE_HELMET
    );

    private AutoBrew() {
        super("AutoBrew", Category.WORLD);
        addParameter(range);
        addParameter(autoFuel);
        addParameter(render);
        addParameter(color);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (!(mc.screen instanceof BrewingStandScreen)) {
            currentTarget = null;
            return;
        }

        if (currentTarget == null) {
            if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                BlockPos pos = ((net.minecraft.world.phys.BlockHitResult) mc.hitResult).getBlockPos();
                BlockState st = mc.level.getBlockState(pos);
                if (st.getBlock() instanceof net.minecraft.world.level.block.BrewingStandBlock) {
                    currentTarget = pos;
                }
            }
        }

        if (!(mc.player.containerMenu instanceof BrewingStandMenu brew)) {
            currentTarget = null;
            return;
        }

        int containerId = brew.containerId;
        int playerInvStart = 5;
        int hotbarStart = playerInvStart + 27;

        // Fuel slot (slot 4) - refill with blaze powder
        if (autoFuel.getValue() && brew.getFuel() <= 0) {
            if (!brew.getSlot(4).hasItem()) {
                for (int i = playerInvStart; i < brew.slots.size(); i++) {
                    var stack = brew.slots.get(i).getItem();
                    if (stack.is(Items.BLAZE_POWDER)) {
                        mc.gameMode.handleInventoryMouseClick(
                            containerId, i, 0, ClickType.QUICK_MOVE, mc.player
                        );
                        return;
                    }
                }
            }
        }

        // If currently brewing, wait
        if (brew.getBrewingTicks() > 0) return;

        // Take finished potions (slots 0-2)
        for (int slot = 0; slot <= 2; slot++) {
            var stack = brew.getSlot(slot).getItem();
            if (!stack.isEmpty() && !stack.is(Items.GLASS_BOTTLE)) {
                mc.gameMode.handleInventoryMouseClick(
                    containerId, slot, 0, ClickType.QUICK_MOVE, mc.player
                );
                return;
            }
        }

        // Add ingredient (slot 3)
        if (!brew.getSlot(3).hasItem()) {
            for (int i = playerInvStart; i < brew.slots.size(); i++) {
                var stack = brew.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (isIngredient(stack)) {
                    mc.gameMode.handleInventoryMouseClick(
                        containerId, i, 0, ClickType.QUICK_MOVE, mc.player
                    );
                    return;
                }
            }
            for (int i = hotbarStart; i < brew.slots.size(); i++) {
                var stack = brew.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (isIngredient(stack)) {
                    mc.gameMode.handleInventoryMouseClick(
                        containerId, i, 0, ClickType.QUICK_MOVE, mc.player
                    );
                    return;
                }
            }
        }

        // Add glass bottles (slots 0-2) if empty
        for (int slot = 0; slot <= 2; slot++) {
            if (!brew.getSlot(slot).hasItem()) {
                for (int i = playerInvStart; i < brew.slots.size(); i++) {
                    var stack = brew.slots.get(i).getItem();
                    if (stack.is(Items.GLASS_BOTTLE)) {
                        mc.gameMode.handleInventoryMouseClick(
                            containerId, i, 0, ClickType.QUICK_MOVE, mc.player
                        );
                        return;
                    }
                }
            }
        }
    }

    @Override
    protected void onDisable() {
        currentTarget = null;
    }

    private static boolean isIngredient(ItemStack stack) {
        return INGREDIENTS.contains(stack.getItem());
    }
}
