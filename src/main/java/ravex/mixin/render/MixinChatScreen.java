package ravex.mixin.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.manager.ModuleManager;
import ravex.modules.Modules;
import ravex.modules.client.Commands;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// theme colored chat input plus Tab completions for the client command list.
// the list looks like the vanilla one, tab cycles through it
@Mixin(ChatScreen.class)
public abstract class MixinChatScreen {
    @Shadow protected EditBox input;

    // vanilla values from CommandSuggestions
    @Unique
    private static final int RAVEX_LINE_HEIGHT = 12;
    @Unique
    private static final int RAVEX_LINE_LIMIT = 10;
    @Unique
    private static final int RAVEX_TEXT_COLOR = 0xFFAAAAAA;
    @Unique
    private static final int RAVEX_SELECTED_COLOR = 0xFFFFFF00;

    @Unique
    private int ravex$selected = 0;
    @Unique
    private List<Cmd> ravex$cached = new ArrayList<>();
    @Unique
    private List<String> ravex$subCached = new ArrayList<>();
    @Unique
    private boolean ravex$cycling = false;
    @Unique
    private boolean ravex$subMode = false;
    @Unique
    private float ravex$reveal = 0f;
    @Unique
    private long ravex$lastFrameNs = 0L;
    @Unique
    private double ravex$lastMouseX = Double.NaN;
    @Unique
    private double ravex$lastMouseY = Double.NaN;
    @Unique
    private String ravex$lastInput = "";

    @Unique
    private boolean ravex$prefixActive() {
        if (input == null) return false;
        if (!Modules.enabled(Commands.class)) return false;
        String prefix = ModuleManager.get(Commands.class).prefix;
        if (prefix == null || prefix.isEmpty()) return false;
        String value = input.getValue();
        return value.startsWith(prefix);
    }

