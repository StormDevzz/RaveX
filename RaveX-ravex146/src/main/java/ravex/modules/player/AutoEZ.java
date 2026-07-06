package ravex.modules.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;

import java.util.List;
import java.util.Random;

public class AutoEZ extends Module {
    public static final AutoEZ INSTANCE = new AutoEZ();

    public final BooleanParameter onlyPlayers = new BooleanParameter("Only Players", true);
    public final NumberParameter delay = new NumberParameter("Delay (ms)", 500.0, 0.0, 3000.0, 100.0);

    private static final List<String> PHRASES = List.of(
            "ez %s bruh",
            "get rekt %s",
            "sit down %s",
            "L %s",
            "cope harder %s",
            "absolute bot %s",
            "fucking noob %s",
            "go back to bed %s",
            "LMAO %s",
            "ur trash %s",
            "stay mad %s",
            "rekt %s ez",
            "you're actually dogshit %s",
            "free kill %s",
            "lol get owned %s",
            "%s just got clapped",
            "think before you speak %s",
            "uninstall %s",
            "breathe through your nose %s",
            "actual bot behavior %s",
            "%s more like L%s",
            "kys... kidding, just get better %s",
            "dogwater %s",
            "negative KD %s",
            "%s died for what",
            "clear skill issue %s",
            "gapped %s",
            "botted %s",
            "outplayed %s",
            "%s u good?",
            "motherless behavior %s",
            "touch grass %s",
            "quit the game %s"
    );

    private final Random random = new Random();
    private long lastKillTime = 0;

    private AutoEZ() {
        super("AutoEZ", Category.PLAYER);
        addParameter(onlyPlayers);
        addParameter(delay);
    }

    public void onPlayerDeath(Player victim, DamageSource source) {
        if (!getEnabled()) return;
        if (victim == Minecraft.getInstance().player) return;
        if (onlyPlayers.getValue() && !(victim instanceof Player)) return;

        Entity killer = source.getEntity();
        if (killer != Minecraft.getInstance().player) return;

        long now = System.currentTimeMillis();
        if (now - lastKillTime < delay.getValue().longValue()) return;
        lastKillTime = now;

        String name = victim.getName().getString();
        String phrase = String.format(PHRASES.get(random.nextInt(PHRASES.size())), name);

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.sendChat(phrase);
        }
    }
}
