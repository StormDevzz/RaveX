package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.event.combat.TotemPopEvent;
import ravex.event.player.DeathEvent;
import ravex.manager.FriendManager;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.StringParameter;

import java.util.*;

public class AutoEz extends Module {

    public final NumberParameter range = new NumberParameter("Range", 25.0, 0.0, 50.0, 1.0);
    public final NumberParameter tickDelay = new NumberParameter("Delay", 50.0, 0.0, 100.0, 1.0);
    public final BooleanParameter kill = new BooleanParameter("Kill", true);
    public final ModeParameter killMsgMode = new ModeParameter("KillMode", "RaveX", List.of("RaveX", "Toxic", "Custom"));
    public final StringParameter customKillMsg = new CustomStringParameter("KillMsg", "RaveX on top");
    public final BooleanParameter pop = new BooleanParameter("Pop", true);
    public final StringParameter popMsg = new CustomStringParameter("PopMsg", "I love it when you pop <NAME>");

    private final Random r = new Random();
    private int lastNum = -1;
    private int lastPop = -1;
    private boolean lastState = false;
    private String targetName = null;
    private final Deque<Message> messageQueue = new ArrayDeque<>();
    private int timer = 0;

    private static final String[] RAVEX_MESSAGES = {
        "RaveX fucked %s",
        "%s got destroyed by RaveX",
        "RaveX on top, %s on bottom",
        "%s just got RaveX'd",
        "EZ %s, RaveX diff",
        "%s stay mad, RaveX wins",
        "RaveX strong, %s weak",
        "Get RaveX'd %s",
        "%s better luck next time against RaveX",
        "RaveX > %s, always"
    };

    private static final String[] TOXIC_MESSAGES = {
        "Wow, you just died in a block game %s",
        "%s died in a block game lmfao.",
        "%s, your mother is of the homophobic type",
        "That's a Victory Royale! better luck next time, %s!",
        "my grandma plays minecraft better than you %s",
        "%s, you should look into purchasing vape",
        "Omg %s I'm so sorry",
        "that was a pretty bad move %s",
        "how does it feel to get stomped on %s",
        "%s, do you really like dying this much?",
        "hey %s, what does your IQ and kills have in common? They are both low",
        "Hey %s, want some PvP advice?",
        "wow, you just died in a game about legos",
        "%s I speak English not your gibberish.",
        "%s Take the L, kid",
        "%s got memed",
        "%s You died in a block game",
        "%s Trash, you barely even hit me.",
        "%s I just fucked him so hard he left the game",
        "%s get bent over and fucked",
        "%s couldn't even beat 4 block",
        "Thanks for the free kill %s!",
        "%s are you even trying?",
        "%s You. Are. Terrible.",
        "%s my mom is better at this game then you",
        "%s lol GG!!!",
        "%s gg ez kid",
        "Don't forget to report me %s",
        "Your IQ is that of a Steve %s",
        "%s ALT+F4 to remove the problem",
        "%s You'll eventually switch back to Fortnite again",
        "%s go back to fortnite where you belong",
        "L %s",
        "%s got rekt",
        "%s L",
        "%s even viv is better than you LMAO"
    };

    public AutoEz() {
        super("AutoEz");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastState = false;
        lastNum = -1;
        messageQueue.clear();
        timer = 0;
    }

    @Subscribe
    public void onTick(TickEvent.Client event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        timer++;

        if (kill.getValue() && anyDead(range.getValue())) {
            if (!lastState) {
                lastState = true;
                sendKillMessage();
            }
        } else {
            lastState = false;
        }

        if (timer >= tickDelay.getValue() && !messageQueue.isEmpty()) {
            Message msg = messageQueue.pollFirst();
            if (msg != null) {
                mc.player.connection.sendChat(msg.message);
                timer = 0;
                if (msg.isKill) messageQueue.clear();
            }
        }
    }

    @Subscribe
    public void onTotemPop(TotemPopEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!pop.getValue() || mc.player == null || mc.level == null) return;

        Player player = event.getPlayer();
        if (player == mc.player) return;
        if (FriendManager.INSTANCE.isFriend(player.getName().getString())) return;
        if (mc.player.distanceTo(player) > range.getValue()) return;

        sendPopMessage(player.getName().getString());
    }

    @Subscribe
    public void onDeath(DeathEvent event) {
        if (!kill.getValue() || event.isSelf()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player player = event.getPlayer();
        if (player == mc.player) return;
        if (FriendManager.INSTANCE.isFriend(player.getName().getString())) return;
        if (mc.player.distanceTo(player) > range.getValue()) return;

        targetName = player.getName().getString();
        if (!lastState) {
            lastState = true;
            sendKillMessage();
        }
    }

    private boolean anyDead(double range) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;

        for (Player pl : mc.level.players()) {
            if (pl != mc.player
                && !FriendManager.INSTANCE.isFriend(pl.getName().getString())
                && mc.player.distanceTo(pl) <= range
                && pl.getHealth() <= 0) {
                targetName = pl.getName().getString();
                return true;
            }
        }
        return false;
    }

    private void sendKillMessage() {
        switch (killMsgMode.getValue()) {
            case "RaveX" -> {
                int num = r.nextInt(RAVEX_MESSAGES.length);
                if (num == lastNum) num = (num < RAVEX_MESSAGES.length - 1) ? num + 1 : 0;
                lastNum = num;
                String msg = RAVEX_MESSAGES[num].replace("%s", targetName != null ? targetName : "You");
                messageQueue.addFirst(new Message(msg, true));
            }
            case "Toxic" -> {
                int num = r.nextInt(TOXIC_MESSAGES.length);
                if (num == lastNum) num = (num < TOXIC_MESSAGES.length - 1) ? num + 1 : 0;
                lastNum = num;
                String msg = TOXIC_MESSAGES[num].replace("%s", targetName != null ? targetName : "You");
                messageQueue.addFirst(new Message(msg, true));
            }
            case "Custom" -> {
                String custom = customKillMsg.getValue();
                if (custom != null && !custom.isEmpty()) {
                    String msg = custom.replace("%s", targetName != null ? targetName : "You");
                    messageQueue.addFirst(new Message(msg, true));
                }
            }
        }
    }

    private void sendPopMessage(String name) {
        String template = popMsg.getValue();
        if (template == null || template.isEmpty()) return;

        int num = r.nextInt(100);
        if (num == lastPop) num = (num < 99) ? num + 1 : 0;
        lastPop = num;

        String msg = template.replace("<NAME>", name);
        messageQueue.addLast(new Message(msg, false));
    }

    public static AutoEz itz() {
        return ModuleManager.get(AutoEz.class);
    }

    private record Message(String message, boolean isKill) {}

    private static class CustomStringParameter extends StringParameter {
        public CustomStringParameter(String name, String defaultValue) {
            super(name, defaultValue);
        }
    }
}
