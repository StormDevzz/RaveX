package ravex.gui.descriptions;

import java.util.HashMap;
import java.util.Map;

public class ClickGuiDescriptions {
    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();

    static {
<<<<<<< HEAD
        DESCRIPTIONS.put("KillAura", "Fucks all retardz");
        DESCRIPTIONS.put("ESP", "See through walls, holes, tunnels & void");
        DESCRIPTIONS.put("AutoTool", "Swap best tool");
        DESCRIPTIONS.put("AntiAfk", "No idle kick");
        DESCRIPTIONS.put("PVEUtils", "Smelt, tame, brew, fertilize & light");
        DESCRIPTIONS.put("ClickGui", "Open menu");
        DESCRIPTIONS.put("Notifications", "Screen alerts");

        DESCRIPTIONS.put("NoBob", "No head bob");
        DESCRIPTIONS.put("Ambient", "Change light");
        DESCRIPTIONS.put("Weather", "Change weather");
        DESCRIPTIONS.put("Fog", "Change fog");
=======
        DESCRIPTIONS.put("KillAura", "Attack enemies");
        DESCRIPTIONS.put("ESP", "See through walls");
        DESCRIPTIONS.put("AutoTool", "Swap best tool");
        DESCRIPTIONS.put("AntiAfk", "No idle kick");
        DESCRIPTIONS.put("BoneMeal", "Fertilize crops");
        DESCRIPTIONS.put("ClickGui", "Open menu");
        DESCRIPTIONS.put("Notifications", "Screen alerts");
        DESCRIPTIONS.put("VisualRange", "Player alerts");
        DESCRIPTIONS.put("NoBob", "No head bob");
        DESCRIPTIONS.put("Ambient", "Change light");
        DESCRIPTIONS.put("Weather", "Change weather");
        DESCRIPTIONS.put("CustomFog", "Change fog");
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        DESCRIPTIONS.put("SkyColor", "Change sky");
        DESCRIPTIONS.put("CloudColor", "Change clouds");
        DESCRIPTIONS.put("Trails", "Show paths");
        DESCRIPTIONS.put("AspectRatio", "Stretch screen");
        DESCRIPTIONS.put("Waypoint", "Save locations");
        DESCRIPTIONS.put("AimAssist", "Aim helper");
        DESCRIPTIONS.put("NameTags", "Show names");
        DESCRIPTIONS.put("Tracers", "Draw lines");
        DESCRIPTIONS.put("Trigger", "Attack on hover");
        DESCRIPTIONS.put("MaceSwap", "Swap to mace");
        DESCRIPTIONS.put("Hud", "Screen overlay");
        DESCRIPTIONS.put("NoWeb", "Walk through webs");
        DESCRIPTIONS.put("Glint", "Item glow");
        DESCRIPTIONS.put("RichPresence", "Discord status");
<<<<<<< HEAD
        DESCRIPTIONS.put("GuiMove", "Walk in menus");
        DESCRIPTIONS.put("NoSlow", "No slow down");
=======
        DESCRIPTIONS.put("GuiWalk", "Walk in menus");
        DESCRIPTIONS.put("NoSlowDown", "No slow down");
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        DESCRIPTIONS.put("Velocity", "Change knockback");
        DESCRIPTIONS.put("AutoEat", "Eat food");
        DESCRIPTIONS.put("NoInteract", "No misclicks");
        DESCRIPTIONS.put("SourceFiller", "Remove water");
        DESCRIPTIONS.put("AirPlace", "Place in air");
        DESCRIPTIONS.put("Scaffold", "Place bridges");
        DESCRIPTIONS.put("Shaders", "Neon glow");
        DESCRIPTIONS.put("FreeLook", "Free look");
        DESCRIPTIONS.put("FreeCam", "Free camera");
        DESCRIPTIONS.put("ViewClip", "See through blocks");
        DESCRIPTIONS.put("Step", "Step up");
        DESCRIPTIONS.put("ReverseStep", "Pull down");
<<<<<<< HEAD
        DESCRIPTIONS.put("ChatHelper", "Chat helpers");
=======
        DESCRIPTIONS.put("Spammer", "Spam chat");
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        DESCRIPTIONS.put("Commands", "Run commands");
        DESCRIPTIONS.put("ItemPhysics", "Item physics");
        DESCRIPTIONS.put("Sounds", "Play sounds");
        DESCRIPTIONS.put("AntiHunger", "No hunger");
        DESCRIPTIONS.put("AutoSign", "Fill signs");
        DESCRIPTIONS.put("AutoShear", "Shear sheep");
        DESCRIPTIONS.put("AutoNameTag", "Name animals");
        DESCRIPTIONS.put("AutoMount", "Ride animals");
        DESCRIPTIONS.put("RideExploit", "Ride anything");
        DESCRIPTIONS.put("AntiVoid", "Void save");
        DESCRIPTIONS.put("AutoMend", "Repair gear");
        DESCRIPTIONS.put("FakePlayer", "Practice dummy");
<<<<<<< HEAD
        DESCRIPTIONS.put("NoDelay", "No delay");
        DESCRIPTIONS.put("AutoRespawn", "Instant respawn");
        DESCRIPTIONS.put("AutoArmor", "Equip armor");
        DESCRIPTIONS.put("FastItem", "Quick item move");
        DESCRIPTIONS.put("Handshake", "Fake handshake");
=======
        DESCRIPTIONS.put("FastUse", "Fast use");
        DESCRIPTIONS.put("AutoRespawn", "Instant respawn");
        DESCRIPTIONS.put("AutoArmor", "Equip armor");
        DESCRIPTIONS.put("FastItem", "Quick item move");
        DESCRIPTIONS.put("HandshakeSpoof", "Fake handshake");
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        DESCRIPTIONS.put("LongJump", "Jump far");
        DESCRIPTIONS.put("AutoLog", "Leave fight");
        DESCRIPTIONS.put("WebAura", "Place webs");
        DESCRIPTIONS.put("AutoWeapon", "Best weapon");
        DESCRIPTIONS.put("MaceAura", "Mace smash");
        DESCRIPTIONS.put("AutoTotem", "Swap totems");
        DESCRIPTIONS.put("Hitboxes", "Bigger targets");
        DESCRIPTIONS.put("Sleepy", "Slide around");
        DESCRIPTIONS.put("Reach", "Longer reach");
        DESCRIPTIONS.put("NoPush", "No push");
        DESCRIPTIONS.put("FastBreak", "Break fast");
        DESCRIPTIONS.put("InstaBreak", "Instant break");
        DESCRIPTIONS.put("GhostHand", "Open through walls");
        DESCRIPTIONS.put("AutoSprint", "Always sprint");
        DESCRIPTIONS.put("Spider", "Climb walls");
        DESCRIPTIONS.put("Timer", "Speed up");
        DESCRIPTIONS.put("Speed", "Move fast");
        DESCRIPTIONS.put("Xray", "See ores");
        DESCRIPTIONS.put("Surround", "Protect feet");
        DESCRIPTIONS.put("BlockOutline", "Block highlight");
        DESCRIPTIONS.put("Fullbright", "Always bright");
        DESCRIPTIONS.put("GuiParticles", "Menu sparkles");
        DESCRIPTIONS.put("NoRotate", "No head turn");
        DESCRIPTIONS.put("NoRender", "Hide things");
        DESCRIPTIONS.put("AutoWalk", "Walk forward");
        DESCRIPTIONS.put("MobOwner", "Show owner");
        DESCRIPTIONS.put("SelfTrap", "Trap yourself");
        DESCRIPTIONS.put("WaxAura", "Wax copper");
        DESCRIPTIONS.put("ChestAura", "Place chests");
        DESCRIPTIONS.put("PortalGui", "Use in portals");
        DESCRIPTIONS.put("Avoid", "Avoid damage");
<<<<<<< HEAD
        DESCRIPTIONS.put("TabHelper", "Better list");
        DESCRIPTIONS.put("ChestHelper", "Chest helpers");
        DESCRIPTIONS.put("AutoReconnect", "Rejoin server");
        DESCRIPTIONS.put("FastLatency", "Fast ping");
        DESCRIPTIONS.put("MultiTask", "Do two things");
        DESCRIPTIONS.put("BlockMixer", "Shuffle blocks");
=======
        DESCRIPTIONS.put("TabUtils", "Better list");
        DESCRIPTIONS.put("ChatUtils", "Chat helpers");
        DESCRIPTIONS.put("ChestUtils", "Chest helpers");
        DESCRIPTIONS.put("AutoReconnect", "Rejoin server");
        DESCRIPTIONS.put("FastLatency", "Fast ping");
        DESCRIPTIONS.put("MultiTask", "Do two things");
        DESCRIPTIONS.put("BlockSelector", "Pick blocks");
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        DESCRIPTIONS.put("AutoSoup", "Drink soup");
        DESCRIPTIONS.put("NameProtect", "Hide names");
        DESCRIPTIONS.put("StashFinder", "Find loot");
        DESCRIPTIONS.put("ClickTP", "Click teleport");
        DESCRIPTIONS.put("ClickFly", "Click to fly");
        DESCRIPTIONS.put("ChorusExploit", "Save teleport");
        DESCRIPTIONS.put("TickShift", "Store bursts");
        DESCRIPTIONS.put("TridentBoost", "Trident boost");
        DESCRIPTIONS.put("WindAura", "Throw wind");
        DESCRIPTIONS.put("BreadCrumbs", "Show trail");
        DESCRIPTIONS.put("ViewModel", "Move hands");
<<<<<<< HEAD
        DESCRIPTIONS.put("AutoCrystal", "Blow all retardz");
        DESCRIPTIONS.put("Trap", "Trap targets");
        DESCRIPTIONS.put("Igniter", "Light TNT");
        DESCRIPTIONS.put("RidingHelper", "Faster riding");
=======
        DESCRIPTIONS.put("AutoCrystal", "Break crystals");
        DESCRIPTIONS.put("Trap", "Trap targets");
        DESCRIPTIONS.put("Igniter", "Light TNT");
        DESCRIPTIONS.put("RocketUtils", "Rocket boost");
        DESCRIPTIONS.put("RidingUtils", "Faster riding");
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        DESCRIPTIONS.put("Blink", "Delay moves");
        DESCRIPTIONS.put("PacketFly", "Fly freely");
        DESCRIPTIONS.put("AutoApple", "Eat apples");
        DESCRIPTIONS.put("TreeCutter", "Cut trees");
        DESCRIPTIONS.put("FakePearl", "Fake pearl");
        DESCRIPTIONS.put("BasePlace", "Place obsidian");
        DESCRIPTIONS.put("AnchorAura", "Explode anchors");
<<<<<<< HEAD
        DESCRIPTIONS.put("ElytraHelper", "Elytra swap");
=======
        DESCRIPTIONS.put("ElytraUtils", "Elytra swap");
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        DESCRIPTIONS.put("ViewLock", "Lock view");
        DESCRIPTIONS.put("ItemSaver", "Save items");
        DESCRIPTIONS.put("AntiAim", "Spin head");
        DESCRIPTIONS.put("NoSwing", "No swing");
        DESCRIPTIONS.put("Animations", "Smooth moves");
        DESCRIPTIONS.put("Swing", "Change swing");
        DESCRIPTIONS.put("DesktopGui", "Desktop control");
        DESCRIPTIONS.put("Breaker", "Break nearby");
        DESCRIPTIONS.put("BowAim", "Aim bow");
        DESCRIPTIONS.put("Quiver", "Shoot arrows");
        DESCRIPTIONS.put("DurabAlert", "Low durability");
        DESCRIPTIONS.put("AntiAttack", "No friendly hits");
        DESCRIPTIONS.put("LagNotify", "Lag warning");
        DESCRIPTIONS.put("PopCounter", "Count totems");
<<<<<<< HEAD
        DESCRIPTIONS.put("BookHelper", "Edit books");
=======
        DESCRIPTIONS.put("BookUtils", "Edit books");
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        DESCRIPTIONS.put("WebSelf", "Web yourself");
        DESCRIPTIONS.put("KeyPearl", "Throw pearl");
        DESCRIPTIONS.put("KeepSprint", "Keep sprint");
        DESCRIPTIONS.put("AutoAuth", "Login helper");
        DESCRIPTIONS.put("SafeWalk", "Safe walk");
<<<<<<< HEAD
        

        DESCRIPTIONS.put("ShieldFucker", "Break shields");
        DESCRIPTIONS.put("KillEffects", "Death effects");
        DESCRIPTIONS.put("AntiQuit", "No leaving");
        DESCRIPTIONS.put("DeathText", "Death message");
        DESCRIPTIONS.put("SoundBlock", "Mute sounds");
        DESCRIPTIONS.put("CoordLogger", "Log position");
        DESCRIPTIONS.put("Religion", "Show religions");
        DESCRIPTIONS.put("Phase", "Walk through walls");
        DESCRIPTIONS.put("MineAnimation", "Hide cracks");
=======
        DESCRIPTIONS.put("AutoTame", "Tame pets");
        DESCRIPTIONS.put("ShieldFucker", "Break shields");
        DESCRIPTIONS.put("KillEffects", "Death effects");
        DESCRIPTIONS.put("AntiQuit", "No leaving");
        DESCRIPTIONS.put("CustomDeathText", "Death message");
        DESCRIPTIONS.put("SoundBlocker", "Mute sounds");
        DESCRIPTIONS.put("CoordLogger", "Log position");
        DESCRIPTIONS.put("Religion", "Show religions");
        DESCRIPTIONS.put("Phase", "Walk through walls");
        DESCRIPTIONS.put("NoMineAnimation", "Hide cracks");
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        DESCRIPTIONS.put("HighJump", "High jump");
        DESCRIPTIONS.put("FastStairs", "Climb fast");
        DESCRIPTIONS.put("NoFall", "Safe fall");
        DESCRIPTIONS.put("Elytra++", "Elytra fly");
        DESCRIPTIONS.put("Flight", "Creative fly");
<<<<<<< HEAD
        DESCRIPTIONS.put("InvClean", "Clean inventory");
        DESCRIPTIONS.put("Replenish", "Refill items");
        

=======
        DESCRIPTIONS.put("InventoryCleaner", "Clean inventory");
        DESCRIPTIONS.put("Replenish", "Refill items");
        DESCRIPTIONS.put("AutoLight", "Place torches");
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        DESCRIPTIONS.put("AutoReplant", "Replant crops");
        DESCRIPTIONS.put("Nuker", "Mass break");
        DESCRIPTIONS.put("AutoTrade", "Trade villagers");
        DESCRIPTIONS.put("AutoFish", "Catch fish");
        DESCRIPTIONS.put("AutoTunnel", "Dig tunnel");
<<<<<<< HEAD
        

        DESCRIPTIONS.put("ECFarmer", "Farm chests");
        DESCRIPTIONS.put("GhostBlocks", "No fake blocks");

        DESCRIPTIONS.put("AntiPearl", "Pearl warning");
        DESCRIPTIONS.put("BedBomb", "Blow beds");
        DESCRIPTIONS.put("AutoPortal", "Build portals");
=======
        DESCRIPTIONS.put("AutoSmelt", "Smelt items");
        DESCRIPTIONS.put("AutoBrew", "Brew potions");
        DESCRIPTIONS.put("ECFarmer", "Farm chests");
        DESCRIPTIONS.put("NoGhostBlocks", "No fake blocks");
        DESCRIPTIONS.put("NoHitDelay", "No cooldown");
        DESCRIPTIONS.put("AntiPearl", "Pearl warning");
        DESCRIPTIONS.put("BedBomb", "Blow beds");
        DESCRIPTIONS.put("PortalBuild", "Build portals");
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        DESCRIPTIONS.put("Criticals", "Crit hits");
        DESCRIPTIONS.put("AutoBow", "Release bow");
        DESCRIPTIONS.put("HoleFill", "Fill holes");
        DESCRIPTIONS.put("AutoClicker", "Click fast");
        DESCRIPTIONS.put("AntiBot", "Block bots");
        DESCRIPTIONS.put("AutoDrop", "Drop blocks");
<<<<<<< HEAD
        DESCRIPTIONS.put("Burrow", "Hide in block");
        DESCRIPTIONS.put("PacketMine", "Click mine");
        DESCRIPTIONS.put("PacketHelper", "Network tools");
        DESCRIPTIONS.put("AutoWither", "Build wither");
        DESCRIPTIONS.put("Borders", "Show borders");
        DESCRIPTIONS.put("Zoom", "Camera zoom");
        DESCRIPTIONS.put("Blur", "Blur background");
        DESCRIPTIONS.put("ToolTips", "Item info");
=======
        DESCRIPTIONS.put("SelfFill", "Fill around");
        DESCRIPTIONS.put("Burrow", "Hide in block");
        DESCRIPTIONS.put("PacketMine", "Click mine");
        DESCRIPTIONS.put("PacketUtils", "Network tools");
        DESCRIPTIONS.put("WitherBuild", "Build wither");
        DESCRIPTIONS.put("Borders", "Show borders");
        DESCRIPTIONS.put("Zoom", "Camera zoom");
        DESCRIPTIONS.put("HoleESP", "Show holes");
        DESCRIPTIONS.put("Blur", "Blur background");
        DESCRIPTIONS.put("ToolTips", "Item info");
        DESCRIPTIONS.put("VoidESP", "Show void");
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        DESCRIPTIONS.put("AutoReGear", "Refill items");
        DESCRIPTIONS.put("AntiReGear", "Break containers");
        DESCRIPTIONS.put("Skeleton", "Show bodies");
        DESCRIPTIONS.put("ChatHud", "Chat overlay");
        DESCRIPTIONS.put("CooldownsHud", "Show cooldowns");
        DESCRIPTIONS.put("IndicatorsHud", "Target info");
        DESCRIPTIONS.put("InvPreviewHud", "Preview items");
        DESCRIPTIONS.put("NowPlayingHud", "Music info");
        DESCRIPTIONS.put("Settings", "Client settings");
        DESCRIPTIONS.put("TpsHud", "Server speed");
        DESCRIPTIONS.put("Calculator", "Do math");
        DESCRIPTIONS.put("Fonts", "Change fonts");
        DESCRIPTIONS.put("PingSpoof", "Fake ping");
        DESCRIPTIONS.put("ShiftInterp", "Force crouch");
<<<<<<< HEAD
        DESCRIPTIONS.put("SmallUser", "Small players");
        DESCRIPTIONS.put("LiquidControl", "Swim freely");
        DESCRIPTIONS.put("TntAura", "Bomb targets");

=======
        DESCRIPTIONS.put("BabyDude", "Small players");
        DESCRIPTIONS.put("LiquidCollision", "Swim freely");
        DESCRIPTIONS.put("TntAura", "Bomb targets");
        DESCRIPTIONS.put("WitherRoseAura", "Place roses");
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        DESCRIPTIONS.put("GridBuilder", "Build grids");
        DESCRIPTIONS.put("Particles", "Pretty particles");
        DESCRIPTIONS.put("Crosshair", "Custom crosshair");
        DESCRIPTIONS.put("PearlTarget", "Track pearls");
        DESCRIPTIONS.put("MiddleClick", "Middle click");
        DESCRIPTIONS.put("Telemetry", "Send data");
<<<<<<< HEAD
        DESCRIPTIONS.put("PortalGod", "Portal shield");
=======
        DESCRIPTIONS.put("PortalGodMode", "Portal shield");
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        DESCRIPTIONS.put("Baritone", "Find path");
        DESCRIPTIONS.put("PauseBaritone", "Pause path");
        DESCRIPTIONS.put("NewChunks", "New chunks");
        DESCRIPTIONS.put("AutoCart", "Send carts");
    }

    public static String getDescription(String moduleName) {
        return DESCRIPTIONS.getOrDefault(moduleName, "No description.");
    }
}
