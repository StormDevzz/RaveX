package ravex.modules;

import ravex.modules.combat.KillAura;
import ravex.modules.combat.AutoCrystal;
import ravex.modules.combat.TntAura;
import ravex.modules.render.ESP;
import ravex.modules.render.Skeleton;

import ravex.modules.esp.Borders;
import ravex.modules.esp.Tunnels;
import ravex.modules.esp.Zoom;
import ravex.modules.esp.HoleESP;
import ravex.modules.esp.Blur;
import ravex.modules.esp.ToolTips;
import ravex.modules.esp.VoidESP;
import ravex.modules.player.AutoTool;
import ravex.modules.player.MiddleClick;
import ravex.modules.misc.AntiAfk;
import ravex.modules.world.BoneMeal;
import ravex.modules.world.Scaffold;
import ravex.modules.render.ClickGui;
import ravex.modules.client.Fonts;
import ravex.modules.client.Notifications;
import ravex.modules.client.DesktopGui;
import ravex.modules.client.Settings;
import ravex.modules.client.Calculator;

import ravex.modules.player.InventoryCleaner;

import ravex.modules.misc.VisualRange;
import ravex.modules.render.NoBob;
import ravex.modules.render.Ambient;
import ravex.modules.render.Weather;
import ravex.modules.combat.AimAssist;
import ravex.modules.render.CustomFog;
import ravex.modules.render.NameTags;
import ravex.modules.render.Tracers;
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
import ravex.modules.player.ElytraSwap;
import ravex.modules.player.ElytraReplace;
import ravex.modules.player.ViewLock;
import ravex.modules.player.ToolSaver;
import ravex.modules.player.AntiAim;
import ravex.modules.movement.ElytraFly;
import ravex.modules.movement.Flight;
import ravex.modules.movement.GuiWalk;
import ravex.modules.movement.NoSlowDown;
import ravex.modules.movement.Velocity;
import ravex.modules.misc.PacketLogger;
import ravex.modules.movement.LongJump;
import ravex.modules.misc.AutoLog;
import ravex.modules.render.KillEffects;
import ravex.modules.misc.AntiQuit;
import ravex.modules.misc.CustomDeathText;
import ravex.modules.misc.NoNarrator;
import ravex.modules.misc.NoPacketKick;
import ravex.modules.misc.SoundBlocker;
import ravex.modules.misc.Announcer;
import ravex.modules.misc.Welcomer;
import ravex.modules.misc.CoordLogger;
import ravex.modules.combat.WebAura;
import ravex.modules.combat.WitherRoseAura;
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
import ravex.modules.movement.KeepSprint;
import ravex.modules.movement.SafeWalk;
import ravex.modules.player.FastBreak;
import ravex.modules.player.InstaBreak;

import ravex.modules.render.FreeCam;
import ravex.modules.render.FreeLook;
import ravex.modules.render.Shaders;
import ravex.modules.render.Glint;
import ravex.modules.render.Sounds;
import ravex.modules.render.ViewClip;
import ravex.modules.render.BlockOutline;
import ravex.modules.player.MobOwner;
import ravex.modules.render.ShiftInterp;
import ravex.modules.render.BabyDude;
import ravex.modules.render.SkyColor;
import ravex.modules.render.CloudColor;
import ravex.modules.render.Trails;
import ravex.modules.render.Waypoint;
import ravex.modules.render.AspectRatio;
import ravex.modules.movement.Speed;
import ravex.modules.movement.NoRotate;
import ravex.modules.movement.LiquidCollision;
import ravex.modules.movement.HighJump;
import ravex.modules.movement.FastStairs;
import ravex.modules.movement.NoFall;
import ravex.modules.combat.SelfTrap;

import ravex.modules.combat.BasePlace;
import ravex.modules.combat.AnchorAura;
import ravex.modules.combat.BowAim;
import ravex.modules.combat.Breaker;
import ravex.modules.combat.Quiver;
import ravex.modules.combat.AutoClicker;
import ravex.modules.combat.AntiBot;
import ravex.modules.combat.AutoDrop;
import ravex.modules.combat.SelfFill;
import ravex.modules.combat.Burrow;

