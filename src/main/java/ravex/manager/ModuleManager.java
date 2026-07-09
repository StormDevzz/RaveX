package ravex.manager;

import ravex.event.EventBusHolder;
import ravex.modules.Category;
import ravex.modules.Module;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager {
    private static final Map<String, String> SUBPACKAGES = Map.of(
        "InvClean", "player.invclean",
        "AutoReGear", "player.autoregear",
        "Nuker", "world.nuker",
        "Xray", "player"
    );

    public static final ModuleManager INSTANCE = new ModuleManager();

    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<?>, Module> byClass = new HashMap<>();

    private ModuleManager() {
        register(Category.COMBAT,
            "KillAura", "AutoCrystal", "AimAssist",
            "Trigger", "MaceSwap", "WebAura",
            "AutoWeapon", "MaceAura", "Hitboxes",
            "Reach", "Surround", "SelfTrap",
            "BasePlace", "AnchorAura", "BowAim",
            "Breaker", "Quiver", "WindAura",
            "Trap", "AutoApple", "WebSelf",
            "KeyPearl", "PearlTarget", "AntiPearl",
            "BedBomb", "Criticals", "AutoBow",
            "HoleFill", "AutoClicker", "AntiBot",
            "AutoDrop", "Burrow", "AntiReGear",
            "TntAura", "KeepSprint", "ShieldFucker",
            "AutoCart", "AutoTotem"
        );

        register(Category.RENDER,
            "Crosshair", "ESP", "Skeleton",
            "NameTags", "Tracers", "NoBob",
            "Ambient", "Weather", "Fog",
            "Shaders", "FreeLook", "FreeCam",
            "ViewClip", "Glint", "Sounds",
            "ItemPhysics", "Fullbright", "BlockOutline",
            "BreadCrumbs", "ViewModel", "Animations",
            "NoRender", "ShiftInterp", "SmallUser",
            "SkyColor", "CloudColor", "Trails",
            "Waypoint", "KillEffects", "Particles",
            "AspectRatio", "Borders", "Zoom",
            "ToolTips", "DeathText"
        );

        register(Category.PLAYER,
            "AutoTool", "NoInteract", "SourceFiller",
            "AirPlace", "AutoArmor", "AutoMend",
            "NoDelay", "AutoRespawn", "FastBreak",
            "TabHelper", "ChestHelper", "MiddleClick",
            "ElytraHelper", "ViewLock", "ItemSaver",
            "AntiAim", "invclean.InvClean", "Replenish",
            "MobOwner", "NoSwing", "Swing",
            "GridBuilder", "AntiHunger", "ChorusExploit",
            "GhostHand", "Handshake", "MultiTask",
            "MineAnimation", "PacketMine", "autoregear.AutoReGear"
        );

        register(Category.MOVEMENT,
            "GuiMove", "NoSlow", "Velocity",
            "Step", "ReverseStep", "NoWeb",
            "AutoWalk", "AntiVoid", "LongJump",
            "Sleepy", "NoPush", "AutoSprint",
            "Spider", "Speed", "NoRotate",
            "Avoid", "RidingHelper", "SafeWalk",
            "HighJump", "FastStairs", "NoFall",
            "ElytraFly", "Flight", "LiquidControl",
            "Blink", "ClickFly", "ClickTP",
            "PacketFly", "Phase", "TickShift",
            "Timer", "TridentBoost"
        );

        register(Category.MISC,
            "AntiAfk", "AutoEat", "PacketHelper",
            "AutoLog", "AntiAttack", "LagNotify",
            "PopCounter", "WaxAura", "AutoReconnect",
            "FastItem", "BlockMixer", "AutoSoup",
            "NameProtect", "StashFinder", "AutoAuth",
            "AntiQuit", "SoundBlock", "ChatHelper",
            "BookHelper", "Religion", "PauseBaritone",
            "AutoPortal", "FakePearl", "NewChunks",
            "PortalGod", "PortalGui", "PingSpoof",
            "RideExploit", "Xray"
        );

        register(Category.WORLD,
            "PVEUtils", "Scaffold", "AutoSign",
            "AutoShear", "AutoNameTag", "AutoMount",
            "FakePlayer", "ChestAura", "Igniter",
            "TreeCutter", "AutoReplant", "nuker.Nuker",
            "AutoTrade", "AutoFish", "AutoTunnel",
            "ECFarmer", "GhostBlocks", "AutoWither"
        );

        register(Category.CLIENT,
            "RichPresence", "GuiParticles", "FastLatency",
            "Fonts", "Notifications", "Hud",
            "DesktopGui", "Settings", "Calculator",
            "ClickGui", "BaritoneModule", "Commands"
        );

        register(Category.HUD,
            "WatermarkHud", "ArrayListHud", "CoordsHud",
            "FpsHud", "NowPlayingHud", "ChatHud",
            "TpsHud", "CooldownsHud", "InvPreviewHud",
            "IndicatorsHud", "CurrencyHud", "ServerBrandHud"
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

    public void init() {
        for (Module m : modules) {
            EventBusHolder.get().subscribe(m);
        }
    }

    public List<Module> getModules() { return modules; }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> clazz) {
        return (T) INSTANCE.byClass.get(clazz);
    }

    public void onTick() {
        for (Module m : modules) {
            if (!m.isHud() && m.getEnabled()) m.onTick();
        }
    }

    private void register(Category category, String... classNames) {
        String basePkg = "ravex.modules." + category.name().toLowerCase();
        for (String name : classNames) {
            try {
                String fullName = SUBPACKAGES.containsKey(name)
                    ? "ravex.modules." + SUBPACKAGES.get(name) + "." + name
                    : basePkg + "." + name;
                Class<?> clazz = Class.forName(fullName);
                var ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                Module module = (Module) ctor.newInstance();
                module.setCategory(category);
                modules.add(module);
                byClass.put(clazz, module);
            } catch (Exception e) {
                throw new RuntimeException("Failed to register " + name + " (" + category + ")", e);
            }
        }
    }
}
