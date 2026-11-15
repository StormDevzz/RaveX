package ravex.modules.client;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.manager.NotificationManager;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.PotionUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.PlayerUtility;
import net.minecraft.network.chat.Component;

import net.minecraft.world.entity.item.ItemEntity;
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
import ravex.modules.Modules;
@Module(name = "Notifications", category = "Client", enabled = true)
public class Notifications {
    @Parameter(name = "Mode", modes = {"Text", "Toast"})
    public String mode = "Toast";
    @Parameter(name = "ClientAlerts", modes = {"Text", "Toast"})
    public String clientAlerts = "Text";
    @Parameter(name = "VisualRange", modes = {"Off", "Text", "Toast"})
    public String visualRange = "Toast";
    @Parameter(name = "ItemCollection", modes = {"Off", "Toast", "Text"})
    public String itemCollection = "Off";
    @Parameter(name = "Tracker", modes = {"Off", "Toast", "Text"})
    public String tracker = "Off";
    @Parameter(name = "PopCounter", modes = {"Off", "Toast", "Text"})
    public String popCounter = "Toast";
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
    public boolean itemSelf = true;

    private final List<String> knownPlayers = new ArrayList<>();
    private final Map<Integer, ItemEntry> trackedItems = new HashMap<>();
    private final Map<String, PlayerState> playerStates = new HashMap<>();
    private final Map<String, Integer> popCounts = new HashMap<>();

    private record ItemEntry(double x, double y, double z, ItemStack stack) {}
    private static class PlayerState {
        ItemStack usingItem = ItemStack.EMPTY;
        boolean hasTotem = false;
        boolean hasShield = false;
        float lastHealth = 20;
        int handCrystals = 0;
        int egapples = 0;
        int gapples = 0;
        String potionName = "";
        boolean hasShieldItem = false;
        boolean hasTotemItem = false;
        long lastCrystals = 0L;
        long lastEgap = 0L;
        long lastGapple = 0L;
        long lastPotion = 0L;
        long lastShield = 0L;
        long lastTotem = 0L;
    }

    private void notifyOpt(String modeVal, String text, int color) {
        notifyOpt(modeVal, text, color, ItemStack.EMPTY);
    }

    private void notifyOpt(String modeVal, String text, int color, ItemStack icon) {
        if ("Toast".equals(modeVal)) {
            if (icon != null && !icon.isEmpty()) {
                NotificationManager.addToast(text, color, NotificationManager.ToastType.INFO,
                        (float) toastOpacity, (int) toastSize, icon);
            } else {
                NotificationManager.addToast(text, color, true, (float) toastOpacity, (int) toastSize);
            }
        } else if ("Text".equals(modeVal)) {
            var mc = MinecraftWrapper.getWrapper();
            if (mc.getPlayer() != null) mc.getPlayer().displayClientMessage(Component.literal(text), false);
        }
    }

    public static void notifyByMode(String text, int color) {
        Notifications notifications = Modules.get(Notifications.class);
        var mc = MinecraftWrapper.getWrapper();
        String mode = notifications != null && Modules.enabled(Notifications.class) ? notifications.mode : "Text";
        if ("Toast".equals(mode) && notifications != null) {
            NotificationManager.addToast(ravex.utility.client.ClientAlertUtility.stripFormatting(text), color,
                    NotificationManager.ToastType.INFO, (float) notifications.toastOpacity, (int) notifications.toastSize);
        } else if (mc.getPlayer() != null) {
            mc.getPlayer().displayClientMessage(Component.literal(text), false);
        }
    }
    public void onEnable() {
        knownPlayers.clear();
        trackedItems.clear();
        playerStates.clear();
        popCounts.clear();
    }

