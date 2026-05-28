package ravex.modules;

import ravex.modules.combat.AimAssist;
import ravex.modules.combat.KillAura;
import ravex.modules.combat.MaceSwap;
import ravex.modules.combat.Trigger;
import ravex.modules.misc.AntiAfk;
import ravex.modules.misc.AutoEat;
import ravex.modules.misc.Optimizer;
import ravex.modules.misc.VisualRange;
import ravex.modules.movement.GuiWalk;
import ravex.modules.movement.NoSlowDown;
import ravex.modules.movement.Velocity;
import ravex.modules.player.AirPlace;
import ravex.modules.player.AutoTool;
import ravex.modules.player.NoInteract;
import ravex.modules.player.RichPresence;
import ravex.modules.player.SourceFiller;
import ravex.modules.render.Ambient;
import ravex.modules.render.ClickGui;
import ravex.modules.render.CustomFog;
import ravex.modules.render.ESP;
import ravex.modules.render.FreeCam;
import ravex.modules.render.FreeLook;
import ravex.modules.render.Hud;
import ravex.modules.render.NameTags;
import ravex.modules.render.NoBob;
import ravex.modules.render.Notifications;
import ravex.modules.render.Shaders;
import ravex.modules.world.BoneMeal;
import ravex.modules.world.Scaffold;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();

    private final List<Module> clickGuiModules = new ArrayList<>();
    private final List<HudModule> hudModules = new ArrayList<>();

    private ModuleManager() {
        // ── Combat ──────────────────────────────────────────────────────────────
        clickGuiModules.add(KillAura.INSTANCE);
        clickGuiModules.add(AimAssist.INSTANCE);
        clickGuiModules.add(Trigger.INSTANCE);
        clickGuiModules.add(MaceSwap.INSTANCE);

        // ── Render ──────────────────────────────────────────────────────────────
        clickGuiModules.add(ESP.INSTANCE);
        clickGuiModules.add(NameTags.INSTANCE);
        clickGuiModules.add(ClickGui.INSTANCE);
        clickGuiModules.add(Notifications.INSTANCE);
        clickGuiModules.add(NoBob.INSTANCE);
        clickGuiModules.add(Ambient.INSTANCE);
        clickGuiModules.add(CustomFog.INSTANCE);
        clickGuiModules.add(Hud.INSTANCE);
        clickGuiModules.add(Shaders.INSTANCE);
        clickGuiModules.add(FreeLook.INSTANCE);
        clickGuiModules.add(FreeCam.INSTANCE);

        // ── Player ──────────────────────────────────────────────────────────────
        clickGuiModules.add(AutoTool.INSTANCE);
        clickGuiModules.add(RichPresence.INSTANCE);
        clickGuiModules.add(NoInteract.INSTANCE);
        clickGuiModules.add(SourceFiller.INSTANCE);
        clickGuiModules.add(AirPlace.INSTANCE);

        // ── Movement ────────────────────────────────────────────────────────────
        clickGuiModules.add(GuiWalk.INSTANCE);
        clickGuiModules.add(NoSlowDown.INSTANCE);
        clickGuiModules.add(Velocity.INSTANCE);

        // ── Misc ─────────────────────────────────────────────────────────────────
        clickGuiModules.add(AntiAfk.INSTANCE);
        clickGuiModules.add(VisualRange.INSTANCE);
        clickGuiModules.add(Optimizer.INSTANCE);
        clickGuiModules.add(AutoEat.INSTANCE);

        // ── World ────────────────────────────────────────────────────────────────
        clickGuiModules.add(BoneMeal.INSTANCE);
        clickGuiModules.add(Scaffold.INSTANCE);

        // ── HUD modules ──────────────────────────────────────────────────────────
        hudModules.add(new HudModule("Watermark", 10, 10, 80, 14) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, float partialTicks) {
                if (!Hud.INSTANCE.getEnabled()) return;
                var font = net.minecraft.client.Minecraft.getInstance().font;
                graphics.drawString(font, "RaveX v1.0", getX(), getY(),
                        ravex.gui.clickgui.ColorUtility.getActiveColor(), true);
            }
        });

        hudModules.add(new HudModule("ActiveModules", 10, 30, 90, 100) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, float partialTicks) {
                if (!Hud.INSTANCE.getEnabled()) return;
                var font = net.minecraft.client.Minecraft.getInstance().font;
                int currentY = getY();
                for (Module m : clickGuiModules) {
                    if (m.getEnabled()) {
                        graphics.drawString(font, m.getName(), getX(), currentY, 0xFF8F8FA0, true);
                        currentY += 10;
                    }
                }
            }
        });

        hudModules.add(new HudModule("Coords", 10, 200, 140, 14) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, float partialTicks) {
                if (!Hud.INSTANCE.getEnabled()) return;
                var font = net.minecraft.client.Minecraft.getInstance().font;
                var player = net.minecraft.client.Minecraft.getInstance().player;
                if (player != null) {
                    String coordText = String.format("XYZ: %.1f / %.1f / %.1f",
                            player.getX(), player.getY(), player.getZ());
                    graphics.drawString(font, coordText, getX(), getY(), 0xFFD0D0E0, true);
                }
            }
        });

        hudModules.add(new HudModule("Fps", 10, 220, 60, 14) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, float partialTicks) {
                if (!Hud.INSTANCE.getEnabled()) return;
                var font = net.minecraft.client.Minecraft.getInstance().font;
                String fpsText = "FPS: " + net.minecraft.client.Minecraft.getInstance().getFps();
                graphics.drawString(font, fpsText, getX(), getY(), 0xFFD0D0E0, true);
            }
        });
    }

    public List<Module> getClickGuiModules() { return clickGuiModules; }
    public List<HudModule> getHudModules()   { return hudModules; }

    public List<Module> getByCategory(Category category) {
        List<Module> list = new ArrayList<>();
        for (Module m : clickGuiModules) {
            if (m.getCategory() == category) list.add(m);
        }
        return list;
    }

    public Module getByName(String name) {
        for (Module m : clickGuiModules) {
            if (m.getName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    public void init() {}

    public List<Module> getModules() { return clickGuiModules; }

    public void onTick() {
        for (Module m : clickGuiModules) {
            if (m.getEnabled()) m.onTick();
        }
    }
}
