package ravex.modules;

import ravex.modules.combat.KillAura;
import ravex.modules.render.ESP;
import ravex.modules.player.AutoTool;
import ravex.modules.misc.AntiAfk;
import ravex.modules.world.BoneMeal;
import ravex.modules.world.Scaffold;
import ravex.modules.render.ClickGui;
import ravex.modules.render.Notifications;
import ravex.modules.misc.VisualRange;
import ravex.modules.render.NoBob;
import ravex.modules.render.Ambient;
import ravex.modules.combat.AimAssist;
import ravex.modules.render.CustomFog;
import ravex.modules.render.NameTags;
import ravex.modules.combat.Trigger;
import ravex.modules.combat.MaceSwap;
import ravex.modules.render.Hud;
import ravex.modules.misc.Optimizer;
import ravex.modules.misc.AutoEat;
import ravex.modules.player.RichPresence;
import ravex.modules.player.NoInteract;
import ravex.modules.player.SourceFiller;
import ravex.modules.player.AirPlace;
import ravex.modules.player.AutoArmor;
import ravex.modules.player.AutoMend;
import ravex.modules.player.FastUse;
import ravex.modules.player.AutoRespawn;
import ravex.modules.movement.GuiWalk;
import ravex.modules.movement.NoSlowDown;
import ravex.modules.movement.Velocity;
import ravex.modules.misc.PacketLogger;
import ravex.modules.movement.LongJump;
import ravex.modules.misc.AutoLog;
import ravex.modules.combat.WebAura;
import ravex.modules.combat.AutoWeapon;
import ravex.modules.combat.MaceAura;
import ravex.modules.player.Offhand;
import ravex.modules.player.MainHand;
import ravex.modules.combat.Hitboxes;
import ravex.modules.movement.Sleepy;
import ravex.modules.combat.Reach;
import ravex.modules.movement.NoPush;
import ravex.modules.movement.AutoSprint;
import ravex.modules.movement.Spider;
import ravex.modules.player.FastBreak;
import ravex.modules.player.InstaBreak;

