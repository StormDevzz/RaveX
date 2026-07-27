package ravex.modules.client;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.manager.NotificationManager;
import ravex.utility.misc.MobUtility;
import ravex.utility.misc.PotionUtility;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.network.chat.Component;

import net.minecraft.world.entity.item.ItemEntity;
import ravex.utility.misc.EntityUtility;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "Notifications", category = "Client")
public class Notifications implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Text", "Toast"})
    public String mode = "Toast";
    @Parameter(name = "VisualRange", modes = {"Off", "Text", "Toast"})
    public String visualRange = "Toast";
    @Parameter(name = "ItemCollection", modes = {"Off", "Toast", "Text"})
    public String itemCollection = "Off";
    @Parameter(name = "Tracker", modes = {"Off", "Toast", "Text"})
    public String tracker = "Off";
    @Parameter(name = "MessageColor", color = true)
    public int messageColor = 0xFF0066FF;
    @Parameter(name = "ToastOpacity", min = 0.25, max = 1.0, step = 0.05)
    public double toastOpacity = 0.25;
    @Parameter(name = "ToastSize", min = 12.0, max = 32.0, step = 1.0)
    public double toastSize = 16.0;
    @Parameter(name = "Monsters")
    public boolean itemMonsters = true;
    @Parameter(name = "Animals")
    public boolean itemAnimals = true;
    @Parameter(name = "Players")
    public boolean itemPlayers = true;
    @Parameter(name = "Self")
    public boolean itemSelf = false;

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

    private Notifications() {
        
        ravex.manager.ModuleManager.INSTANCE.getByName("Notifications").setEnabled(true);
    }

    private void notifyOpt(String modeVal, String text, int color) {
        if ("Toast".equals(modeVal)) {
            NotificationManager.addToast(text, color, true, (float) toastOpacity, (int) toastSize);
        } else if ("Text".equals(modeVal)) {
            var mc = MinecraftWrapper.getWrapper();
            if (mc.getPlayer() != null) mc.getPlayer().displayClientMessage(Component.literal(text), false);
        }
    }
    public void onEnable() {
        knownPlayers.clear();
        trackedItems.clear();
        playerStates.clear();
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null || mc.getPlayer() == null) return;
        tickVisualRange(mc);
        tickItemCollection(mc);
        tickTracker(mc);
    }

    private void tickVisualRange(MinecraftWrapper mc) {
        String vr = visualRange;
        if ("Off".equals(vr)) return;
        List<String> currentPlayers = new ArrayList<>();
        for (net.minecraft.world.entity.player.Player p : mc.getLevel().players()) {
            if (p == mc.getPlayer()) continue;
            String name = p.getName().getString();
            currentPlayers.add(name);
            if (!knownPlayers.contains(name)) {
                notifyOpt(vr, ravex.utility.misc.LanguageUtility.t("entered", name), messageColor);
            }
        }
        for (String name : knownPlayers) {
            if (!currentPlayers.contains(name)) {
                notifyOpt(vr, ravex.utility.misc.LanguageUtility.t("left", name), messageColor);
            }
        }
        knownPlayers.clear();
        knownPlayers.addAll(currentPlayers);
    }

    private void tickItemCollection(MinecraftWrapper mc) {
        String ic = itemCollection;
        if ("Off".equals(ic)) return;
        AABB range = new AABB(mc.getPlayer().blockPosition()).inflate(64);
        Set<Integer> currentIds = new HashSet<>();
        for (ItemEntity item : mc.getLevel().getEntitiesOfClass(ItemEntity.class, range)) {
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
                net.minecraft.world.entity.LivingEntity nearest = null;
                double nearestDist = Double.MAX_VALUE;
                AABB pickRange = new AABB(entry.x - 2, entry.y - 2, entry.z - 2, entry.x + 2, entry.y + 2, entry.z + 2);
                for (net.minecraft.world.entity.LivingEntity le : mc.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, pickRange)) {
                    if (!canPickUpItems(le)) continue;
                    double d = le.distanceToSqr(entry.x, entry.y, entry.z);
                    boolean self = le == mc.getPlayer();
                    boolean player = le instanceof net.minecraft.world.entity.player.Player && !self;
                    boolean monster = MobUtility.isHostile(le);
                    boolean animal = MobUtility.isPassive(le);
                    if (!itemSelf && self) continue;
                    if (!itemPlayers && player) continue;
                    if (!itemMonsters && monster) continue;
                    if (!itemAnimals && animal) continue;
                    if (d < nearestDist) {
                        nearest = le;
                        nearestDist = d;
                    }
                }
                if (nearest != null && nearestDist < 2.25) {
                    String name = nearest instanceof net.minecraft.world.entity.player.Player p ? p.getName().getString() : nearest.getType().getDescription().getString();
                    notifyOpt(ic, ravex.utility.misc.LanguageUtility.t("pickup", name, itemName), 0xFFDAA520);
                }
            }
        }
        for (int id : removed) trackedItems.remove(id);
    }

    private void tickTracker(MinecraftWrapper mc) {
        String tr = tracker;
        if ("Off".equals(tr)) return;
        for (net.minecraft.world.entity.player.Player p : mc.getLevel().players()) {
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

    private boolean canPickUpItems(net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.player.Player) return true;
        if (MobUtility.isHostile(entity)) return true;
        if (entity instanceof net.minecraft.world.entity.npc.villager.Villager) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.Llama) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.Donkey) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.Mule) return true;
        return false;
    }

    private boolean hasItemInHands(net.minecraft.world.entity.player.Player p, net.minecraft.world.item.Item item) {
        return p.getMainHandItem().is(item) || p.getOffhandItem().is(item);
    }

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

    public static void notifyToggle(ravex.modules.Module module, boolean enabled) {
        if (!ravex.manager.ModuleManager.delegate(Notifications.class).getEnabled()) return;
        var mc = MinecraftWrapper.getWrapper();
        int color = ravex.manager.ModuleManager.delegate(Notifications.class).messageColor;
        if (ravex.manager.ModuleManager.delegate(Notifications.class).mode.equals("Toast")) {
            NotificationManager.addToast(module.getName(), color, enabled, ravex.manager.ModuleManager.delegate(Notifications.class).toastOpacity, (int) ravex.manager.ModuleManager.delegate(Notifications.class).toastSize);
            return;
        }
        String action = enabled ? "Enabled" : "Disabled";
        if (mc.getPlayer() != null) {
            Component message = Component.literal("[")
                .withStyle(style -> style.withColor(0x7F7F7F))
                .append(Component.literal("RaveX").withStyle(style -> style.withColor(color)))
                .append(Component.literal("] ravex.modules.Module ").withStyle(style -> style.withColor(color)))
                .append(Component.literal(module.getName()).withStyle(style -> style.withColor(color)))
                .append(Component.literal(" has been ").withStyle(style -> style.withColor(0x7F7F7F)))
                .append(Component.literal(action).withStyle(style -> style.withColor(color)))
                .append(Component.literal(".").withStyle(style -> style.withColor(0x7F7F7F)));
            mc.getPlayer().displayClientMessage(message, false);
        }
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Notifications").getEnabled();
    }

    public static Notifications itz() {
        return ravex.manager.ModuleManager.delegate(Notifications.class);
    }


}