import ravex.modules.exploit.Timer;
import ravex.modules.exploit.ClickTP;
import ravex.modules.exploit.ClickFly;
import ravex.modules.exploit.ChorusExploit;
import ravex.modules.exploit.TickShift;
import ravex.modules.exploit.TridentBoost;
import ravex.modules.exploit.FakePearl;
import ravex.modules.player.Xray;
import ravex.modules.player.Replenish;
import ravex.modules.player.NoSwing;
import ravex.modules.player.Swing;
import ravex.modules.combat.Surround;
import ravex.modules.combat.WindAura;
import ravex.modules.combat.Trap;
import ravex.modules.combat.AutoApple;
import ravex.modules.combat.WebSelf;
import ravex.modules.combat.KeyPearl;
import ravex.modules.combat.Criticals;
import ravex.modules.combat.AutoBow;
import ravex.modules.combat.HoleFill;
import ravex.modules.exploit.PacketMine;
import ravex.modules.exploit.PacketPlace;
import ravex.modules.exploit.PingSpoof;
import ravex.modules.world.TreeCutter;
import ravex.modules.world.WitherBuild;
import ravex.modules.world.AutoLight;
import ravex.modules.world.AutoReplant;
import ravex.modules.world.Nuker;
import ravex.modules.world.AutoTrade;
import ravex.modules.world.AutoFish;
import ravex.modules.world.AutoTunnel;
import ravex.modules.world.AutoSmelt;
import ravex.modules.world.AutoBrew;
import ravex.modules.player.ECFarmer;
import ravex.modules.world.NoGhostBlocks;
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
        clickGuiModules.add(AutoCrystal.INSTANCE);
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
        clickGuiModules.add(BasePlace.INSTANCE);
        clickGuiModules.add(AnchorAura.INSTANCE);
        clickGuiModules.add(BowAim.INSTANCE);
        clickGuiModules.add(Breaker.INSTANCE);
        clickGuiModules.add(Quiver.INSTANCE);

        clickGuiModules.add(WindAura.INSTANCE);
        clickGuiModules.add(Trap.INSTANCE);
        clickGuiModules.add(AutoApple.INSTANCE);
        clickGuiModules.add(WebSelf.INSTANCE);
        clickGuiModules.add(KeyPearl.INSTANCE);
        clickGuiModules.add(ravex.modules.combat.NoHitDelay.INSTANCE);
        clickGuiModules.add(ravex.modules.combat.AntiPearl.INSTANCE);
        clickGuiModules.add(ravex.modules.combat.BedBomb.INSTANCE);
        clickGuiModules.add(Criticals.INSTANCE);
        clickGuiModules.add(AutoBow.INSTANCE);
        clickGuiModules.add(HoleFill.INSTANCE);
        clickGuiModules.add(AutoClicker.INSTANCE);
        clickGuiModules.add(AntiBot.INSTANCE);
        clickGuiModules.add(AutoDrop.INSTANCE);
        clickGuiModules.add(SelfFill.INSTANCE);
        clickGuiModules.add(Burrow.INSTANCE);
        clickGuiModules.add(PacketMine.INSTANCE);
        clickGuiModules.add(ravex.modules.combat.AntiReGear.INSTANCE);
        clickGuiModules.add(TntAura.INSTANCE);
        clickGuiModules.add(WitherRoseAura.INSTANCE);

        // ── Render ──────────────────────────────────────────────────────────────
        clickGuiModules.add(ESP.INSTANCE);
        clickGuiModules.add(Skeleton.INSTANCE);
        clickGuiModules.add(NameTags.INSTANCE);
        clickGuiModules.add(Tracers.INSTANCE);
        clickGuiModules.add(ClickGui.INSTANCE);
        clickGuiModules.add(NoBob.INSTANCE);
        clickGuiModules.add(Ambient.INSTANCE);
        clickGuiModules.add(Weather.INSTANCE);
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
        clickGuiModules.add(ravex.modules.render.ViewModel.INSTANCE);
        clickGuiModules.add(ravex.modules.render.NoRender.INSTANCE);
        clickGuiModules.add(ShiftInterp.INSTANCE);
        clickGuiModules.add(BabyDude.INSTANCE);
        clickGuiModules.add(SkyColor.INSTANCE);
        clickGuiModules.add(CloudColor.INSTANCE);
        clickGuiModules.add(Trails.INSTANCE);
        clickGuiModules.add(Waypoint.INSTANCE);
        clickGuiModules.add(KillEffects.INSTANCE);
        clickGuiModules.add(AspectRatio.INSTANCE);

        clickGuiModules.add(Borders.INSTANCE);
        clickGuiModules.add(Tunnels.INSTANCE);
        clickGuiModules.add(Zoom.INSTANCE);
        clickGuiModules.add(HoleESP.INSTANCE);
        clickGuiModules.add(Blur.INSTANCE);
        clickGuiModules.add(ToolTips.INSTANCE);
        clickGuiModules.add(VoidESP.INSTANCE);

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
        clickGuiModules.add(MiddleClick.INSTANCE);
        clickGuiModules.add(ElytraSwap.INSTANCE);
        clickGuiModules.add(ElytraReplace.INSTANCE);
        clickGuiModules.add(ViewLock.INSTANCE);
        clickGuiModules.add(ToolSaver.INSTANCE);
        clickGuiModules.add(AntiAim.INSTANCE);
        clickGuiModules.add(ravex.modules.player.AutoEZ.INSTANCE);
        clickGuiModules.add(InventoryCleaner.INSTANCE);
        clickGuiModules.add(Replenish.INSTANCE);
        clickGuiModules.add(MobOwner.INSTANCE);
        clickGuiModules.add(NoSwing.INSTANCE);
        clickGuiModules.add(Swing.INSTANCE);
        clickGuiModules.add(ravex.modules.player.AutoReGear.AutoReGear.INSTANCE);

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
        clickGuiModules.add(ravex.modules.movement.ExtraRocket.INSTANCE);
        clickGuiModules.add(ravex.modules.movement.ExtraRiding.INSTANCE);
        clickGuiModules.add(KeepSprint.INSTANCE);
        clickGuiModules.add(SafeWalk.INSTANCE);
        clickGuiModules.add(HighJump.INSTANCE);
        clickGuiModules.add(FastStairs.INSTANCE);
        clickGuiModules.add(NoFall.INSTANCE);
        clickGuiModules.add(ElytraFly.INSTANCE);
        clickGuiModules.add(Flight.INSTANCE);
        clickGuiModules.add(LiquidCollision.INSTANCE);


        // ── Misc ─────────────────────────────────────────────────────────────────
        clickGuiModules.add(AntiAfk.INSTANCE);
        clickGuiModules.add(VisualRange.INSTANCE);
        clickGuiModules.add(Optimizer.INSTANCE);
        clickGuiModules.add(AutoEat.INSTANCE);
        clickGuiModules.add(PacketLogger.INSTANCE);
        clickGuiModules.add(AutoLog.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.Spammer.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.DurabAlert.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.AntiAttack.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.LagNotify.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.PopCounter.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.AntiBookBan.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.WaxAura.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.AutoReconnect.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.ItemScroller.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.BlockSelector.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.AutoSoup.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.NameProtect.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.StashFinder.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.AutoAuth.INSTANCE);
        clickGuiModules.add(AntiQuit.INSTANCE);
        clickGuiModules.add(CustomDeathText.INSTANCE);
        clickGuiModules.add(NoNarrator.INSTANCE);
        clickGuiModules.add(NoPacketKick.INSTANCE);
        clickGuiModules.add(SoundBlocker.INSTANCE);
        clickGuiModules.add(Announcer.INSTANCE);
        clickGuiModules.add(Welcomer.INSTANCE);
        clickGuiModules.add(CoordLogger.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.BookFill.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.ExtraBook.INSTANCE);

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
        clickGuiModules.add(ravex.modules.world.Igniter.INSTANCE);
        clickGuiModules.add(TreeCutter.INSTANCE);
        clickGuiModules.add(ravex.modules.world.AutoTame.INSTANCE);
        clickGuiModules.add(AutoLight.INSTANCE);
        clickGuiModules.add(AutoReplant.INSTANCE);
        clickGuiModules.add(Nuker.INSTANCE);
        clickGuiModules.add(AutoTrade.INSTANCE);
        clickGuiModules.add(AutoFish.INSTANCE);
        clickGuiModules.add(AutoTunnel.INSTANCE);
        clickGuiModules.add(AutoSmelt.INSTANCE);
        clickGuiModules.add(AutoBrew.INSTANCE);
        clickGuiModules.add(ECFarmer.INSTANCE);
        clickGuiModules.add(NoGhostBlocks.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.PortalBuild.INSTANCE);
        clickGuiModules.add(WitherBuild.INSTANCE);

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
        clickGuiModules.add(ravex.modules.exploit.Blink.INSTANCE);
        clickGuiModules.add(ravex.modules.exploit.PacketFly.INSTANCE);
        clickGuiModules.add(FakePearl.INSTANCE);
        clickGuiModules.add(ravex.modules.exploit.Phase.INSTANCE);
        clickGuiModules.add(ravex.modules.exploit.RocketExtender.INSTANCE);
        clickGuiModules.add(ravex.modules.exploit.RaytraceBypass.INSTANCE);
        clickGuiModules.add(ravex.modules.exploit.NoMineAnimation.INSTANCE);
        clickGuiModules.add(ravex.modules.exploit.PacketEat.INSTANCE);
        clickGuiModules.add(PacketPlace.INSTANCE);
        clickGuiModules.add(PingSpoof.INSTANCE);


        // ── Client ────────────────────────────────────────────────────────────────
        clickGuiModules.add(RichPresence.INSTANCE);
        clickGuiModules.add(ravex.modules.client.GuiParticles.INSTANCE);
        clickGuiModules.add(ravex.modules.client.FastLatency.INSTANCE);
        clickGuiModules.add(Fonts.INSTANCE);
        clickGuiModules.add(Notifications.INSTANCE);
        clickGuiModules.add(DesktopGui.INSTANCE);
        clickGuiModules.add(Settings.INSTANCE);
        clickGuiModules.add(Calculator.INSTANCE);
        clickGuiModules.add(ravex.modules.misc.Commands.INSTANCE);

        // ── HUD modules ──────────────────────────────────────────────────────────
        hudModules.add(new HudModule("Watermark", 10, 10, 80, 14) {
            {
                addParameter(new ravex.parameter.ColorParameter("Color", 0xFF1E88E5));
                addParameter(new ravex.parameter.BooleanParameter("Shadow", true));
            }
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, float partialTicks) {
                if (!Hud.INSTANCE.getEnabled()) return;
                int ac = getParamColor("Color");
                boolean shadow = getParamBool("Shadow");
                int bx = getX(), by = getY();
                String text = "RaveX v" + ravex.RaveX.version + " NextGen";
                int tw = ravex.utility.render.HudRenderer.textWidth(text);
                int pw = tw + 12;
                int ph = 14;
                ravex.utility.render.HudRenderer.drawPanel(graphics, bx, by, pw, ph, ac);
                ravex.utility.render.HudRenderer.drawText(graphics, text, bx + 6, by + 3, ac, shadow);
            }

            private int getParamColor(String name) {
                for (var p : getParameters()) {
                    if (p.getName().equals(name) && p instanceof ravex.parameter.ColorParameter cp) return cp.getValue();
                }
                return ravex.gui.clickgui.ColorUtility.getActiveColor();
            }

            private boolean getParamBool(String name) {
                for (var p : getParameters()) {
                    if (p.getName().equals(name) && p instanceof ravex.parameter.BooleanParameter bp) return bp.getValue();
                }
                return true;
            }
        });

        hudModules.add(new HudModule("ActiveModules", 10, 40, 90, 100) {
            {
                addParameter(new ravex.parameter.BooleanParameter("Shadow", true));
                addParameter(new ravex.parameter.ModeParameter("Highlight", "Active Color",
                    java.util.List.of("Active Color", "Rainbow", "Gradient")));
            }
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, float partialTicks) {
                if (!Hud.INSTANCE.getEnabled()) return;
                boolean shadow = getParamBool("Shadow");
                String highlightMode = getParamMode("Highlight");
                java.util.List<String> names = new java.util.ArrayList<>();
                for (Module m : clickGuiModules) {
                    if (m.getEnabled()) names.add(m.getName());
                }
                if (names.isEmpty()) return;
                int bx = getX(), by = getY();
                int lh = 10;
                int pw = 10;
                for (String n : names) {
                    int nw = ravex.utility.render.HudRenderer.textWidth(n) + 2;
                    if (nw > pw) pw = nw;
                }
                int cy = by;
                int idx = 0;
                int activeColor = ravex.gui.clickgui.ColorUtility.getActiveColor();
                for (String n : names) {
                    int color;
                    switch (highlightMode) {
                        case "Rainbow":
                            color = ravex.gui.clickgui.ColorUtility.getRainbowColor(idx, 4000);
                            break;
                        case "Gradient":
                            color = ravex.gui.clickgui.ColorUtility.getColor(idx).getRGB();
                            break;
                        default: // "Active Color"
                            // Dim alternating modules for readability
                            color = (idx % 2 == 0) ? activeColor
                                : ravex.gui.clickgui.ColorUtility.darker(activeColor, 0.6f);
                            break;
                    }
                    ravex.utility.render.HudRenderer.drawText(graphics, n, bx, cy, color, shadow);
                    cy += lh;
                    idx++;
                }
                setWidth(pw);
                setHeight(names.size() * lh);
            }

            private boolean getParamBool(String name) {
                for (var p : getParameters()) {
                    if (p.getName().equals(name) && p instanceof ravex.parameter.BooleanParameter bp) return bp.getValue();
                }
                return true;
            }

            private String getParamMode(String name) {
                for (var p : getParameters()) {
                    if (p.getName().equals(name) && p instanceof ravex.parameter.ModeParameter mp) return mp.getValue();
                }
                return "Active Color";
            }
        });

        hudModules.add(new HudModule("Coords", 10, 200, 140, 14) {
            {
                addParameter(new ravex.parameter.BooleanParameter("Shadow", true));
                addParameter(new ravex.parameter.BooleanParameter("ColoredLabels", true));
            }
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, float partialTicks) {
                if (!Hud.INSTANCE.getEnabled()) return;
                var player = net.minecraft.client.Minecraft.getInstance().player;
                if (player == null) return;
                int ac = ravex.gui.clickgui.ColorUtility.getActiveColor();
                boolean shadow = getParamBool("Shadow");
                boolean colored = getParamBool("ColoredLabels");
                int bx = getX(), by = getY();
                String xStr = String.format("%.1f", player.getX());
                String yStr = String.format("%.1f", player.getY());
                String zStr = String.format("%.1f", player.getZ());
                String full = (colored ? "X " : "") + xStr + (colored ? " Y " : " / ") + yStr + (colored ? " Z " : " / ") + zStr;
                int pw = ravex.utility.render.HudRenderer.textWidth(full) + 10;
                ravex.utility.render.HudRenderer.drawPanel(graphics, bx, by, pw, 14, ac);
                int cx = bx + 5;
                if (colored) {
                    ravex.utility.render.HudRenderer.drawText(graphics, "X ", cx, by + 3, 0xFFFF4455, shadow);
                    cx += ravex.utility.render.HudRenderer.textWidth("X ");
                    ravex.utility.render.HudRenderer.drawText(graphics, xStr, cx, by + 3, 0xFFD0D0E0, shadow);
                    cx += ravex.utility.render.HudRenderer.textWidth(xStr);
                    ravex.utility.render.HudRenderer.drawText(graphics, " Y ", cx, by + 3, 0xFF44FF88, shadow);
                    cx += ravex.utility.render.HudRenderer.textWidth(" Y ");
                    ravex.utility.render.HudRenderer.drawText(graphics, yStr, cx, by + 3, 0xFFD0D0E0, shadow);
                    cx += ravex.utility.render.HudRenderer.textWidth(yStr);
                    ravex.utility.render.HudRenderer.drawText(graphics, " Z ", cx, by + 3, 0xFF44AAFF, shadow);
                    cx += ravex.utility.render.HudRenderer.textWidth(" Z ");
                    ravex.utility.render.HudRenderer.drawText(graphics, zStr, cx, by + 3, 0xFFD0D0E0, shadow);
                } else {
                    ravex.utility.render.HudRenderer.drawText(graphics, full, cx, by + 3, 0xFFD0D0E0, shadow);
                }
            }

            private boolean getParamBool(String name) {
                for (var p : getParameters()) {
                    if (p.getName().equals(name) && p instanceof ravex.parameter.BooleanParameter bp) return bp.getValue();
                }
                return true;
            }
        });

        hudModules.add(ravex.modules.hud.NowPlayingHud.INSTANCE);
        hudModules.add(ravex.modules.hud.ChatHud.INSTANCE);
        hudModules.add(ravex.modules.hud.TpsHud.INSTANCE);
        hudModules.add(ravex.modules.hud.CooldownsHud.INSTANCE);
        hudModules.add(ravex.modules.hud.InvPreviewHud.INSTANCE);
        hudModules.add(ravex.modules.hud.IndicatorsHud.INSTANCE);
        hudModules.add(new HudModule("Fps", 10, 220, 60, 14) {
            {
                addParameter(new ravex.parameter.ColorParameter("HighColor", 0xFF44FF88));
                addParameter(new ravex.parameter.ColorParameter("MidColor", 0xFFFFCC33));
                addParameter(new ravex.parameter.ColorParameter("LowColor", 0xFFFF4455));
                addParameter(new ravex.parameter.BooleanParameter("Shadow", true));
            }
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, float partialTicks) {
                if (!Hud.INSTANCE.getEnabled()) return;
                int fps = net.minecraft.client.Minecraft.getInstance().getFps();
                int ac = ravex.gui.clickgui.ColorUtility.getActiveColor();
                boolean shadow = getParamBool("Shadow");
                int bx = getX(), by = getY();
                int fpsColor;
                if (fps >= 60) fpsColor = getParamColor("HighColor");
                else if (fps >= 30) fpsColor = getParamColor("MidColor");
                else fpsColor = getParamColor("LowColor");
                String text = fps + " FPS";
                int pw = ravex.utility.render.HudRenderer.textWidth(text) + 10;
                ravex.utility.render.HudRenderer.drawPanel(graphics, bx, by, pw, 14, ac);
                int ix = bx + 5;
                ravex.utility.render.HudRenderer.drawText(graphics, String.valueOf(fps), ix, by + 3, fpsColor, shadow);
                ix += ravex.utility.render.HudRenderer.textWidth(String.valueOf(fps));
                ravex.utility.render.HudRenderer.drawText(graphics, " FPS", ix, by + 3, 0xFF8080A0, false);
            }

            private int getParamColor(String name) {
                for (var p : getParameters()) {
                    if (p.getName().equals(name) && p instanceof ravex.parameter.ColorParameter cp) return cp.getValue();
                }
                return ravex.gui.clickgui.ColorUtility.getActiveColor();
            }

            private boolean getParamBool(String name) {
                for (var p : getParameters()) {
                    if (p.getName().equals(name) && p instanceof ravex.parameter.BooleanParameter bp) return bp.getValue();
                }
                return true;
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
