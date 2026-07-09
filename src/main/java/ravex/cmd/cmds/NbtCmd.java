package ravex.cmd.cmds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.utility.network.NetworkUtility;

public class NbtCmd extends Cmd {
    public NbtCmd() {
        super("nbt", "Show detailed NBT data of held item", "iteminfo");
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) {
            stack = mc.player.getOffhandItem();
            if (stack.isEmpty()) {
                CmdReg.print(this, "§cHold an item in your hand.");
                return;
            }
        }

        String itemName = stack.getDisplayName().getString();
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        int count = stack.getCount();
        int maxDamage = stack.getMaxDamage();
        int damage = maxDamage - stack.getDamageValue();

        CmdReg.print(this, "§lItem Data");
        CmdReg.print("§7Name: §e" + itemName);
        CmdReg.print("§7ID: §e" + (id != null ? id.toString() : "unknown"));
        CmdReg.print("§7Count: §e" + count);
        if (maxDamage > 1) {
            CmdReg.print("§7Durability: §e" + damage + "§7/§e" + maxDamage);
        }
        CmdReg.print("");

        String components = NetworkUtility.formatComponents(stack);
        if (components.isEmpty()) {
            CmdReg.print(this, "§eNo components on this item.");
            return;
        }

        String[] lines = components.split("\n");
        int maxLines = Math.min(lines.length, 80);
        for (int i = 0; i < maxLines; i++) {
            CmdReg.print("§8" + lines[i]);
        }
        if (lines.length > 80) {
            CmdReg.print("§8... and " + (lines.length - 80) + " more components");
        }
    }
}
