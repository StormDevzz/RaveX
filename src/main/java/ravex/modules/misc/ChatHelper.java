package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import ravex.event.Subscribe;
import ravex.event.combat.AttackEvent;
import ravex.event.player.DeathEvent;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.StringParameter;
import ravex.event.EventBusHolder;
import ravex.event.client.SoundEvent;
import ravex.manager.ModuleManager;
import ravex.utility.misc.MobUtility;
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
public class ChatHelper extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Announcer", List.of("Announcer", "Welcomer", "AutoEZ", "ZoV", "Spammer", "CoordLogger", "DurabAlert", "ChatFilter"));
    public final BooleanParameter announcerEnabled = new BooleanParameter("Announcer", false);
    public final BooleanParameter welcomerEnabled = new BooleanParameter("Welcomer", false);
    public final BooleanParameter autoEZEnabled = new BooleanParameter("AutoEZ", false);
    public final BooleanParameter spammerEnabled = new BooleanParameter("Spammer", false);
    public final BooleanParameter coordLoggerEnabled = new BooleanParameter("CoordLogger", false);
    public final BooleanParameter durabAlertEnabled = new BooleanParameter("DurabAlert", false);
    public final BooleanParameter chatFilter = new BooleanParameter("ChatFilter", false);
    public final BooleanParameter filterDuplicate = new BooleanParameter("FilterDuplicate", false);
    public final BooleanParameter onlyName = new BooleanParameter("OnlyName", true);
    public final BooleanParameter timestamp = new BooleanParameter("Timestamp", false);
    public final ModeParameter timestampFormat = new ModeParameter("TSFormat", "HH:mm", List.of("HH:mm", "HH:mm:ss", "[HH:mm]", "[HH:mm:ss]"));
    public final BooleanParameter announceWalk = new BooleanParameter("Walk", true);
    public final BooleanParameter announceEat = new BooleanParameter("Eat", true);
    public final BooleanParameter announceHit = new BooleanParameter("Hit", true);
    public final ModeParameter announceMode = new ModeParameter("AnnounceMode", "Periodic", List.of("Periodic", "Milestone"));
    public final NumberParameter interval = new NumberParameter("Interval", 10, 10, 300, 10);
    public final BooleanParameter onlyFirstJoin = new BooleanParameter("FirstJoinOnly", true);
    public final BooleanParameter ezOnlyPlayers = new BooleanParameter("EZOnlyPlayers", true);
    public final NumberParameter ezDelay = new NumberParameter("EZDelay", 500.0, 0.0, 3000.0, 100.0);
    public final ModeParameter zovStyle = new ModeParameter("ZoVStyle", "Extended", List.of("Simple", "Extended"));
    public final BooleanParameter zov = new BooleanParameter("ZoV", false);
    public final ModeParameter spamMode = new ModeParameter("SpamMode", "Text", List.of("Text", "File"));
    public final StringParameter spamText = new StringParameter("SpamText", "RaveX on top!");
    public final StringParameter spamFile = new StringParameter("SpamFile", "spam.txt");
    public final NumberParameter spamDelay = new NumberParameter("SpamDelay", 1000.0, 100.0, 10000.0, 100.0);
    public final BooleanParameter logDeath = new BooleanParameter("LogDeath", true);
    public final BooleanParameter logJoin = new BooleanParameter("LogJoin", false);
    public final BooleanParameter chatNotify = new BooleanParameter("ChatNotify", true);
    public final ModeParameter dAlertMode = new ModeParameter("AlertMode", "Own", List.of("Own", "Enemy", "Both"));
    public final NumberParameter threshold = new NumberParameter("Threshold", 10.0, 1.0, 100.0, 1.0);
    public final BooleanParameter sound = new BooleanParameter("Sound", true);
    public final NumberParameter chatHistorySize = new NumberParameter("ChatHistory", 1000.0, 100.0, 10000.0, 100.0);
    public final BooleanParameter copyOnClick = new BooleanParameter("CopyOnClick", false);
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

    private ChatHelper() {
        super("ChatHelper");
        chatFilter.setVisible(() -> "ChatFilter".equals(mode.getValue()));
        onlyName.setVisible(() -> "ChatFilter".equals(mode.getValue()));
        timestamp.setVisible(() -> false);
        timestampFormat.setVisible(() -> false);
        chatHistorySize.setVisible(() -> false);
        copyOnClick.setVisible(() -> false);
        filterDuplicate.setVisible(() -> false);
        announcerEnabled.setVisible(() -> "Announcer".equals(mode.getValue()));
        announceWalk.setVisible(() -> "Announcer".equals(mode.getValue()));
        announceEat.setVisible(() -> "Announcer".equals(mode.getValue()));
        announceHit.setVisible(() -> "Announcer".equals(mode.getValue()));
        announceMode.setVisible(() -> "Announcer".equals(mode.getValue()));
        interval.setVisible(() -> "Announcer".equals(mode.getValue()));
        welcomerEnabled.setVisible(() -> "Welcomer".equals(mode.getValue()));
        onlyFirstJoin.setVisible(() -> "Welcomer".equals(mode.getValue()));
        autoEZEnabled.setVisible(() -> "AutoEZ".equals(mode.getValue()));
        ezOnlyPlayers.setVisible(() -> "AutoEZ".equals(mode.getValue()));
        ezDelay.setVisible(() -> "AutoEZ".equals(mode.getValue()));
        zov.setVisible(() -> "ZoV".equals(mode.getValue()));
        zovStyle.setVisible(() -> "ZoV".equals(mode.getValue()));
        spammerEnabled.setVisible(() -> "Spammer".equals(mode.getValue()));
        spamMode.setVisible(() -> "Spammer".equals(mode.getValue()));
        spamText.setVisible(() -> "Spammer".equals(mode.getValue()));
        spamFile.setVisible(() -> "Spammer".equals(mode.getValue()));
        spamDelay.setVisible(() -> "Spammer".equals(mode.getValue()));
        coordLoggerEnabled.setVisible(() -> "CoordLogger".equals(mode.getValue()));
        logDeath.setVisible(() -> "CoordLogger".equals(mode.getValue()));
        logJoin.setVisible(() -> "CoordLogger".equals(mode.getValue()));
        chatNotify.setVisible(() -> "CoordLogger".equals(mode.getValue()));
        durabAlertEnabled.setVisible(() -> "DurabAlert".equals(mode.getValue()));
        dAlertMode.setVisible(() -> "DurabAlert".equals(mode.getValue()));
        threshold.setVisible(() -> "DurabAlert".equals(mode.getValue()));
        sound.setVisible(() -> "DurabAlert".equals(mode.getValue()));
    }

    public boolean shouldFilterMessage(String msg) {
        if (!getEnabled()) return false;
        if ("ChatFilter".equals(mode.getValue())) {
            if (onlyName.getValue() && Minecraft.getInstance().player != null) {
                String playerName = Minecraft.getInstance().player.getGameProfile().name().toLowerCase();
                if (!msg.toLowerCase().contains(playerName)) return true;
            }
            return ravex.utility.network.NetworkUtility.isAdMessage(msg);
        }
        if (!chatFilter.getValue()) return false;
        if (filterDuplicate.getValue()) {
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
        if (!getEnabled() || !timestamp.getValue()) return message;
        var now = java.time.LocalTime.now();
        String fmt = timestampFormat.getValue();
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
        if (!getEnabled() || !zov.getValue()) return message;
        boolean extended = "Extended".equals(zovStyle.getValue());
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
        if (!getEnabled() || !coordLoggerEnabled.getValue()) return;
        if (!logDeath.getValue()) return;
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
        if (chatNotify.getValue()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("§7[§cCoordLogger§7] §fDEATH at X=" +
                        String.format("%.1f", x) + " Y=" + String.format("%.1f", y) +
                        " Z=" + String.format("%.1f", z)),
                    false
                );
            }
        }
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (announcerEnabled.getValue()) {
            lastX = mc.player.getX();
            lastZ = mc.player.getZ();
            lastFoodLevel = mc.player.getFoodData().getFoodLevel();
            blocksWalked = 0; foodEaten = 0; hitsDealt = 0; tickCounter = 0;
        }
        if (welcomerEnabled.getValue()) {
            knownPlayers.clear();
            if (mc.level != null) {
                for (Player p : mc.level.players()) {
                    knownPlayers.add(p.getUUID());
                }
            }
        }
        if (coordLoggerEnabled.getValue()) {
            new File(LOG_DIR).mkdirs();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            currentFile = LOG_DIR + "/session_" + sdf.format(new Date()) + ".log";
            try (FileWriter fw = new FileWriter(currentFile, true)) {
                fw.write("=== CoordLogger Session Started ===\n");
            } catch (Exception ignored) {}
            if (logJoin.getValue()) {
                double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
                String dim = mc.player.level().dimension().identifier().toString();
                SimpleDateFormat tsdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String ts = tsdf.format(new Date());
                String line = String.format("[%s] JOIN | X: %.1f Y: %.1f Z: %.1f | Dim: %s\n",
                    ts, x, y, z, dim);
                try (FileWriter fw = new FileWriter(currentFile, true)) {
                    fw.write(line);
                } catch (Exception ignored) {}
                if (chatNotify.getValue()) {
                    mc.player.displayClientMessage(
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
        if (!getEnabled() || !autoEZEnabled.getValue()) return;
        if (event.isSelf()) return;
        Player victim = event.getPlayer();
        if (ezOnlyPlayers.getValue() && !(victim instanceof Player)) return;
        Entity killer = event.getSource().getEntity();
        if (killer != Minecraft.getInstance().player) return;
        long now = System.currentTimeMillis();
        if (now - lastKillTime < ezDelay.getValue().longValue()) return;
        lastKillTime = now;
        String name = victim.getName().getString();
        String phrase = String.format(EZ_PHRASES.get(random.nextInt(EZ_PHRASES.size())), name);
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.sendChat(phrase);
        }
    }

    public void onHit() {
        if (!getEnabled() || !announcerEnabled.getValue()) return;
        if (announceHit.getValue()) hitsDealt++;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || p.connection == null) return;
        if (announcerEnabled.getValue()) tickAnnouncer(mc, p);
        if (welcomerEnabled.getValue()) tickWelcomer(mc, p);
        if (spammerEnabled.getValue()) tickSpammer();
        if (durabAlertEnabled.getValue()) tickDurabAlert(mc);
    }

    private void tickAnnouncer(Minecraft mc, LocalPlayer p) {
        tickCounter++;
        if (announceWalk.getValue()) {
            double dx = p.getX() - lastX;
            double dz = p.getZ() - lastZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.1) blocksWalked += dist;
            lastX = p.getX(); lastZ = p.getZ();
        }
        if (announceEat.getValue()) {
            int cur = p.getFoodData().getFoodLevel();
            if (cur > lastFoodLevel) foodEaten++;
            lastFoodLevel = cur;
        }
        String modeStr = announceMode.getValue();
        if (modeStr.equals("Periodic")) {
            int intervalTicks = interval.getValue().intValue() * 20;
            if (tickCounter >= intervalTicks) { doAnnounce(p); tickCounter = 0; }
        } else {
            checkMilestone(100, p); checkMilestone(500, p);
            checkMilestone(1000, p); checkMilestone(2500, p);
            checkMilestone(5000, p); checkMilestone(10000, p);
            if (foodEaten == 10 || foodEaten == 25 || foodEaten == 50 || foodEaten == 100) {
                p.connection.sendChat("I just ate " + foodEaten + " times, damn I'm hungry af");
                foodEaten = 0;
            }
            if (hitsDealt == 100 || hitsDealt == 500 || hitsDealt == 1000 || hitsDealt == 5000) {
                p.connection.sendChat("I dealt " + hitsDealt + " hits, stop moving!");
                hitsDealt = 0;
            }
        }
    }

    private void checkMilestone(int target, LocalPlayer p) {
        if (blocksWalked >= target && blocksWalked - 50 < target) {
            p.connection.sendChat("I walked " + target + " blocks already");
            blocksWalked = 0;
        }
    }

    private void doAnnounce(LocalPlayer p) {
        if (blocksWalked < 0.5 && foodEaten == 0 && hitsDealt == 0) return;
        StringBuilder sb = new StringBuilder("[Announcer] ");
        boolean added = false;
        if (announceWalk.getValue() && blocksWalked >= 1.0) { sb.append("Walked ").append(String.format("%.0f", blocksWalked)).append("b. "); added = true; }
        if (announceEat.getValue() && foodEaten > 0) { sb.append("Ate ").append(foodEaten).append("x. "); added = true; }
        if (announceHit.getValue() && hitsDealt > 0) { sb.append("Hit ").append(hitsDealt).append("x. "); added = true; }
        if (added) p.connection.sendChat(sb.toString());
        blocksWalked = 0; foodEaten = 0; hitsDealt = 0;
    }

    private void tickWelcomer(Minecraft mc, LocalPlayer me) {
        for (Player player : mc.level.players()) {
            if (player == me) continue;
            if (knownPlayers.contains(player.getUUID())) continue;
            knownPlayers.add(player.getUUID());
            String name = player.getGameProfile().name();
            int idx = me.getRandom().nextInt(welcomeMessages.length);
            String msg = String.format(welcomeMessages[idx], name);
            me.connection.sendCommand(msg);
        }
    }

    private void tickSpammer() {
        long now = System.currentTimeMillis();
        if (now - lastSpamTime < spamDelay.getValue().longValue()) return;
        lastSpamTime = now;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || p.connection == null) return;
        String msg;
        if ("File".equals(spamMode.getValue())) {
            List<String> lines = null;
            try { lines = Files.readAllLines(Path.of(spamFile.getValue())); } catch (IOException ignored) {}
            if (lines == null || lines.isEmpty()) return;
            msg = lines.get(random.nextInt(lines.size()));
        } else {
            msg = spamText.getValue();
        }
        p.connection.sendChat(msg);
    }

    private void tickDurabAlert(Minecraft mc) {
        double thresh = threshold.getValue();
        String am = dAlertMode.getValue();
        if (am.equals("Own") || am.equals("Both")) {
            EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
            String[] names = {"Boots", "Leggings", "Chestplate", "Helmet"};
            for (int i = 0; i < slots.length; i++) {
                var stack = mc.player.getItemBySlot(slots[i]);
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
            LivingEntity living = MobUtility.asLivingEntity(mc.crosshairPickEntity);
            if (living != null && !living.equals(mc.player)) {
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
        if (sound.getValue()) {
            EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.FAILURE));
        }
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(ChatHelper.class);
    }

    public static ChatHelper itz() {
        return ModuleManager.get(ChatHelper.class);
    }
}