import ravex.modules.render.FreeCam;
import ravex.modules.render.FreeLook;
import ravex.modules.render.Shaders;
import ravex.modules.render.Glint;
import ravex.modules.render.Sounds;
import ravex.modules.render.ViewClip;
import ravex.modules.render.BlockOutline;
import ravex.modules.render.MobOwner;
import ravex.modules.movement.Speed;
import ravex.modules.movement.NoRotate;
import ravex.modules.combat.SelfTrap;
import ravex.modules.exploit.Timer;
import ravex.modules.exploit.ClickTP;
import ravex.modules.exploit.ClickFly;
import ravex.modules.exploit.ChorusExploit;
import ravex.modules.exploit.TickShift;
import ravex.modules.exploit.TridentBoost;
import ravex.modules.player.Xray;
import ravex.modules.combat.Surround;
import ravex.modules.combat.WindAura;
import ravex.modules.render.BreadCrumbs;

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
        clickGuiModules.add(WebAura.INSTANCE);
        clickGuiModules.add(AutoWeapon.INSTANCE);
        clickGuiModules.add(MaceAura.INSTANCE);
        clickGuiModules.add(Hitboxes.INSTANCE);
        clickGuiModules.add(Offhand.INSTANCE);
        clickGuiModules.add(MainHand.INSTANCE);
        clickGuiModules.add(Reach.INSTANCE);
        clickGuiModules.add(Surround.INSTANCE);
        clickGuiModules.add(SelfTrap.INSTANCE);
        clickGuiModules.add(WindAura.INSTANCE);

        // ── Render ──────────────────────────────────────────────────────────────
        clickGuiModules.add(ESP.INSTANCE);
        clickGuiModules.add(MobOwner.INSTANCE);
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
        clickGuiModules.add(ViewClip.INSTANCE);
        clickGuiModules.add(Glint.INSTANCE);
        clickGuiModules.add(Sounds.INSTANCE);
        clickGuiModules.add(ravex.modules.render.ItemPhysics.INSTANCE);
        clickGuiModules.add(ravex.modules.render.Fullbright.INSTANCE);
        clickGuiModules.add(BlockOutline.INSTANCE);
        clickGuiModules.add(Xray.INSTANCE);
        clickGuiModules.add(BreadCrumbs.INSTANCE);

        // ── Player ──────────────────────────────────────────────────────────────
        clickGuiModules.add(AutoTool.INSTANCE);
        clickGuiModules.add(NoInteract.INSTANCE);
        clickGuiModules.add(SourceFiller.INSTANCE);
        clickGuiModules.add(AirPlace.INSTANCE);
        clickGuiModules.add(AutoArmor.INSTANCE);
        clickGuiModules.add(AutoMend.INSTANCE);
        clickGuiModules.add(FastUse.INSTANCE);
        clickGuiModules.add(AutoRespawn.INSTANCE);
        clickGuiModules.add(FastBreak.INSTANCE);
        clickGuiModules.add(InstaBreak.INSTANCE);
        clickGuiModules.add(ravex.modules.player.ExtraTab.INSTANCE);
        clickGuiModules.add(ravex.modules.player.ExtraChest.INSTANCE);
        clickGuiModules.add(ravex.modules.player.ExtraChat.INSTANCE);

        // ── Movement ────────────────────────────────────────────────────────────
        clickGuiModules.add(GuiWalk.INSTANCE);
        clickGuiModules.add(NoSlowDown.INSTANCE);
        clickGuiModules.add(Velocity.INSTANCE);
        clickGuiModules.add(ravex.modules.movement.Step.INSTANCE);
        clickGuiModules.add(ravex.modules.movement.ReverseStep.INSTANCE);
        clickGuiModules.add(ravex.modules.movement.NoWeb.INSTANCE);
        clickGuiModules.add(ravex.modules.movement.AutoWalk.INSTANCE);
        clickGuiModules.add(ravex.modules.movement.AntiVoid.INSTANCE);
        clickGuiModules.add(LongJump.INSTANCE);
        clickGuiModules.add(Sleepy.INSTANCE);
        clickGuiModules.add(NoPush.INSTANCE);
        clickGuiModules.add(AutoSprint.INSTANCE);
        clickGuiModules.add(Spider.INSTANCE);
        clickGuiModules.add(Speed.INSTANCE);
        clickGuiModules.add(NoRotate.INSTANCE);
        clickGuiModules.add(ravex.modules.movement.Avoid.INSTANCE);

        // ── Misc ─────────────────────────────────────────────────────────────────
        clickGuiModules.add(AntiAfk.INSTANCE);
        clickGuiModules.add(VisualRange.INSTANCE);
        clickGuiModules.add(Optimizer.INSTANCE);
        clickGuiModules.add(AutoEat.INSTANCE);
        clickGuiModules.add(PacketLogger.INSTANCE);
        clickGuiModules.add(AutoLog.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.Spammer.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.Commands.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.WaxAura.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.AutoReconnect.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.ItemScroller.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.BlockSelector.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.AutoSoup.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.NameProtect.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.StashFinder.INSTANCE);

        // ── World ────────────────────────────────────────────────────────────────
        clickGuiModules.add(BoneMeal.INSTANCE);
        clickGuiModules.add(Scaffold.INSTANCE);
        clickGuiModules.add(ravex.modules.exploit.AntiHunger.INSTANCE);
        clickGuiModules.add(ravex.modules.world.AutoSign.INSTANCE);
        clickGuiModules.add(ravex.modules.world.AutoShear.INSTANCE);
        clickGuiModules.add(ravex.modules.world.AutoNameTag.INSTANCE);
        clickGuiModules.add(ravex.modules.world.AutoMount.INSTANCE);
        clickGuiModules.add(ravex.modules.world.FakePlayer.INSTANCE);
        clickGuiModules.add(ravex.modules.world.ChestAura.INSTANCE);

        // ── Exploit ──────────────────────────────────────────────────────────────
        clickGuiModules.add(ravex.modules.exploit.RideExploit.INSTANCE);
        clickGuiModules.add(ravex.modules.exploit.PacketCanceller.INSTANCE);
        clickGuiModules.add(ravex.modules.exploit.HandshakeSpoof.INSTANCE);
        clickGuiModules.add(ravex.modules.exploit.GhostHand.INSTANCE);
        clickGuiModules.add(Timer.INSTANCE);
        clickGuiModules.add(ravex.modules.exploit.PortalGui.INSTANCE);
        clickGuiModules.add(ravex.modules.exploit.MultiTask.INSTANCE);
        clickGuiModules.add(ClickTP.INSTANCE);
        clickGuiModules.add(ClickFly.INSTANCE);
        clickGuiModules.add(ChorusExploit.INSTANCE);
        clickGuiModules.add(TickShift.INSTANCE);
        clickGuiModules.add(TridentBoost.INSTANCE);


        // ── Client ────────────────────────────────────────────────────────────────
        clickGuiModules.add(RichPresence.INSTANCE);
        clickGuiModules.add(ravex.modules.client.GuiParticles.INSTANCE);
        clickGuiModules.add(ravex.modules.client.FastLatency.INSTANCE);

        // ── HUD modules ──────────────────────────────────────────────────────────
        hudModules.add(new HudModule("Watermark", 10, 10, 80, 14) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, float partialTicks) {
                if (!Hud.INSTANCE.getEnabled()) return;
                ravex.utility.render.FontRenderUtility.drawString(graphics, "RaveX v1.0", getX(), getY(),
                        ravex.gui.clickgui.ColorUtility.getActiveColor(), true);
            }
        });

        hudModules.add(new HudModule("ActiveModules", 10, 30, 90, 100) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, float partialTicks) {
                if (!Hud.INSTANCE.getEnabled()) return;
                int currentY = getY();
                for (Module m : clickGuiModules) {
                    if (m.getEnabled()) {
                        ravex.utility.render.FontRenderUtility.drawString(graphics, m.getName(), getX(), currentY, 0xFF8F8FA0, true);
                        currentY += 10;
                    }
                }
            }
        });

        hudModules.add(new HudModule("Coords", 10, 200, 140, 14) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, float partialTicks) {
                if (!Hud.INSTANCE.getEnabled()) return;
                var player = net.minecraft.client.Minecraft.getInstance().player;
                if (player != null) {
                    String coordText = String.format("XYZ: %.1f / %.1f / %.1f",
                            player.getX(), player.getY(), player.getZ());
                    ravex.utility.render.FontRenderUtility.drawString(graphics, coordText, getX(), getY(), 0xFFD0D0E0, true);
                }
            }
        });

        hudModules.add(new HudModule("Fps", 10, 220, 60, 14) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, float partialTicks) {
                if (!Hud.INSTANCE.getEnabled()) return;
                String fpsText = "FPS: " + net.minecraft.client.Minecraft.getInstance().getFps();
                ravex.utility.render.FontRenderUtility.drawString(graphics, fpsText, getX(), getY(), 0xFFD0D0E0, true);
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
