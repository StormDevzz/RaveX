package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Welcomer extends Module {
    public static final Welcomer INSTANCE = new Welcomer();

    public final BooleanParameter onlyFirstJoin = new BooleanParameter("First Join Only", true);

    private final Set<UUID> knownPlayers = new HashSet<>();
    private final String[] messages = new String[] {
        "Sup %s",
        "Yo %s!",
        "What's good %s?",
        "Ayy %s is in the house!",
        "Bruh %s just joined",
        "Holy shit %s is here",
        "Damn %s finally joined",
        "%s what's poppin'?",
        "Yo yo yo %s!",
        "Ey yo %s!",
        "Shit %s pulled up",
        "%s just rolled in",
        "%s is finally here, bout damn time",
        "What's up %s my G",
        "Oh damn %s joined",
        "%s in the building",
        "%s just spawned",
        "Yo %s what it do",
        "%s has entered the server",
        "Ayyy %s",
        "%s just logged in, everyone hide!",
        "Oh fuck %s is here",
        "%s my guy what's up",
        "Yooo %s!",
        "%s joined, party time!",
        "%s just appeared outta nowhere",
        "Here comes %s",
        "%s is back at it again",
        "Damnnn %s in the chat",
        "Ayo %s!",
        "What's cracking %s",
        "%s just joined the squad",
        "Yo check it, %s is here"
    };

    private Welcomer() {
        super("Welcomer", Category.MISC);
        addParameter(onlyFirstJoin);
    }

    @Override
    protected void onEnable() {
        knownPlayers.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            for (Player p : mc.level.players()) {
                knownPlayers.add(p.getUUID());
            }
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (me == null || mc.level == null || me.connection == null) return;

        for (Player p : mc.level.players()) {
            if (p == me) continue;
            if (knownPlayers.contains(p.getUUID())) continue;

            knownPlayers.add(p.getUUID());
            String name = p.getGameProfile().name();
            int idx = me.getRandom().nextInt(messages.length);
            String msg = String.format(messages[idx], name);

            if (!onlyFirstJoin.getValue() || knownPlayers.size() <= 1) {
                
            }
            me.connection.sendCommand(msg);
        }
    }
}
