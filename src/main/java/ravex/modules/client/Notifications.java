package ravex.modules.client;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
=======
import ravex.modules.Category;
import ravex.modules.Module;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.manager.NotificationManager;
<<<<<<< HEAD
import ravex.utility.misc.MobUtility;
import ravex.utility.misc.PotionUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
public class Notifications extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Toast", List.of("Text", "Toast"));
    public final ModeParameter visualRange = new ModeParameter("VisualRange", "Toast", List.of("Off", "Text", "Toast"));
    public final ModeParameter itemCollection = new ModeParameter("ItemCollection", "Off", List.of("Off", "Toast", "Text"));
    public final ModeParameter tracker = new ModeParameter("Tracker", "Off", List.of("Off", "Toast", "Text"));
    public final ColorParameter messageColor = new ColorParameter("MessageColor", 0xFF0066FF);
    public final NumberParameter toastOpacity = new NumberParameter("ToastOpacity", 0.25, 0.25, 1.0, 0.05);
    public final NumberParameter toastSize = new NumberParameter("ToastSize", 16.0, 12.0, 32.0, 1.0);
    public final BooleanParameter itemMonsters = new BooleanParameter("Monsters", true);
    public final BooleanParameter itemAnimals = new BooleanParameter("Animals", true);
    public final BooleanParameter itemPlayers = new BooleanParameter("Players", true);
    public final BooleanParameter itemSelf = new BooleanParameter("Self", false);

    private final List<String> knownPlayers = new ArrayList<>();
    private final Map<Integer, ItemEntry> trackedItems = new HashMap<>();
    private final Map<String, PlayerState> playerStates = new HashMap<>();

    private record ItemEntry(double x, double y, double z, ItemStack stack) {}
    private static class PlayerState {
        ItemStack usingItem = ItemStack.EMPTY;
        boolean hasTotem = false;
        boolean hasShield = false;
        float lastHealth = 20;
    }

=======
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.List;
public class Notifications extends Module {
    public static final Notifications INSTANCE = new Notifications();
    public final ModeParameter mode = new ModeParameter("Mode", "Text", List.of("Text", "Toast"));
    public final ColorParameter messageColor = new ColorParameter("MessageColor", 0xFF0066FF);
    public final NumberParameter toastOpacity = new NumberParameter("ToastOpacity", 0.85, 0.25, 1.0, 0.05);
    public final NumberParameter toastSize = new NumberParameter("ToastSize", 16.0, 12.0, 32.0, 1.0);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private Notifications() {
        super("Notifications");
        setEnabled(true);
        toastOpacity.setVisible(() -> mode.getValue().equals("Toast"));
        toastSize.setVisible(() -> mode.getValue().equals("Toast"));
<<<<<<< HEAD
        itemMonsters.setVisible(() -> !"Off".equals(itemCollection.getValue()));
        itemAnimals.setVisible(() -> !"Off".equals(itemCollection.getValue()));
        itemPlayers.setVisible(() -> !"Off".equals(itemCollection.getValue()));
        itemSelf.setVisible(() -> !"Off".equals(itemCollection.getValue()));
    }

    private void notifyOpt(String modeVal, String text, int color) {
        if ("Toast".equals(modeVal)) {
            NotificationManager.addToast(text, color, true, toastOpacity.getValue().floatValue(), toastSize.getValue().intValue());
        } else if ("Text".equals(modeVal)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.player.displayClientMessage(Component.literal(text), false);
        }
    }

    @Override
    protected void onEnable() {
        knownPlayers.clear();
        trackedItems.clear();
        playerStates.clear();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        tickVisualRange(mc);
        tickItemCollection(mc);
        tickTracker(mc);
    }

    private void tickVisualRange(Minecraft mc) {
        String vr = visualRange.getValue();
        if ("Off".equals(vr)) return;
        List<String> currentPlayers = new ArrayList<>();
        for (Player p : mc.level.players()) {
            if (p == mc.player) continue;
            String name = p.getName().getString();
            currentPlayers.add(name);
            if (!knownPlayers.contains(name)) {
                notifyOpt(vr, ravex.utility.misc.LanguageUtility.t("entered", name), messageColor.getValue());
            }
        }
        for (String name : knownPlayers) {
            if (!currentPlayers.contains(name)) {
                notifyOpt(vr, ravex.utility.misc.LanguageUtility.t("left", name), messageColor.getValue());
            }
        }
        knownPlayers.clear();
        knownPlayers.addAll(currentPlayers);
    }

    private void tickItemCollection(Minecraft mc) {
        String ic = itemCollection.getValue();
        if ("Off".equals(ic)) return;
        AABB range = new AABB(mc.player.blockPosition()).inflate(64);
        Set<Integer> currentIds = new HashSet<>();
        for (ItemEntity item : mc.level.getEntitiesOfClass(ItemEntity.class, range)) {
            int id = item.getId();
            currentIds.add(id);
            trackedItems.put(id, new ItemEntry(item.getX(), item.getY(), item.getZ(), item.getItem().copy()));
        }
        List<Integer> removed = new ArrayList<>();
        for (Map.Entry<Integer, ItemEntry> e : trackedItems.entrySet()) {
            if (!currentIds.contains(e.getKey())) {
                removed.add(e.getKey());
                ItemEntry entry = e.getValue();
                String itemName = entry.stack.getHoverName().getString();
                LivingEntity nearest = null;
                double nearestDist = Double.MAX_VALUE;
                AABB pickRange = new AABB(entry.x - 2, entry.y - 2, entry.z - 2, entry.x + 2, entry.y + 2, entry.z + 2);
                for (LivingEntity le : mc.level.getEntitiesOfClass(LivingEntity.class, pickRange)) {
                    if (!canPickUpItems(le)) continue;
                    double d = le.distanceToSqr(entry.x, entry.y, entry.z);
                    boolean self = le == mc.player;
                    boolean player = le instanceof Player && !self;
                    boolean monster = MobUtility.isHostile(le);
                    boolean animal = MobUtility.isPassive(le);
                    if (!itemSelf.getValue() && self) continue;
                    if (!itemPlayers.getValue() && player) continue;
                    if (!itemMonsters.getValue() && monster) continue;
                    if (!itemAnimals.getValue() && animal) continue;
                    if (d < nearestDist) {
                        nearest = le;
                        nearestDist = d;
                    }
                }
                if (nearest != null && nearestDist < 2.25) {
                    String name = nearest instanceof Player p ? p.getName().getString() : nearest.getType().getDescription().getString();
                    notifyOpt(ic, ravex.utility.misc.LanguageUtility.t("pickup", name, itemName), 0xFFDAA520);
                }
            }
        }
        for (int id : removed) trackedItems.remove(id);
    }

