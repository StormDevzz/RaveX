package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.StringParameter;
import ravex.utility.network.NetworkUtility;
import ravex.utility.render.ColorUtility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class MessageAura extends Module {
    public final NumberParameter range = new NumberParameter("Range", 10.0, 1.0, 100.0, 0.5);
    public final NumberParameter delay = new NumberParameter("Delay", 5.0, 0.0, 60.0, 1.0);
    public final ModeParameter mode = new ModeParameter("Mode", "Once", List.of("Once", "Repeat"));

    public final StringParameter messages = new StringParameter("Messages", "hello|hey|sup");
    public final BooleanParameter whisper = new BooleanParameter("Whisper", false);
    public final BooleanParameter swing = new BooleanParameter("Swing", true);
    public final BooleanParameter notifyChat = new BooleanParameter("NotifyChat", true);
    public final NumberParameter randDelay = new NumberParameter("RandDelay", 0.0, 0.0, 5.0, 0.5);
    public final ModeParameter targetMode = new ModeParameter("TargetMode", "All", List.of("All", "Single"));
    public final BooleanParameter antiBot = new BooleanParameter("AntiBot", true);

    private final Map<UUID, Long> sentPlayers = new HashMap<>();
    private long lastSentTime = 0;
    private int msgIndex = 0;
    private final Random random = new Random();

    private String displayTarget = "";
    private String displayMsg = "";
    private long displayTime = 0;

    private MessageAura() {
        super("MessageAura");
    }

    @Override
    protected void onEnable() {
        sentPlayers.clear();
        lastSentTime = 0;
        msgIndex = 0;
        displayTarget = "";
        displayMsg = "";
        displayTime = 0;
    }

    private String[] getMessageList() {
        return messages.getValue().split("\\|", -1);
    }

    private String pickMessage() {
        String[] list = getMessageList();
        if (list.length <= 1) {
            String msg = messages.getValue().trim();
            return msg.isEmpty() ? "hello" : msg;
        }
        String picked = list[msgIndex % list.length].trim();
        if (picked.isEmpty()) picked = list[0].trim();
        msgIndex++;
        return picked;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        long now = System.currentTimeMillis();
        long delayMs = (long) (delay.getValue() * 1000);
        double rd = randDelay.getValue();
        long randExtra = rd > 0 ? (long) (random.nextDouble() * rd * 1000) : 0;
        long totalDelay = delayMs + randExtra;
        boolean isOnce = "Once".equals(mode.getValue());

        for (Player target : mc.level.players()) {
            if (target == mc.player) continue;
            if (mc.player.distanceTo(target) > range.getValue()) continue;

            if (antiBot.getValue()) {
                var ab = ModuleManager.get(ravex.modules.combat.AntiBot.class);
                if (ab != null && ab.getEnabled() && ab.isBot(target)) continue;
            }

            if (isOnce) {
                if (sentPlayers.containsKey(target.getUUID())) continue;
            } else {
                Long lastSent = sentPlayers.get(target.getUUID());
                if (lastSent != null && (now - lastSent) < totalDelay) continue;
            }

            if ((now - lastSentTime) < totalDelay) continue;

            String playerName = target.getName().getString();
            String rawMsg = pickMessage();
            String msg = rawMsg.replace("{player}", playerName);

            if (whisper.getValue()) {
                msg = "/msg " + playerName + " " + msg;
            }

            if (mc.player.connection != null) {
                mc.player.connection.sendChat(msg);
            }

            if (swing.getValue() && mc.player != null) {
                mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }

            sentPlayers.put(target.getUUID(), now);
            lastSentTime = now;

            displayTarget = playerName;
            displayMsg = rawMsg;
            displayTime = now;

            if (notifyChat.getValue()) {
                NetworkUtility.displayClientMessage(
                    "§7[§5MessageAura§7] §d" + playerName + " §7<- §f" + rawMsg
                );
            }

            if ("Single".equals(targetMode.getValue())) break;
        }
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || displayTarget.isEmpty()) return;

        long elapsed = System.currentTimeMillis() - displayTime;
        if (elapsed > 3000) return;

        int alpha = (int) (255 * (1.0f - elapsed / 3000.0f));
        if (alpha < 0) alpha = 0;

        String text = displayTarget + " <- " + displayMsg;
        int x = mc.getWindow().getGuiScaledWidth() / 2;
        int y = mc.getWindow().getGuiScaledHeight() / 2 + 20;

        graphics.drawString(
            mc.font,
            text,
            x - mc.font.width(text) / 2,
            y,
            ColorUtility.withAlpha(0xFFFFFFFF, alpha),
            true
        );
    }

    public static MessageAura itz() {
        return ModuleManager.get(MessageAura.class);
    }
}