    @Unique
    private List<Cmd> ravex$matches() {
        String prefix = ModuleManager.get(Commands.class).prefix;
        String value = input.getValue();
        if (value.endsWith(" ")) value = value.substring(0, value.length() - 1);
        String token = value.length() >= prefix.length() ? value.substring(prefix.length()).toLowerCase() : "";
        String[] parts = token.split("\\s+", 2);
        String cmdName = parts[0];

        if (parts.length == 2 && !parts[1].isEmpty()) {
            Cmd cmd = CmdReg.INSTANCE.getCommands().get(cmdName);
            if (cmd != null) {
                ravex$subMode = true;
                String subToken = parts[1];
                ravex$subCached = new ArrayList<>(cmd.getSubcommandSuggestions(new String[]{cmdName, subToken}));
                ravex$subCached.removeIf(s -> !s.toLowerCase().startsWith(subToken.toLowerCase()));
                // если введена полная сабкоманда, не показывать подсказки
                if (ravex$subCached.size() == 1 && ravex$subCached.get(0).equalsIgnoreCase(subToken)) {
                    ravex$subMode = false;
                    ravex$subCached.clear();
                }
                return new ArrayList<>();
            }
        }

        ravex$subMode = false;
        ravex$subCached.clear();

        Set<Cmd> unique = new LinkedHashSet<>(CmdReg.INSTANCE.getCommands().values());
        List<Cmd> out = new ArrayList<>();
        for (Cmd c : unique) {
            if (c.getName().toLowerCase().startsWith(token)) out.add(c);
        }
        return out;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void ravex$renderCommandHints(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (input == null) return;
        boolean active = ravex$prefixActive();
        input.setTextColor(active ? ColorUtility.getActiveColor() : EditBox.DEFAULT_TEXT_COLOR);
        if (active) {
            String currentInput = input.getValue();
            if (!currentInput.equals(ravex$lastInput)) {
                if (ravex$cycling) {
                    ravex$cycling = false;
                    ravex$subMode = false;
                }
                ravex$cached = ravex$matches();
                ravex$lastInput = currentInput;
            }
            if (ravex$subMode) {
                if (ravex$subCached.isEmpty()) ravex$cycling = false;
                if (ravex$selected >= ravex$subCached.size()) ravex$selected = 0;
            } else {
                if (ravex$cached.isEmpty()) ravex$cycling = false;
                if (ravex$selected >= ravex$cached.size()) ravex$selected = 0;
            }
        }

        boolean wantShow = active && (!ravex$subMode ? !ravex$cached.isEmpty() : !ravex$subCached.isEmpty());
        long now = System.nanoTime();
        float dt = ravex$lastFrameNs == 0L ? 0.016f : Math.min(0.05f, (now - ravex$lastFrameNs) / 1_000_000_000f);
        ravex$lastFrameNs = now;
        float target = wantShow ? 1f : 0f;
        ravex$reveal += (target - ravex$reveal) * Math.min(1f, dt * 24f);
        if (Math.abs(target - ravex$reveal) < 0.004f) ravex$reveal = target;
        if (ravex$reveal <= 0.01f) {
            ravex$reveal = 0f;
            if (!active) {
                ravex$cached = new ArrayList<>();
                ravex$subCached = new ArrayList<>();
            }
            return;
        }

        String prefix = ModuleManager.get(Commands.class).prefix;
        int total;
        if (ravex$subMode) total = ravex$subCached.size();
        else total = ravex$cached.size();
        int shown = Math.min(RAVEX_LINE_LIMIT, total);
        int first = Math.max(0, Math.min(ravex$selected - shown + 1, total - shown));
        if (first < 0) first = 0;

        int maxW = 0;
        for (int i = 0; i < shown; i++) {
            String label;
            if (ravex$subMode) label = ravex$subCached.get(first + i);
            else label = prefix + ravex$cached.get(first + i).getName();
            maxW = Math.max(maxW, FontRenderUtility.getStringWidth(label));
        }
        int boxW = maxW + 4;
        int boxX;
        if (ravex$subMode) {
            boxX = input.getScreenX(input.getCursorPosition()) + 2;
        } else {
            boxX = input.getScreenX(0) + 2;
        }
        int boxTop = input.getY() - shown * RAVEX_LINE_HEIGHT + 2;
        int screenW = ravex.mcwrapper.MinecraftWrapper.getWrapper().getScreenWidth();
        if (boxX + boxW > screenW) boxX = Math.max(2, screenW - boxW);
        if (boxTop < 2) boxTop = 2;

        float a = ravex$reveal;
        int fillColor = ravex.mcwrapper.MinecraftWrapper.getWrapper()
            .getOptions().getBackgroundColor(0.6F);
        if (((fillColor >>> 24) & 0xFF) < 0x38) fillColor = 0x60000000;
        fillColor = ColorUtility.applyAlpha(fillColor, a);
        int normalText = ColorUtility.applyAlpha(RAVEX_TEXT_COLOR, a);
        int selectedText = ColorUtility.applyAlpha(RAVEX_SELECTED_COLOR, a);
        boolean hasUp = first > 0;
        boolean hasDown = first + shown < total;

        // vanilla scroll strips and arrows
        if (hasUp || hasDown) {
            graphics.fill(boxX, boxTop - 1, boxX + boxW, boxTop, fillColor);
            graphics.fill(boxX, boxTop + shown * RAVEX_LINE_HEIGHT, boxX + boxW,
                boxTop + shown * RAVEX_LINE_HEIGHT + 1, fillColor);
        }
        if (hasUp) {
            int dash = ColorUtility.applyAlpha(0xFFFFFFFF, a);
            for (int i = 0; i < boxW; i += 2) {
                graphics.fill(boxX + i, boxTop - 1, boxX + i + 1, boxTop, dash);
            }
        }
        if (hasDown) {
            int dash = ColorUtility.applyAlpha(0xFFFFFFFF, a);
            for (int i = 0; i < boxW; i += 2) {
                graphics.fill(boxX + i, boxTop + shown * RAVEX_LINE_HEIGHT,
                    boxX + i + 1, boxTop + shown * RAVEX_LINE_HEIGHT + 1, dash);
            }
        }

        boolean mouseMoved = mouseX != ravex$lastMouseX || mouseY != ravex$lastMouseY;
        ravex$lastMouseX = mouseX;
        ravex$lastMouseY = mouseY;

        for (int i = 0; i < shown; i++) {
            int rowY = boxTop + i * RAVEX_LINE_HEIGHT;
            graphics.fill(boxX, rowY, boxX + boxW, rowY + RAVEX_LINE_HEIGHT, fillColor);

            if (mouseMoved && mouseX > boxX && mouseX < boxX + boxW
                && mouseY > rowY && mouseY < rowY + RAVEX_LINE_HEIGHT) {
                ravex$selected = first + i;
            }

            boolean selected = (first + i) == ravex$selected;
            String label;
            if (ravex$subMode) label = ravex$subCached.get(first + i);
            else label = prefix + ravex$cached.get(first + i).getName();
            FontRenderUtility.drawString(graphics,
                label,
                boxX + 1, rowY + 2,
                selected ? selectedText : normalText, false);
        }

        Render2DUtility.drawRoundBorder(graphics, boxX - 1, boxTop - 1, boxW + 2,
            shown * RAVEX_LINE_HEIGHT + 2, 3, 1,
            ColorUtility.applyAlpha(ColorUtility.getActiveColor(), a));
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void ravex$tabComplete(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (input == null) return;
        if (!Modules.enabled(Commands.class)) return;
        String prefix = ModuleManager.get(Commands.class).prefix;
        if (prefix == null || prefix.isEmpty()) return;

        if (event.key() == GLFW.GLFW_KEY_TAB) {
            if (!input.getValue().startsWith(prefix)) return;
            String value = input.getValue();

            if (!ravex$cycling) {
                // First press: compute suggestions and insert the first one
                boolean hadTrailingSpace = value.endsWith(" ");
                String analyzeValue = hadTrailingSpace ? value.substring(0, value.length() - 1) : value;
                String afterPrefix = analyzeValue.length() >= prefix.length()
                    ? analyzeValue.substring(prefix.length()).trim()
                    : "";
                String[] parts = afterPrefix.split("\\s+", 2);
                String cmdName = parts[0];
                Cmd cmd = CmdReg.INSTANCE.getCommands().get(cmdName);

                ravex$subMode = false;
                ravex$subCached.clear();

                if (cmd != null && (hadTrailingSpace || (parts.length == 2 && !parts[1].isEmpty()))) {
                    ravex$subMode = true;
                    String subToken = parts.length == 2 ? parts[1] : "";
                    ravex$subCached = new ArrayList<>(cmd.getSubcommandSuggestions(new String[]{cmdName, subToken}));
                    ravex$subCached.removeIf(s -> !s.toLowerCase().startsWith(subToken.toLowerCase()));
                    // если введена полная сабкоманда, не показывать подсказки
                    if (ravex$subCached.size() == 1 && ravex$subCached.get(0).equalsIgnoreCase(subToken)) {
                        ravex$subMode = false;
                        ravex$subCached.clear();
                        return;
                    }
                } else {
                    ravex$cached = new ArrayList<>(CmdReg.INSTANCE.getCommands().values());
                    String token = cmdName.toLowerCase();
                    ravex$cached.removeIf(c -> !c.getName().toLowerCase().startsWith(token));
                }

                int total = ravex$subMode ? ravex$subCached.size() : ravex$cached.size();
                if (total == 0) {
                    ravex$cycling = false;
                    ravex$subMode = false;
                    return;
                }

                ravex$cycling = true;
                ravex$selected = 0;

                if (ravex$subMode) {
                    int lastSpace = value.lastIndexOf(' ');
                    String base = value.substring(0, lastSpace + 1);
                    input.setValue(base + ravex$subCached.get(0));
                } else {
                    input.setValue(prefix + ravex$cached.get(0).getName());
                }
                input.moveCursor(999, false);
                ravex$lastInput = input.getValue();
                cir.cancel();
                return;
            }

            // Subsequent presses: cycle through the list and insert each one
            int size = ravex$subMode ? ravex$subCached.size() : ravex$cached.size();
            if (size == 0) {
                ravex$cycling = false;
                ravex$subMode = false;
                return;
            }
            if (size > 1) {
                int step = org.lwjgl.glfw.GLFW.glfwGetKey(
                    ravex.mcwrapper.MinecraftWrapper.getWrapper().getWindowHandle(),
                    GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ? -1 : 1;
                ravex$selected = Math.floorMod(ravex$selected + step, size);
            }

            if (ravex$subMode) {
                int lastSpace = value.lastIndexOf(' ');
                String base = value.substring(0, lastSpace + 1);
                input.setValue(base + ravex$subCached.get(ravex$selected));
            } else {
                input.setValue(prefix + ravex$cached.get(ravex$selected).getName());
            }
            input.moveCursor(999, false);
            ravex$lastInput = input.getValue();
            cir.cancel();
            return;
        }

        // arrow keys for navigation while cycling
        if (ravex$cycling && (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN)) {
            int size = ravex$subMode ? ravex$subCached.size() : ravex$cached.size();
            if (size > 0) {
                int step = event.key() == GLFW.GLFW_KEY_UP ? -1 : 1;
                ravex$selected = Math.floorMod(ravex$selected + step, size);
                String value2 = input.getValue();
                if (ravex$subMode) {
                    int lastSpace = value2.lastIndexOf(' ');
                    String base = value2.substring(0, lastSpace + 1);
                    input.setValue(base + ravex$subCached.get(ravex$selected));
                } else {
                    input.setValue(prefix + ravex$cached.get(ravex$selected).getName());
                }
                input.moveCursor(999, false);
                ravex$lastInput = input.getValue();
                cir.cancel();
                return;
            }
        }

        // Space or Enter while cycling: confirm the selection
        if (ravex$cycling && (event.key() == GLFW.GLFW_KEY_SPACE || event.key() == GLFW.GLFW_KEY_ENTER)) {
            ravex$cycling = false;
            ravex$subMode = false;
            cir.cancel();
            return;
        }

        // any other key ends the tab cycle
        ravex$cycling = false;
        ravex$subMode = false;
        ravex$lastInput = input.getValue();
    }
}
