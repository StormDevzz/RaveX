package ravex.modules.hud;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.scores.PlayerTeam;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Module;
import ravex.modules.Modules;
import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import ravex.modules.client.Hud;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.HudRendererUtility;

@HudModule("StaffBoardHud")
public class StaffBoardHud extends Module {

    @Parameter(name = "ShowHeads")
    public boolean showHeads = true;

    @Parameter(name = "ShowPing")
    public boolean showPing = true;

    @Parameter(name = "ShowVanished")
    public boolean showVanished = true;

    @Parameter(name = "ShowGameMode")
    public boolean showGameMode = false;

    @Parameter(name = "ShowSelf")
    public boolean showSelf = false;

    @Parameter(name = "HighlightColor", color = true)
    public int highlightColor = 0xFF38BDF8;

    private static final int ROW_H = 18;
    private static final int HEAD_SIZE = 12;

    private List<StaffInfo> staffEntries = new ArrayList<>();
    private int lastCount = 0;
    private boolean flash = false;

    private StaffBoardHud() {
        super("StaffBoardHud", 10, 120, 185, 36);
        setX(10);
        setY(120);
        setWidth(185);
        setHeight(36);
    }

    private static class StaffInfo {
        final String name;
        final String displayName;
        final PlayerInfo info;
        final boolean vanished;

        StaffInfo(String name, String displayName, PlayerInfo info, boolean vanished) {
            this.name = name;
            this.displayName = displayName;
            this.info = info;
            this.vanished = vanished;
        }
    }

    @Subscribe
    public void onClientTick(TickEvent.Client event) {
        if (!getEnabled()) return;
        MinecraftWrapper mc = MinecraftWrapper.getWrapper();
        var connection = mc.getConnection();
        if (connection == null || mc.getPlayer() == null) {
            staffEntries = new ArrayList<>();
            return;
        }

        Collection<PlayerInfo> listed = connection.getListedOnlinePlayers();
        Collection<PlayerInfo> online = connection.getOnlinePlayers();
        Set<UUID> listedIds = new HashSet<>();
        if (listed != null) {
            for (PlayerInfo info : listed) {
                if (info.getProfile() != null) listedIds.add(info.getProfile().id());
            }
        }

        List<StaffInfo> newList = new ArrayList<>();
        String selfName = mc.getPlayer().getName().getString();

        if (listed != null) {
            for (PlayerInfo info : listed) {
                String rawName = info.getProfile() != null ? info.getProfile().name() : "";
                if (rawName == null) rawName = "";
                Component displayComp = info.getTabListDisplayName();
                String displayName = displayComp != null ? displayComp.getString() : rawName;
                if (!showSelf && rawName.equalsIgnoreCase(selfName)) continue;
                if (isStaff(info, rawName, displayName)) {
                    newList.add(new StaffInfo(rawName, displayName, info, false));
                }
            }
        }

        if (online != null && showVanished) {
            for (PlayerInfo info : online) {
                UUID id = info.getProfile() != null ? info.getProfile().id() : new UUID(0L, 0L);
                if (listedIds.contains(id)) continue;
                String rawName = info.getProfile() != null ? info.getProfile().name() : "";
                if (rawName == null) rawName = "";
                Component displayComp = info.getTabListDisplayName();
                String displayName = displayComp != null ? displayComp.getString() : rawName;
                if (!showSelf && rawName.equalsIgnoreCase(selfName)) continue;
                if (isStaff(info, rawName, displayName)) {
                    newList.add(new StaffInfo(rawName, displayName, info, true));
                }
            }
        }

        newList.sort(Comparator.comparing((StaffInfo s) -> s.name.toLowerCase()));
        staffEntries = newList;
        if (newList.size() > lastCount) {
            flash = true;
        }
        lastCount = newList.size();
    }

