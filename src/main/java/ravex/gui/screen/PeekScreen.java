package ravex.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.lwjgl.glfw.GLFW;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.mixin.player.AccessorContainerScreen;
import ravex.utility.misc.LanguageUtility;

import java.util.List;

public class PeekScreen extends ShulkerBoxScreen {
    private final Screen parentScreen;

    public PeekScreen(ShulkerBoxMenu menu, Inventory inventory, Component title, Screen parentScreen) {
        super(menu, inventory, title);
        this.parentScreen = parentScreen;
    }

    public static PeekScreen create(ItemStack shulkerStack, Inventory playerInventory, Screen parent) {
        SimpleContainer container = new SimpleContainer(27);
        ItemContainerContents contents = shulkerStack.get(DataComponents.CONTAINER);
        if (contents != null) {
            List<ItemStack> list = contents.stream().toList();
            for (int i = 0; i < Math.min(list.size(), 27); i++) {
                container.setItem(i, list.get(i));
            }
        }
        return new PeekScreen(new ShulkerBoxMenu(0, playerInventory, container), playerInventory, shulkerStack.getHoverName(), parent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            Slot hovered = ((AccessorContainerScreen) this).invokeGetHoveredSlot(event.x(), event.y());
            if (hovered != null && hovered.hasItem()) {
                ItemStack stack = hovered.getItem();
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents != null && !contents.stream().toList().isEmpty()) {
                    var mc = MinecraftWrapper.getInstance();
                    if (mc.player != null) {
                        mc.setScreen(PeekScreen.create(stack, mc.player.getInventory(), this));
                    }
                }
            }
        }
        // Never pass clicks to the slots, this screen is a read-only preview.
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return true;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        // Read-only: ignore every slot interaction.
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_E) {
            MinecraftWrapper.getInstance().setScreen(parentScreen);
        }
        // Swallow hotbar/drop keys so items cannot be moved from the preview.
        return true;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, "\u00a77" + LanguageUtility.t("shulker_peek_readonly"),
                this.width / 2, this.topPos - 12, 0xFFAAAAAA);
    }
}
