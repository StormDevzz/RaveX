package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.modules.client.Commands;
import ravex.manager.ModuleManager;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import java.util.HashSet;
import java.util.Set;
public class HelpCmd extends Cmd {
    public HelpCmd() {
        super("help", "Show this list");
    }
    @Override
    public void execute(String[] args) {
        String p = ModuleManager.get(Commands.class).prefix;
        var player = MinecraftWrapper.getWrapper().getPlayer();
        if (player == null) return;

        // vanilla chat look: gray dashes, clickable command
        player.displayClientMessage(Component.literal("--- ")
            .append(Component.literal("RaveX commands").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" ---").withStyle(ChatFormatting.GRAY)), false);

        Set<Cmd> printed = new HashSet<>();
        java.util.List<Cmd> sorted = new java.util.ArrayList<>();
        for (Cmd cmd : CmdReg.INSTANCE.getCommands().values()) {
            if (printed.add(cmd)) sorted.add(cmd);
        }
        sorted.sort(java.util.Comparator.comparing(Cmd::getName));
        for (Cmd cmd : sorted) {
            String usage = p + cmd.getName();
            MutableComponent line = Component.literal(usage).withStyle(Style.EMPTY
                .withColor(ChatFormatting.WHITE)
                .withClickEvent(new ClickEvent.SuggestCommand(usage + " "))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to type"))));
            line.append(Component.literal(" - " + cmd.getDescription()).withStyle(ChatFormatting.GRAY));
            player.displayClientMessage(line, false);
        }
    }
}