    private boolean isStaff(PlayerInfo info, String rawName, String displayName) {
        String lowerName = (rawName != null ? rawName : "").toLowerCase();
        String lowerDisplay = (displayName != null ? displayName : "").toLowerCase();
        String combined = (lowerName + " " + lowerDisplay).toLowerCase();

        String[] keywords = {
            "admin", "moderator", "moder", "helper", "staff", "support",
            "developer", "dev", "builder", "media", "curator", "owner", "operator", "op"
        };
        for (String kw : keywords) {
            if (combined.contains(kw)) return true;
        }

        String[] rwPrefixes = {
            "helper", "magister", "imperator", "dragon", "god", "d.helper",
            "overlord", "titan", "avenger", "hero", "cobra", "d.admin", "d.moder"
        };
        for (String p : rwPrefixes) {
            if (lowerDisplay.contains("[" + p + "]") || lowerDisplay.contains(p)) return true;
            if (lowerName.contains(p)) return true;
        }

        String[] extraPrefixes = {
            "champion", "sun", "jelly", "pegas", "vampire", "hydra", "bunny", "tiger", "opka"
        };
        for (String p : extraPrefixes) {
            if (lowerDisplay.contains("[" + p + "]") || lowerDisplay.contains(p)) return true;
            if (lowerName.contains(p)) return true;
        }

        String[] staffNames = {
            "qbasty", "xymb", "pistonmaster", "previouslysmart", "ashiqtasdid"
        };
        for (String n : staffNames) {
            if (lowerName.equals(n)) return true;
        }

        PlayerTeam team = info.getTeam();
        if (team != null) {
            Component prefixComp = team.getPlayerPrefix();
            Component suffixComp = team.getPlayerSuffix();
            String teamStr = ((prefixComp != null ? prefixComp.getString() : "")
                    + " " + (suffixComp != null ? suffixComp.getString() : "")).toLowerCase();
            for (String kw : keywords) {
                if (teamStr.contains(kw)) return true;
            }
            for (String p : rwPrefixes) {
                if (teamStr.contains("[" + p + "]") || teamStr.contains(p)) return true;
            }
            for (String p : extraPrefixes) {
                if (teamStr.contains("[" + p + "]") || teamStr.contains(p)) return true;
            }
        }
        return false;
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        int bx = getX();
        int by = getY();
        int pw = 185;
        int rowCount = staffEntries.size();
        int headerH = 20;
        int ph = rowCount == 0 ? 36 : (headerH + 4 + rowCount * ROW_H + 4);
        setWidth(pw);
        setHeight(ph);

        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);
        if (flash) {
            Render2DUtility.drawRoundBorder(graphics, bx, by, pw, ph, 6, 1, 0x80FFFFFF);
            flash = false;
        }

        Render2DUtility.drawRound(graphics, bx + 8, by + 7, 6, 6, 3, highlightColor);
        FontRenderUtility.drawString(graphics, "Staff", bx + 18, by + 6, 0xFFFFFFFF, true);

        String countStr = String.valueOf(rowCount);
        int countW = FontRenderUtility.getStringWidth(countStr);
        int pillW = countW + 8;
        int pillX = bx + pw - 8 - pillW;
        Render2DUtility.drawRound(graphics, pillX, by + 4, pillW, 12, 3, 0x40161C28);
        Render2DUtility.drawRoundBorder(graphics, pillX, by + 4, pillW, 12, 3, 1, 0x18FFFFFF);
        FontRenderUtility.drawString(graphics, countStr, pillX + 4, by + 6, highlightColor, false);

        graphics.fill(bx + 6, by + headerH, bx + pw - 6, by + headerH + 1, 0x18FFFFFF);

        if (rowCount == 0) {
            FontRenderUtility.drawString(graphics, "No staff online", bx + 8, by + 23, 0xFF8E99A8, false);
            return;
        }

        int fontH = FontRenderUtility.getFontHeight();
        int startY = by + headerH + 4;

