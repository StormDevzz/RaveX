package ravex.modules.misc;
import ravex.event.client.SoundEvent;
import ravex.event.combat.AttackEvent;
import ravex.event.EventBusHolder;
import ravex.event.player.DeathEvent;
import ravex.event.Subscribe;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.parameter.StringParameter;
import ravex.utility.misc.EntityUtility;
import ravex.utility.network.NetworkUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;






@Module(name = "ChatHelper", category = "Misc")
public class ChatHelper {
    @Parameter(name = "Mode", modes = {"Announcer", "Welcomer", "AutoEZ", "ZoV", "Spammer", "CoordLogger", "DurabAlert", "ChatFilter"})
    public String mode = "Announcer";
    @Parameter(name = "Announcer", visible = "mode=Announcer")
    public boolean announcerEnabled = false;
    @Parameter(name = "Welcomer", visible = "mode=Welcomer")
    public boolean welcomerEnabled = false;
    @Parameter(name = "AutoEZ", visible = "mode=AutoEZ")
    public boolean autoEZEnabled = false;
    @Parameter(name = "Spammer", visible = "mode=Spammer")
    public boolean spammerEnabled = false;
    @Parameter(name = "CoordLogger", visible = "mode=CoordLogger")
    public boolean coordLoggerEnabled = false;
    @Parameter(name = "DurabAlert", visible = "mode=DurabAlert")
    public boolean durabAlertEnabled = false;
    @Parameter(name = "Walk", visible = "mode=Announcer")
    public boolean announceWalk = true;
    @Parameter(name = "Eat", visible = "mode=Announcer")
    public boolean announceEat = true;
    @Parameter(name = "Hit", visible = "mode=Announcer")
    public boolean announceHit = true;
    @Parameter(name = "AnnounceMode", modes = {"Periodic", "Milestone"}, visible = "mode=Announcer")
    public String announceMode = "Periodic";
    @Parameter(name = "Interval", min = 10, max = 300, step = 10, visible = "mode=Announcer")
    public double interval = 10;
    @Parameter(name = "FirstJoinOnly", visible = "mode=Welcomer")
    public boolean onlyFirstJoin = true;
    @Parameter(name = "EZOnlyPlayers", visible = "mode=AutoEZ")
    public boolean ezOnlyPlayers = true;
    @Parameter(name = "EZDelay", min = 0.0, max = 3000.0, step = 100.0, visible = "mode=AutoEZ")
    public double ezDelay = 500.0;
    @Parameter(name = "ZoV", visible = "mode=ZoV")
    public boolean zov = false;
    @Parameter(name = "ZoVStyle", modes = {"Simple", "Extended"}, visible = "mode=ZoV")
    public String zovStyle = "Extended";
    @Parameter(name = "SpamMode", modes = {"Text", "File"}, visible = "mode=Spammer")
    public String spamMode = "Text";
    @Parameter(name = "SpamText", visible = "mode=Spammer")
    public String spamText = "RaveX on top!";
    @Parameter(name = "SpamFile", visible = "mode=Spammer")
    public String spamFile = "spam.txt";
    @Parameter(name = "SpamDelay", min = 100.0, max = 10000.0, step = 100.0, visible = "mode=Spammer")
    public double spamDelay = 1000.0;
    @Parameter(name = "LogDeath", visible = "mode=CoordLogger")
    public boolean logDeath = true;
    @Parameter(name = "LogJoin", visible = "mode=CoordLogger")
    public boolean logJoin = false;
    @Parameter(name = "ChatNotify", visible = "mode=CoordLogger")
    public boolean chatNotify = true;
    @Parameter(name = "AlertMode", modes = {"Own", "Enemy", "Both"}, visible = "mode=DurabAlert")
    public String dAlertMode = "Own";
    @Parameter(name = "Threshold", min = 1.0, max = 100.0, step = 1.0, visible = "mode=DurabAlert")
    public double threshold = 10.0;
    @Parameter(name = "Sound", visible = "mode=DurabAlert")
    public boolean sound = true;
    @Parameter(name = "ChatFilter", visible = "mode=ChatFilter")
    public boolean chatFilter = false;
    @Parameter(name = "FilterDuplicate", visible = "mode=ChatFilter")
    public boolean filterDuplicate = false;
    @Parameter(name = "OnlyName", visible = "mode=ChatFilter")
    public boolean onlyName = true;
    @Parameter(name = "Timestamp")
    public boolean timestamp = false;
    @Parameter(name = "TSFormat", modes = {"HH:mm", "HH:mm:ss", "[HH:mm]", "[HH:mm:ss]"})
    public String timestampFormat = "HH:mm";
    @Parameter(name = "ChatHistory", min = 100.0, max = 10000.0, step = 100.0)
    public double chatHistorySize = 1000.0;
    @Parameter(name = "CopyOnClick")
    public boolean copyOnClick = false;
    private static final String LOG_DIR = "RaveX/coordlogs";
    private String currentFile = null;
    private static final long ALERT_COOLDOWN_MS = 30000;
    private final Map<String, Long> alertCooldowns = new HashMap<>();
    private double lastX, lastZ;
    private double blocksWalked;
    private int foodEaten;
    private int hitsDealt;
    private int tickCounter;
    private int lastFoodLevel;
    private long lastSpamTime = 0;
    private final Set<UUID> knownPlayers = new HashSet<>();
    private final String[] welcomeMessages = {
        "Sup %s", "Yo %s!", "What's good %s?", "Ayy %s is in the house!",
        "Bruh %s just joined", "Holy shit %s is here", "Damn %s finally joined",
        "%s what's poppin'?", "Yo yo yo %s!", "Ey yo %s!", "Shit %s pulled up",
        "%s just rolled in", "%s is finally here, bout damn time",
        "What's up %s my G", "Oh damn %s joined", "%s in the building",
        "%s just spawned", "Yo %s what it do", "%s has entered the server",
        "Ayyy %s", "%s just logged in, everyone hide!", "Oh fuck %s is here",
        "%s my guy what's up", "Yooo %s!", "%s joined, party time!",
        "%s just appeared outta nowhere", "Here comes %s",
        "%s is back at it again", "Damnnn %s in the chat", "Ayo %s!",
        "What's cracking %s", "%s just joined the squad", "Yo check it, %s is here"
    };
    private static final List<String> EZ_PHRASES = List.of(
        "ez %s bruh", "get rekt %s", "sit down %s", "L %s",
        "cope harder %s", "absolute bot %s", "fucking noob %s",
        "go back to bed %s", "LMAO %s", "ur trash %s",
        "stay mad %s", "rekt %s ez", "you're actually dogshit %s",
        "free kill %s", "lol get owned %s", "%s just got clapped",
        "think before you speak %s", "uninstall %s",
        "breathe through your nose %s", "actual bot behavior %s",
        "%s more like L%s", "kys... kidding, just get better %s",
        "dogwater %s", "negative KD %s", "%s died for what",
        "clear skill issue %s", "gapped %s", "botted %s",
        "outplayed %s", "%s u good?", "motherless behavior %s",
        "touch grass %s", "quit the game %s"
    );
    private final Random random = new Random();
    private long lastKillTime = 0;
    private String lastMessage = "";
    private int duplicateCount = 0;

