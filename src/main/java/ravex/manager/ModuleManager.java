package ravex.manager;

import ravex.RaveX;
import ravex.modules.Category;
import ravex.modules.Module;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();

    private final List<Module> modules = new ArrayList<>();

    private ModuleManager() {
        combat(
            "KillAura", "AutoCrystal", "AimAssist", "Trigger", "MaceSwap",
            "WebAura", "AutoWeapon", "MaceAura", "Hitboxes",
            "Reach", "Surround", "SelfTrap", "BasePlace",
            "AnchorAura", "BowAim", "Breaker", "Quiver", "WindAura",
            "Trap", "AutoApple", "WebSelf", "KeyPearl", "PearlTarget",
            "NoHitDelay", "AntiPearl", "BedBomb", "Criticals", "AutoBow",
            "HoleFill", "AutoClicker", "AntiBot", "AutoDrop", "SelfFill",
            "Burrow", "AntiReGear", "TntAura", "WitherRoseAura",
            "KeepSprint", "ShieldFucker", "AutoCart", "AutoTotem"
        );

        render(
            "Crosshair", "ESP", "Skeleton", "NameTags", "Tracers",
            "NoBob", "Ambient", "Weather", "CustomFog",
            "Hud", "Shaders", "FreeLook", "FreeCam", "ViewClip",
            "Glint", "Sounds", "ItemPhysics", "Fullbright", "BlockOutline",
            "BreadCrumbs", "ViewModel", "Animations", "NoRender",
            "ShiftInterp", "BabyDude", "SkyColor", "CloudColor", "Trails",
            "Waypoint", "KillEffects", "Particles", "AspectRatio",
            "Borders", "Zoom", "HoleESP", "Blur", "ToolTips",
            "VoidESP"
        );

        player(
            "AutoTool", "NoInteract", "SourceFiller", "AirPlace",
            "AutoArmor", "AutoMend", "FastUse", "AutoRespawn",
            "FastBreak", "InstaBreak", "TabUtils", "ChestUtils",
            "MiddleClick", "ElytraUtils",
            "ViewLock", "ItemSaver", "AntiAim",
            "inventorycleaner.InventoryCleaner", "Replenish", "MobOwner", "NoSwing",
            "Swing", "GridBuilder", "Xray"
        );
        register(Category.PLAYER, "ravex.modules.player.autoregear.AutoReGear");

        movement(
            "GuiWalk", "NoSlowDown", "Velocity", "Step", "ReverseStep",
            "NoWeb", "AutoWalk", "AntiVoid", "LongJump", "Sleepy",
            "NoPush", "AutoSprint", "Spider", "Speed", "NoRotate",
            "Avoid", "RocketUtils", "RidingUtils", "SafeWalk",
            "HighJump", "FastStairs", "NoFall", "ElytraFly", "Flight",
            "LiquidCollision"
        );

        misc(
            "AntiAfk", "VisualRange", "AutoEat",
            "PacketUtils", "AutoLog", "Spammer",
            "DurabAlert", "AntiAttack", "LagNotify", "PopCounter",
            "WaxAura", "AutoReconnect", "FastItem",
            "BlockSelector", "AutoSoup", "NameProtect", "StashFinder",
            "AutoAuth", "AntiQuit", "CustomDeathText",
            "SoundBlocker", "ChatUtils",
            "CoordLogger", "BookUtils", "Religion",
            "Commands", "PauseBaritone", "PortalBuild"
        );

        world(
            "BoneMeal", "Scaffold", "AutoSign", "AutoShear",
            "AutoNameTag", "AutoMount", "FakePlayer", "ChestAura",
            "Igniter", "TreeCutter", "AutoTame", "AutoLight",
            "AutoReplant", "nuker.Nuker", "AutoTrade", "AutoFish", "AutoTunnel",
            "AutoSmelt", "AutoBrew", "ECFarmer", "NoGhostBlocks",
            "WitherBuild"
        );

        exploit(
            "RideExploit", "HandshakeSpoof", "GhostHand",
            "Timer", "PortalGui", "PortalGodMode", "MultiTask",
            "ClickTP", "GrimInstantMine", "ClickFly", "ChorusExploit",
            "TickShift", "TridentBoost", "Blink", "PacketFly",
            "FakePearl", "Phase", "RocketExtender", "RaytraceBypass",
            "PacketMine", "AntiHunger", "NewChunks",
            "NoMineAnimation", "PacketEat", "PingSpoof"
        );

        client(
            "RichPresence", "GuiParticles", "FastLatency", "Fonts",
            "Notifications", "DesktopGui", "Settings", "Calculator",
            "ClickGui", "BaritoneModule"
        );

        hud(
            "WatermarkHud", "ActiveModulesHud", "CoordsHud", "FpsHud",
            "NowPlayingHud", "ChatHud", "TpsHud", "CooldownsHud",
            "InvPreviewHud", "IndicatorsHud", "CurrencyHud", "ServerBrandHud"
        );
    }

    public List<Module> getClickGuiModules() {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) {
            if (!m.isHud()) list.add(m);
        }
        return list;
    }
    public List<Module> getHudModules() {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) {
            if (m.isHud()) list.add(m);
        }
        return list;
    }

    public List<Module> getByCategory(Category category) {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) {
            if (m.getCategory() == category) list.add(m);
        }
        return list;
    }

    public Module getByName(String name) {
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    public void init() {}

    public List<Module> getModules() { return modules; }

    public void onTick() {
        for (Module m : modules) {
            if (!m.isHud() && m.getEnabled()) m.onTick();
        }
    }

    private void combat(String... names)     { each(Category.COMBAT, "ravex.modules.combat", names); }
    private void render(String... names)     { each(Category.RENDER, "ravex.modules.render", names); }
    private void player(String... names)     { each(Category.PLAYER, "ravex.modules.player", names); }
    private void movement(String... names)   { each(Category.MOVEMENT, "ravex.modules.movement", names); }
    private void misc(String... names)       { each(Category.MISC, "ravex.modules.misc", names); }
    private void world(String... names)      { each(Category.WORLD, "ravex.modules.world", names); }
    private void exploit(String... names)    { each(Category.EXPLOIT, "ravex.modules.exploit", names); }
    private void client(String... names)     { each(Category.CLIENT, "ravex.modules.client", names); }
    private void hud(String... names)        { each(Category.HUD, "ravex.modules.hud", names); }

    private void each(Category category, String pkg, String... names) {
        for (String name : names) register(category, pkg + "." + name);
    }

    private void register(Category category, String fqn) {
        try {
            Class<?> clazz = Class.forName(fqn);
            Module instance = (Module) clazz.getField("INSTANCE").get(null);
            instance.setCategory(category);
            modules.add(instance);
        } catch (Exception e) {
            RaveX.LOGGER.error("Failed to register: {}", fqn, e);
        }
    }
}