        for (int i = 0; i < staffEntries.size(); i++) {
            StaffInfo staff = staffEntries.get(i);
            int rowY = startY + i * ROW_H;

            if (i % 2 == 1) {
                Render2DUtility.drawRound(graphics, bx + 4, rowY, pw - 8, ROW_H - 1, 2, 0x14FFFFFF);
            }

            int curLeftX = bx + 7;
            if (showHeads) {
                int headY = rowY + (ROW_H - 1 - HEAD_SIZE) / 2;
                boolean headDrawn = false;
                if (staff.info != null) {
                    try {
                        PlayerSkin skin = staff.info.getSkin();
                        if (skin != null && skin.body() != null && skin.body().texturePath() != null) {
                            Identifier tex = skin.body().texturePath();
                            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, curLeftX, headY,
                                    8.0f, 8.0f, HEAD_SIZE, HEAD_SIZE, 8, 8, 64, 64, 0xFFFFFFFF);
                            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, curLeftX, headY,
                                    40.0f, 8.0f, HEAD_SIZE, HEAD_SIZE, 8, 8, 64, 64, 0xFFFFFFFF);
                            headDrawn = true;
                        }
                    } catch (Exception ignored) {}
                }
                if (!headDrawn) {
                    Render2DUtility.drawRound(graphics, curLeftX, headY, HEAD_SIZE, HEAD_SIZE, 2, 0xFF2A3444);
                }
                curLeftX += HEAD_SIZE + 4;
            }

            int dotY = rowY + (ROW_H - 1 - 4) / 2;
            int dotColor = staff.vanished ? 0xFFC084FC : 0xFF4ADE80;
            Render2DUtility.drawRound(graphics, curLeftX, dotY, 4, 4, 2, dotColor);
            curLeftX += 7;

            int rightBlockW = 0;
            String pingStr = "";
            int pingColor = 0xFF4ADE80;
            int pingW = 0;
            if (showPing && staff.info != null) {
                int ping = staff.info.getLatency();
                pingStr = ping + "ms";
                pingColor = ping < 60 ? 0xFF4ADE80 : (ping < 150 ? 0xFFFBBF24 : 0xFFF87171);
                pingW = FontRenderUtility.getStringWidth(pingStr);
                rightBlockW += pingW;
            }

            String tag = extractTag(staff.displayName);
            int tagW = 0;
            if (!tag.isEmpty()) {
                tagW = FontRenderUtility.getStringWidth(tag) + 6;
                rightBlockW += tagW + (pingW > 0 ? 4 : 0);
            }

            int maxNameW = (bx + pw - 8 - rightBlockW - 4) - curLeftX;
            String nameText = staff.displayName;
            if (FontRenderUtility.getStringWidth(nameText) > maxNameW && maxNameW > 10) {
                while (!nameText.isEmpty() && FontRenderUtility.getStringWidth(nameText + "...") > maxNameW) {
                    nameText = nameText.substring(0, nameText.length() - 1);
                }
                nameText += "...";
            }

            int textY = rowY + (ROW_H - 1 - fontH) / 2 + 1;
            int nameColor = staff.vanished ? 0xFFE9D5FF : 0xFFEEEEEE;
            FontRenderUtility.drawString(graphics, nameText, curLeftX, textY, nameColor, false);

            int rightEdge = bx + pw - 8;
            if (pingW > 0) {
                int pingX = rightEdge - pingW;
                FontRenderUtility.drawString(graphics, pingStr, pingX, textY, pingColor, false);
                rightEdge -= (pingW + 4);
            }

            if (tagW > 0) {
                int tagX = rightEdge - tagW;
                int tagY = rowY + (ROW_H - 1 - 10) / 2;
                Render2DUtility.drawRound(graphics, tagX, tagY, tagW, 10, 2, 0x40161C28);
                FontRenderUtility.drawString(graphics, tag, tagX + 3, tagY + 1, highlightColor, false);
            }
        }
    }

    private String extractTag(String display) {
        if (display == null) return "";
        String d = display.toLowerCase();
        if (d.contains("admin") || d.contains("op") || d.contains("owner")) return "ADMIN";
        if (d.contains("moder") || d.contains("mod")) return "MOD";
        if (d.contains("helper") || d.contains("d.helper")) return "HELPER";
        if (d.contains("media") || d.contains("youtube") || d.contains("yt")) return "MEDIA";
        return "";
    }
}