    public boolean shouldFilterMessage(String msg) {
        if (!Modules.enabled(ChatHelper.class)) return false;
        if ("ChatFilter".equals(mode)) {
            if (onlyName) {
                var player = MinecraftWrapper.getWrapper().getPlayer();
                if (player != null) {
                    String playerName = player.getGameProfile().name().toLowerCase();
                    if (!msg.toLowerCase().contains(playerName)) return true;
                }
            }
            return ravex.utility.network.NetworkUtility.isAdMessage(msg);
        }
        if (!chatFilter) return false;
        if (filterDuplicate) {
            if (msg.equals(lastMessage)) {
                duplicateCount++;
                return duplicateCount > 2;
            } else {
                lastMessage = msg;
                duplicateCount = 0;
            }
        }
        return ravex.utility.network.NetworkUtility.isAdMessage(msg);
    }

    public String applyTimestamp(String message) {
        if (!Modules.enabled(ChatHelper.class) || !timestamp) return message;
        var now = java.time.LocalTime.now();
        String fmt = timestampFormat;
        String ts = switch (fmt) {
            case "HH:mm" -> String.format("%02d:%02d", now.getHour(), now.getMinute());
            case "HH:mm:ss" -> String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond());
            case "[HH:mm]" -> String.format("[%02d:%02d]", now.getHour(), now.getMinute());
            case "[HH:mm:ss]" -> String.format("[%02d:%02d:%02d]", now.getHour(), now.getMinute(), now.getSecond());
            default -> "";
        };
        return ts + " " + message;
    }

    public String applyZov(String message) {
        if (!Modules.enabled(ChatHelper.class) || !zov) return message;
        boolean extended = "Extended".equals(zovStyle);
        String r = message
            .replace('з', 'Z').replace('З', 'Z')
            .replace('в', 'V').replace('В', 'V');
        if (extended) {
            r = r
                .replace('а', 'a').replace('А', 'A')
                .replace('е', 'e').replace('Е', 'E')
                .replace('о', 'o').replace('О', 'O')
                .replace('р', 'p').replace('Р', 'P')
                .replace('с', 'c').replace('С', 'C')
                .replace('х', 'x').replace('Х', 'X')
                .replace('у', 'y').replace('У', 'Y')
                .replace('к', 'k').replace('К', 'K')
                .replace('м', 'm').replace('М', 'M');
        }
        return r;
    }

    public void onDeath(double x, double y, double z, String dimension) {
        if (!Modules.enabled(ChatHelper.class) || !coordLoggerEnabled) return;
        if (!logDeath) return;
        new File(LOG_DIR).mkdirs();
        if (currentFile == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            currentFile = LOG_DIR + "/session_" + sdf.format(new Date()) + ".log";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(new Date());
        String line = String.format("[%s] DEATH | X: %.1f Y: %.1f Z: %.1f | Dim: %s\n",
            timestamp, x, y, z, dimension);
        try (FileWriter fw = new FileWriter(currentFile, true)) {
            fw.write(line);
        } catch (Exception ignored) {}
        if (chatNotify) {
            var mc = MinecraftWrapper.getWrapper();
            if (mc.getPlayer() != null) {
                mc.getPlayer().displayClientMessage(
                    Component.literal("§7[§cCoordLogger§7] §fDEATH at X=" +
                        String.format("%.1f", x) + " Y=" + String.format("%.1f", y) +
                        " Z=" + String.format("%.1f", z)),
                    false
                );
            }
        }
    }
    public void onEnable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        if (announcerEnabled) {
            lastX = mc.getPlayer().getX();
            lastZ = mc.getPlayer().getZ();
            lastFoodLevel = mc.getPlayer().getFoodData().getFoodLevel();
            blocksWalked = 0; foodEaten = 0; hitsDealt = 0; tickCounter = 0;
        }
        if (welcomerEnabled) {
            knownPlayers.clear();
            if (mc.getLevel() != null) {
                for (net.minecraft.world.entity.player.Player p : mc.getLevel().players()) {
                    knownPlayers.add(p.getUUID());
                }
            }
        }
        if (coordLoggerEnabled) {
            new File(LOG_DIR).mkdirs();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            currentFile = LOG_DIR + "/session_" + sdf.format(new Date()) + ".log";
            try (FileWriter fw = new FileWriter(currentFile, true)) {
                fw.write("=== CoordLogger Session Started ===\n");
            } catch (Exception ignored) {}
            if (logJoin) {
                double x = mc.getPlayer().getX(), y = mc.getPlayer().getY(), z = mc.getPlayer().getZ();
                String dim = mc.getPlayer().level().dimension().identifier().toString();
                SimpleDateFormat tsdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String ts = tsdf.format(new Date());
                String line = String.format("[%s] JOIN | X: %.1f Y: %.1f Z: %.1f | Dim: %s\n",
                    ts, x, y, z, dim);
                try (FileWriter fw = new FileWriter(currentFile, true)) {
                    fw.write(line);
                } catch (Exception ignored) {}
                if (chatNotify) {
                    mc.getPlayer().displayClientMessage(
                        Component.literal("§7[§cCoordLogger§7] §fJOIN at X=" +
                            String.format("%.1f", x) + " Y=" + String.format("%.1f", y) +
                            " Z=" + String.format("%.1f", z)),
                        false
                    );
                }
            }
        }
    }

    @Subscribe
    public void onAttack(AttackEvent event) {
        onHit();
    }

    @Subscribe
    public void onDeath(DeathEvent event) {
        if (!Modules.enabled(ChatHelper.class) || !autoEZEnabled) return;
        if (event.isSelf()) return;
        net.minecraft.world.entity.player.Player victim = event.getPlayer();
        if (ezOnlyPlayers && !(victim instanceof net.minecraft.world.entity.player.Player)) return;
        net.minecraft.world.entity.Entity killer = event.getSource().getEntity();
        if (killer != MinecraftWrapper.getWrapper().getPlayer()) return;
        long now = System.currentTimeMillis();
        if (now - lastKillTime < (long) ezDelay) return;
        lastKillTime = now;
        String name = victim.getName().getString();
        String phrase = String.format(EZ_PHRASES.get(random.nextInt(EZ_PHRASES.size())), name);
        if (MinecraftWrapper.getWrapper().getPlayer() != null) {
            NetworkUtility.sendChat(phrase);
        }
    }

    public void onHit() {
        if (!Modules.enabled(ChatHelper.class) || !announcerEnabled) return;
        if (announceHit) hitsDealt++;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        net.minecraft.client.player.LocalPlayer p = mc.getPlayer();
        if (p == null || mc.getLevel() == null || p.connection == null) return;
        if (announcerEnabled) tickAnnouncer(mc, p);
        if (welcomerEnabled) tickWelcomer(mc, p);
        if (spammerEnabled) tickSpammer();
        if (durabAlertEnabled) tickDurabAlert(mc);
    }

    private void tickAnnouncer(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer p) {
        tickCounter++;
        if (announceWalk) {
            double dx = p.getX() - lastX;
            double dz = p.getZ() - lastZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.1) blocksWalked += dist;
            lastX = p.getX(); lastZ = p.getZ();
        }
        if (announceEat) {
            int cur = p.getFoodData().getFoodLevel();
            if (cur > lastFoodLevel) foodEaten++;
            lastFoodLevel = cur;
        }
        String modeStr = announceMode;
        if (modeStr.equals("Periodic")) {
            int intervalTicks = (int) interval * 20;
            if (tickCounter >= intervalTicks) { doAnnounce(p); tickCounter = 0; }
        } else {
            checkMilestone(100, p); checkMilestone(500, p);
            checkMilestone(1000, p); checkMilestone(2500, p);
            checkMilestone(5000, p); checkMilestone(10000, p);
            if (foodEaten == 10 || foodEaten == 25 || foodEaten == 50 || foodEaten == 100) {
                NetworkUtility.sendChat("I just ate " + foodEaten + " times, damn I'm hungry af");
                foodEaten = 0;
            }
            if (hitsDealt == 100 || hitsDealt == 500 || hitsDealt == 1000 || hitsDealt == 5000) {
                NetworkUtility.sendChat("I dealt " + hitsDealt + " hits, stop moving!");
                hitsDealt = 0;
            }
        }
    }

    private void checkMilestone(int target, net.minecraft.client.player.LocalPlayer p) {
        if (blocksWalked >= target && blocksWalked - 50 < target) {
            NetworkUtility.sendChat("I walked " + target + " blocks already");
            blocksWalked = 0;
        }
    }

    private void doAnnounce(net.minecraft.client.player.LocalPlayer p) {
        if (blocksWalked < 0.5 && foodEaten == 0 && hitsDealt == 0) return;
        StringBuilder sb = new StringBuilder("[Announcer] ");
        boolean added = false;
        if (announceWalk && blocksWalked >= 1.0) { sb.append("Walked ").append(String.format("%.0f", blocksWalked)).append("b. "); added = true; }
        if (announceEat && foodEaten > 0) { sb.append("Ate ").append(foodEaten).append("x. "); added = true; }
        if (announceHit && hitsDealt > 0) { sb.append("Hit ").append(hitsDealt).append("x. "); added = true; }
        if (added) NetworkUtility.sendChat(sb.toString());
        blocksWalked = 0; foodEaten = 0; hitsDealt = 0;
    }

    private void tickWelcomer(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer me) {
        for (net.minecraft.world.entity.player.Player player : mc.getLevel().players()) {
            if (player == me) continue;
            if (knownPlayers.contains(player.getUUID())) continue;
            knownPlayers.add(player.getUUID());
            String name = player.getGameProfile().name();
            int idx = me.getRandom().nextInt(welcomeMessages.length);
            String msg = String.format(welcomeMessages[idx], name);
            NetworkUtility.sendCommand(msg);
        }
    }

    private void tickSpammer() {
        long now = System.currentTimeMillis();
        if (now - lastSpamTime < (long) spamDelay) return;
        lastSpamTime = now;
        net.minecraft.client.player.LocalPlayer p = MinecraftWrapper.getWrapper().getPlayer();
        if (p == null || p.connection == null) return;
        String msg;
        if ("File".equals(spamMode)) {
            List<String> lines = null;
            try { lines = Files.readAllLines(Path.of(spamFile)); } catch (IOException ignored) {}
            if (lines == null || lines.isEmpty()) return;
            msg = lines.get(random.nextInt(lines.size()));
        } else {
            msg = spamText;
        }
        NetworkUtility.sendChat(msg);
    }

    private void tickDurabAlert(MinecraftWrapper mc) {
        double thresh = threshold;
        String am = dAlertMode;
        if (am.equals("Own") || am.equals("Both")) {
            EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
            String[] names = {"Boots", "Leggings", "Chestplate", "Helmet"};
            for (int i = 0; i < slots.length; i++) {
                var stack = mc.getPlayer().getItemBySlot(slots[i]);
                if (stack.isEmpty() || !stack.isDamageableItem()) continue;
                int maxDmg = stack.getMaxDamage();
                int curDmg = stack.getDamageValue();
                double pct = (double) (maxDmg - curDmg) / maxDmg * 100.0;
                if (pct < thresh) {
                    doAlert("Own " + names[i], "Your " + names[i] + " is at " + String.format("%.0f", pct) + "% durability!");
                }
            }
        }
        if (am.equals("Enemy") || am.equals("Both")) {
            net.minecraft.world.entity.LivingEntity living = EntityUtility.asLivingEntity(mc.getCrosshairPickEntity());
            if (living != null && !living.equals(mc.getPlayer())) {
                EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
                String[] names = {"Boots", "Leggings", "Chestplate", "Helmet"};
                for (int i = 0; i < slots.length; i++) {
                    var stack = living.getItemBySlot(slots[i]);
                    if (stack.isEmpty() || !stack.isDamageableItem()) continue;
                    int maxDmg = stack.getMaxDamage();
                    int curDmg = stack.getDamageValue();
                    double pct = (double) (maxDmg - curDmg) / maxDmg * 100.0;
                    if (pct < thresh) {
                        String entityName = living.getName().getString();
                        doAlert("Enemy " + names[i] + "@" + entityName,
                                entityName + "'s " + names[i] + " is at " + String.format("%.0f", pct) + "% durability!");
                    }
                }
            }
        }
    }

    private void doAlert(String cooldownKey, String message) {
        long now = System.currentTimeMillis();
        Long lastAlert = alertCooldowns.get(cooldownKey);
        if (lastAlert != null && (now - lastAlert) < ALERT_COOLDOWN_MS) return;
        alertCooldowns.put(cooldownKey, now);
        ravex.manager.NotificationManager.add("§e" + message, 0xFFFFCC33, 3000);
        if (sound) {
            EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.FAILURE));
        }
    }






}