    private void tickTracker(Minecraft mc) {
        String tr = tracker.getValue();
        if ("Off".equals(tr)) return;
        for (Player p : mc.level.players()) {
            String name = p.getName().getString();
            PlayerState state = playerStates.computeIfAbsent(name, k -> new PlayerState());

            ItemStack using = p.getUseItem();
            if (!using.isEmpty() && using != state.usingItem) {
                if (using.is(Items.GOLDEN_APPLE)) {
                    notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("ate_gapple", name), 0xFFFFAA00);
                } else if (using.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                    notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("ate_egapple", name), 0xFFFF55FF);
                } else if (using.is(Items.POTION) || using.is(Items.SPLASH_POTION) || using.is(Items.LINGERING_POTION)) {
                    String effect = PotionUtility.getPotionName(using);
                    notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("drank", name, effect), 0xFF00AAFF);
                }
            }
            state.usingItem = using;

            boolean nowHasTotem = hasItemInHands(p, Items.TOTEM_OF_UNDYING);
            if (state.hasTotem && !nowHasTotem && p.getHealth() <= 0.5f) {
                notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("pop_totem", name), 0xFFFF4444);
            }
            state.hasTotem = nowHasTotem;

            boolean nowHasShield = hasItemInHands(p, Items.SHIELD);
            if (state.hasShield && !nowHasShield && state.lastHealth - p.getHealth() < 0.01f) {
                notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("shield_broke", name), 0xFF888888);
            }
            state.hasShield = nowHasShield;
            state.lastHealth = p.getHealth();
        }
    }

    private boolean canPickUpItems(LivingEntity entity) {
        if (entity instanceof Player) return true;
        if (MobUtility.isHostile(entity)) return true;
        if (entity instanceof net.minecraft.world.entity.npc.villager.Villager) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.Llama) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.Donkey) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.Mule) return true;
        return false;
    }

    private boolean hasItemInHands(Player p, net.minecraft.world.item.Item item) {
        return p.getMainHandItem().is(item) || p.getOffhandItem().is(item);
    }

=======
    }
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private static String argbToMcHex(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return "§x" +
            "§" + Character.forDigit((r >> 4) & 0xF, 16) +
            "§" + Character.forDigit(r & 0xF, 16) +
            "§" + Character.forDigit((g >> 4) & 0xF, 16) +
            "§" + Character.forDigit(g & 0xF, 16) +
            "§" + Character.forDigit((b >> 4) & 0xF, 16) +
            "§" + Character.forDigit(b & 0xF, 16);
    }
    public static void notifyToggle(Module module, boolean enabled) {
        if (!ModuleManager.get(Notifications.class).getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
<<<<<<< HEAD
        int color = ModuleManager.get(Notifications.class).messageColor.getValue();
        if (ModuleManager.get(Notifications.class).mode.getValue().equals("Toast")) {
            NotificationManager.addToast(module.getName(), color, enabled, ModuleManager.get(Notifications.class).toastOpacity.getValue().floatValue(), ModuleManager.get(Notifications.class).toastSize.getValue().intValue());
            return;
        }
        String action = enabled ? "Enabled" : "Disabled";
=======
        int color = INSTANCE.messageColor.getValue();
        String action = enabled ? "Enabled" : "Disabled";
        if (INSTANCE.mode.getValue().equals("Toast")) {
            NotificationManager.addToast(module.getName() + " " + action, color, enabled, INSTANCE.toastOpacity.getValue().floatValue(), INSTANCE.toastSize.getValue().intValue());
            return;
        }
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (mc.player != null) {
            Component message = Component.literal("[")
                .withStyle(style -> style.withColor(0x7F7F7F))
                .append(Component.literal("RaveX").withStyle(style -> style.withColor(color)))
<<<<<<< HEAD
                .append(Component.literal("] Module ").withStyle(style -> style.withColor(color)))
=======
                .append(Component.literal("] Module ").withStyle(style -> style.withColor(0x7F7F7F)))
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                .append(Component.literal(module.getName()).withStyle(style -> style.withColor(color)))
                .append(Component.literal(" has been ").withStyle(style -> style.withColor(0x7F7F7F)))
                .append(Component.literal(action).withStyle(style -> style.withColor(color)))
                .append(Component.literal(".").withStyle(style -> style.withColor(0x7F7F7F)));
            mc.player.displayClientMessage(message, false);
        }
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(Notifications.class);
    }

    public static Notifications itz() {
        return ModuleManager.get(Notifications.class);
    }
}
