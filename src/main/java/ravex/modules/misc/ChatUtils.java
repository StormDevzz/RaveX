package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.ModeParameter;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
public class ChatUtils extends Module {
    public static final ChatUtils INSTANCE = new ChatUtils();
    public final BooleanParameter zov = new BooleanParameter("ZoV", false);
    public final NumberParameter chatHistorySize = new NumberParameter("ChatHistory", 1000.0, 100.0, 10000.0, 100.0);
    public final NumberParameter interval = new NumberParameter("Interval", 10, 10, 300, 10);
    public final BooleanParameter announceWalk = new BooleanParameter("Walk", true);
    public final BooleanParameter announceEat = new BooleanParameter("Eat", true);
    public final BooleanParameter announceHit = new BooleanParameter("Hit", true);
    public final ModeParameter announceMode = new ModeParameter("AnnounceMode", "Periodic", List.of("Periodic", "Milestone"));
    public final BooleanParameter onlyFirstJoin = new BooleanParameter("FirstJoinOnly", true);
    public final BooleanParameter ezOnlyPlayers = new BooleanParameter("EZOnlyPlayers", true);
    public final NumberParameter ezDelay = new NumberParameter("EZDelay(ms)", 500.0, 0.0, 3000.0, 100.0);
    public final ModeParameter mode = new ModeParameter("Mode", "All", List.of("All", "Announcer", "Welcomer", "AutoEZ", "ExtraChat"));
    private double lastX, lastZ;
    private double blocksWalked;
    private int foodEaten;
    private int hitsDealt;
    private int tickCounter;
    private int lastFoodLevel;
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
    private ChatUtils() {
        super("ChatUtils");
        zov.setVisible(() -> {
            String m = mode.getValue();
            return m.equals("All") || m.equals("ExtraChat");
        });
        chatHistorySize.setVisible(() -> {
            String m = mode.getValue();
            return m.equals("All") || m.equals("ExtraChat");
        });
        interval.setVisible(() -> {
            String m = mode.getValue();
            return m.equals("All") || m.equals("Announcer");
        });
        announceWalk.setVisible(() -> {
            String m = mode.getValue();
            return m.equals("All") || m.equals("Announcer");
        });
        announceEat.setVisible(() -> {
            String m = mode.getValue();
            return m.equals("All") || m.equals("Announcer");
        });
        announceHit.setVisible(() -> {
            String m = mode.getValue();
            return m.equals("All") || m.equals("Announcer");
        });
        announceMode.setVisible(() -> {
            String m = mode.getValue();
            return m.equals("All") || m.equals("Announcer");
        });
        onlyFirstJoin.setVisible(() -> {
            String m = mode.getValue();
            return m.equals("All") || m.equals("Welcomer");
        });
        ezOnlyPlayers.setVisible(() -> {
            String m = mode.getValue();
            return m.equals("All") || m.equals("AutoEZ");
        });
        ezDelay.setVisible(() -> {
            String m = mode.getValue();
            return m.equals("All") || m.equals("AutoEZ");
        });
    }
    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        String m = mode.getValue();
        if (m.equals("All") || m.equals("Announcer")) {
            if (mc.player != null) {
                lastX = mc.player.getX();
                lastZ = mc.player.getZ();
                lastFoodLevel = mc.player.getFoodData().getFoodLevel();
            }
            blocksWalked = 0; foodEaten = 0; hitsDealt = 0; tickCounter = 0;
        }
        if (m.equals("All") || m.equals("Welcomer")) {
            knownPlayers.clear();
            if (mc.level != null) {
                for (Player p : mc.level.players()) {
                    knownPlayers.add(p.getUUID());
                }
            }
        }
    }
    public void onHit() {
        if (!getEnabled()) return;
        String m = mode.getValue();
        if (!m.equals("All") && !m.equals("Announcer")) return;
        if (announceHit.getValue()) {
            hitsDealt++;
        }
    }
    public void onPlayerDeath(Player victim, DamageSource source) {
        if (!getEnabled()) return;
        String m = mode.getValue();
        if (!m.equals("All") && !m.equals("AutoEZ")) return;
        if (victim == Minecraft.getInstance().player) return;
        if (ezOnlyPlayers.getValue() && !(victim instanceof Player)) return;
        Entity killer = source.getEntity();
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
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || p.connection == null) return;
        String m = mode.getValue();
        if (m.equals("All") || m.equals("Announcer")) {
            tickAnnouncer(mc, p);
        }
        if (m.equals("All") || m.equals("Welcomer")) {
            tickWelcomer(mc, p);
        }
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
            if (tickCounter >= intervalTicks) {
                doAnnounce(p);
                tickCounter = 0;
            }
        } else {
            checkMilestone(100, p);  checkMilestone(500, p);
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
        if (announceWalk.getValue() && blocksWalked >= 1.0) {
            sb.append("Walked ").append(String.format("%.0f", blocksWalked)).append("b. ");
            added = true;
        }
        if (announceEat.getValue() && foodEaten > 0) {
            sb.append("Ate ").append(foodEaten).append("x. ");
            added = true;
        }
        if (announceHit.getValue() && hitsDealt > 0) {
            sb.append("Hit ").append(hitsDealt).append("x. ");
            added = true;
        }
        if (added) {
            p.connection.sendChat(sb.toString());
        }
        blocksWalked = 0;
        foodEaten = 0;
        hitsDealt = 0;
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
}