    @ravex.event.Subscribe
    public void onTotemPop(ravex.event.combat.TotemPopEvent event) {
        if ("Off".equals(popCounter)) return;
        var popped = event.getPlayer();
        if (popped == null) return;
        var self = MinecraftWrapper.getWrapper().getPlayer();
        if (popped == self) return;
        String name = popped.getName().getString();
        int count = popCounts.getOrDefault(name, 1);
        popCounts.put(name, count + 1);
        notifyOpt(popCounter, ravex.utility.misc.LanguageUtility.t("pop_count", name, count), 0xFFFFAA00);
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
                    boolean monster = EntityUtility.isHostile(le);
                    boolean animal = EntityUtility.isPassive(le);
                    if (!itemSelf && self) continue;
                    if (!itemPlayers && player) continue;
                    if (!itemMonsters && monster) continue;
                    if (!itemAnimals && animal) continue;
                    if (d < nearestDist) {
                        nearest = le;
                        nearestDist = d;
                    }
                }
                if (nearest != null && nearestDist < 4.0) {
                    boolean self = nearest == mc.getPlayer();
                    String name = nearest instanceof net.minecraft.world.entity.player.Player p ? p.getName().getString() : nearest.getType().getDescription().getString();
                    String itemLabel = itemName;
                    if (entry.stack.getCount() > 1) itemLabel += " x" + entry.stack.getCount();
                    String message = self
                            ? ravex.utility.misc.LanguageUtility.t("pickup_self", itemLabel)
                            : ravex.utility.misc.LanguageUtility.t("pickup", name, itemLabel);
                    notifyOpt(ic, message, 0xFFDAA520, entry.stack);
                }
            }
        }
        for (int id : removed) trackedItems.remove(id);
    }

    private void tickTracker(MinecraftWrapper mc) {
        String tr = tracker;
        if ("Off".equals(tr)) return;
        net.minecraft.world.entity.player.Player target = findTrackerTarget(mc);
        if (target == null) return;

        String name = target.getName().getString();
        PlayerState state = playerStates.computeIfAbsent(name, k -> new PlayerState());

        ItemStack using = target.getUseItem();
        if (!using.isEmpty() && !ItemStack.isSameItemSameComponents(using, state.usingItem)) {
            if (InventoryUtility.isGoldenApple(using)) {
                notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("ate_gapple", name), 0xFFFFAA00);
            } else if (InventoryUtility.isEnchantedGoldenApple(using)) {
                notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("ate_egapple", name), 0xFFFF55FF);
            } else if (InventoryUtility.isPotion(using) || using.is(Items.SPLASH_POTION) || using.is(Items.LINGERING_POTION)) {
                notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("drank", name, cleanPotionName(using)), 0xFF00AAFF);
            }
        }
        state.usingItem = using.copy();

        boolean nowHasTotem = InventoryUtility.isTotem(target.getMainHandItem()) || InventoryUtility.isTotem(target.getOffhandItem());
        if (state.hasTotem && !nowHasTotem && PlayerUtility.getHealth(target) <= 0.5f) {
            notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("pop_totem", name), 0xFFFF4444);
        }
        state.hasTotem = nowHasTotem;

        boolean nowHasShield = target.getMainHandItem().is(Items.SHIELD) || target.getOffhandItem().is(Items.SHIELD);
        if (state.hasShield && !nowHasShield && state.lastHealth - PlayerUtility.getHealth(target) < 0.01f) {
            notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("shield_broke", name), 0xFF888888);
        }
        state.hasShield = nowHasShield;
        state.lastHealth = PlayerUtility.getHealth(target);

        int crystals = handCount(target, Items.END_CRYSTAL);
        long now = System.currentTimeMillis();
        if (crystals > state.handCrystals && crystals > 0 && now - state.lastCrystals >= 3000L) {
            notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("tracker_crystals", name, crystals), 0xFF00E5FF);
            state.lastCrystals = now;
        }
        state.handCrystals = crystals;

        int egapples = InventoryUtility.countItem(target, Items.ENCHANTED_GOLDEN_APPLE);
        if (egapples > state.egapples && egapples > 0 && now - state.lastEgap >= 3000L) {
            notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("tracker_egapple", name, egapples), 0xFFFF55FF);
            state.lastEgap = now;
        }
        state.egapples = egapples;

        int gapples = InventoryUtility.countItem(target, Items.GOLDEN_APPLE);
        if (gapples > state.gapples && gapples > 0 && now - state.lastGapple >= 3000L) {
            notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("tracker_gapple", name, gapples), 0xFFFFAA00);
            state.lastGapple = now;
        }
        state.gapples = gapples;

        String potion = findPotion(target);
        if (!potion.isEmpty() && !potion.equals(state.potionName) && now - state.lastPotion >= 3000L) {
            notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("tracker_potion", name, potion), 0xFF00AAFF);
            state.lastPotion = now;
        }
        state.potionName = potion;

        boolean shieldItem = hasItem(target, Items.SHIELD);
        if (shieldItem && !state.hasShieldItem && now - state.lastShield >= 5000L) {
            notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("tracker_shield", name), 0xFFAAAAAA);
            state.lastShield = now;
        }
        state.hasShieldItem = shieldItem;

        boolean totemItem = hasItem(target, Items.TOTEM_OF_UNDYING);
        if (totemItem && !state.hasTotemItem && now - state.lastTotem >= 5000L) {
            notifyOpt(tr, ravex.utility.misc.LanguageUtility.t("tracker_totem", name), 0xFFFFAA00);
            state.lastTotem = now;
        }
        state.hasTotemItem = totemItem;
    }

    private net.minecraft.world.entity.player.Player findTrackerTarget(MinecraftWrapper mc) {
        if (mc.getCrosshairPickEntity() instanceof net.minecraft.world.entity.player.Player aimed
                && aimed != mc.getPlayer() && aimed.isAlive()) {
            return aimed;
        }
        net.minecraft.world.entity.player.Player best = null;
        double bestDistance = 20.0 * 20.0;
        for (net.minecraft.world.entity.player.Player p : mc.getLevel().players()) {
            if (p == mc.getPlayer() || !p.isAlive()) continue;
            double distance = p.distanceToSqr(mc.getPlayer());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = p;
            }
        }
        return best;
    }

    private int handCount(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item item) {
        int count = 0;
        if (player.getMainHandItem().is(item)) count += player.getMainHandItem().getCount();
        if (player.getOffhandItem().is(item)) count += player.getOffhandItem().getCount();
        return count;
    }

    private boolean hasItem(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item item) {
        return InventoryUtility.countItem(player, item) > 0 || player.getOffhandItem().is(item);
    }

    private String findPotion(net.minecraft.world.entity.player.Player player) {
        String fallback = "";
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!InventoryUtility.isPotion(stack)) continue;
            String name = cleanPotionName(stack);
            if (name.toLowerCase().contains("strength")) return name;
            if (fallback.isEmpty()) fallback = name;
        }
        if (InventoryUtility.isPotion(player.getOffhandItem())) {
            return cleanPotionName(player.getOffhandItem());
        }
        return fallback;
    }

    private String cleanPotionName(ItemStack stack) {
        return PotionUtility.getPotionName(stack).replaceAll("§.", "");
    }

    private boolean canPickUpItems(net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.player.Player) return true;
        if (EntityUtility.isHostile(entity)) return true;
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
        if (!Modules.enabled(Notifications.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        int color = Modules.get(Notifications.class).messageColor;
        if (Modules.get(Notifications.class).mode.equals("Toast")) {
            NotificationManager.addToast(module.getName(), color, enabled, Modules.get(Notifications.class).toastOpacity, (int) Modules.get(Notifications.class).toastSize);
            return;
        }
        String action = enabled ? ravex.utility.misc.LanguageUtility.t("enabled") : ravex.utility.misc.LanguageUtility.t("disabled");
        if (mc.getPlayer() != null) {
            Component message = Component.literal("[")
                .withStyle(style -> style.withColor(0x7F7F7F))
                .append(Component.literal("RaveX").withStyle(style -> style.withColor(color)))
                .append(Component.literal("] ").withStyle(style -> style.withColor(color)))
                .append(Component.literal(module.getName()).withStyle(style -> style.withColor(color)))
                .append(Component.literal(" " + ravex.utility.misc.LanguageUtility.t("has_been") + " ").withStyle(style -> style.withColor(0x7F7F7F)))
                .append(Component.literal(action).withStyle(style -> style.withColor(color)))
                .append(Component.literal(".").withStyle(style -> style.withColor(0x7F7F7F)));
            mc.getPlayer().displayClientMessage(message, false);
        }
    }






}
