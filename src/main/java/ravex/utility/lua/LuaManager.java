package ravex.utility.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ravex.modules.Module;
import ravex.modules.ModuleManager;

import java.io.*;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class LuaManager {
    public static final LuaManager INSTANCE = new LuaManager();

    private Globals globals;
    private final Map<String, LuaTimer> luaTimers = new ConcurrentHashMap<>();
    private final AtomicInteger tickCounter = new AtomicInteger(0);

    // Ключ для XOR расшифровки скриптов — тот же, что используется при сборке
    private static final String SCRIPT_KEY = "RaveX_ScriptKey_2025";
    private static boolean nativeAvailable = false;

    static {
        try {
            ravex.utility.misc.NativeLoader.load();
            nativeAvailable = true;
        } catch (Throwable ignored) {}
    }

    private SocketChannel discordChannel = null;
    private OutputStream  discordOut     = null;
    private long discordNonce = 1;

    private static final String DISCORD_CLIENT_ID = "1517835260799484034";

    private LuaManager() {
        initEngine();
    }

    public void initEngine() {
        globals = JsePlatform.standardGlobals();
        registerClientLib();
        registerPlayerLib();
        registerModulesLib();
        registerDiscordLib();
        registerTimerLib();

        try (InputStream in = LuaManager.class.getResourceAsStream("/lua/init.lua")) {
            if (in != null) {
                String code = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                globals.load(code, "init.lua").call();
                ravex.RaveX.LOGGER.info("[Lua] init.lua loaded successfully.");
            } else {
                ravex.RaveX.LOGGER.warn("[Lua] init.lua not found in resources.");
            }
        } catch (Exception e) {
            ravex.RaveX.LOGGER.error("[Lua] Failed to load init.lua: " + e.getMessage());
        }
    }

    public Globals getGlobals() {
        return globals;
    }

    private void registerClientLib() {
        LuaValue lib = LuaValue.tableOf();

        lib.set("print", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                        Component.literal("§7[§5Lua§7] §f" + arg.tojstring()), false);
                }
                return LuaValue.NIL;
            }
        });

        lib.set("version", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(ravex.RaveX.version);
            }
        });

        lib.set("getTime", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf((double) System.currentTimeMillis());
            }
        });

        lib.set("getEnabledCount", new ZeroArgFunction() {
            private int lastCount = -1;
            private long lastCheck = 0;

            @Override public LuaValue call() {
                long now = System.currentTimeMillis();
                if (now - lastCheck > 1000) {
                    int count = 0;
                    for (Module m : ModuleManager.INSTANCE.getModules()) {
                        if (m.getEnabled()) count++;
                    }
                    lastCount = count;
                    lastCheck = now;
                }
                return LuaValue.valueOf(lastCount);
            }
        });

        globals.set("client", lib);
    }

    private void registerPlayerLib() {
        LuaValue lib = LuaValue.tableOf();

        lib.set("isInGame", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null && mc.level != null);
            }
        });
        lib.set("getHealth", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return mc.player != null
                    ? LuaValue.valueOf((double) mc.player.getHealth())
                    : LuaValue.valueOf(0);
            }
        });
        lib.set("getMaxHealth", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return mc.player != null
                    ? LuaValue.valueOf((double) mc.player.getMaxHealth())
                    : LuaValue.valueOf(20);
            }
        });
        lib.set("getX", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return mc.player != null ? LuaValue.valueOf(mc.player.getX()) : LuaValue.valueOf(0);
            }
        });
        lib.set("getY", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return mc.player != null ? LuaValue.valueOf(mc.player.getY()) : LuaValue.valueOf(0);
            }
        });
        lib.set("getZ", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return mc.player != null ? LuaValue.valueOf(mc.player.getZ()) : LuaValue.valueOf(0);
            }
        });
        lib.set("getName", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return mc.player != null
                    ? LuaValue.valueOf(mc.player.getGameProfile().name())
                    : LuaValue.valueOf("Unknown");
            }
        });

        lib.set("isRightClicking", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.options.keyUse.isDown());
            }
        });

        lib.set("getLookingPos", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return LuaValue.NIL;
                double dist = arg.optdouble(4.5);
                net.minecraft.world.phys.HitResult hit = mc.player.pick(dist, 1.0F, false);
                if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    net.minecraft.core.BlockPos pos = ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos();
                    LuaValue tbl = LuaValue.tableOf();
                    tbl.set("x", pos.getX());
                    tbl.set("y", pos.getY());
                    tbl.set("z", pos.getZ());
                    return tbl;
                } else {
                    net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition(1.0F);
                    net.minecraft.world.phys.Vec3 look = mc.player.getViewVector(1.0F);
                    net.minecraft.world.phys.Vec3 target = eye.add(look.x * dist, look.y * dist, look.z * dist);
                    net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(target);
                    LuaValue tbl = LuaValue.tableOf();
                    tbl.set("x", pos.getX());
                    tbl.set("y", pos.getY());
                    tbl.set("z", pos.getZ());
                    return tbl;
                }
            }
        });

        lib.set("setHighlightPos", new org.luaj.vm2.lib.VarArgFunction() {
            @Override public org.luaj.vm2.Varargs invoke(org.luaj.vm2.Varargs args) {
                if (args.narg() < 3 || args.arg(1).isnil() || args.arg(2).isnil() || args.arg(3).isnil()) {
                    ravex.modules.player.AirPlace.highlightPos = null;
                } else {
                    ravex.modules.player.AirPlace.highlightPos = new net.minecraft.world.phys.Vec3(
                        args.arg(1).todouble(), args.arg(2).todouble(), args.arg(3).todouble()
                    );
                }
                return LuaValue.NIL;
            }
        });

        lib.set("placeBlock", new ThreeArgFunction() {
            @Override public LuaValue call(LuaValue xVal, LuaValue yVal, LuaValue zVal) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.level == null) return LuaValue.FALSE;
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(xVal.checkint(), yVal.checkint(), zVal.checkint());
                net.minecraft.world.phys.Vec3 hitVec = net.minecraft.world.phys.Vec3.atCenterOf(pos);
                net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(hitVec, net.minecraft.core.Direction.UP, pos, false);
                mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                return LuaValue.TRUE;
            }
        });

        lib.set("swingHand", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
                return LuaValue.NIL;
            }
        });

        lib.set("isHoldingBlock", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return LuaValue.FALSE;
                net.minecraft.world.item.ItemStack stack = mc.player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
                return LuaValue.valueOf(!stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BlockItem);
            }
        });

        lib.set("sendChat", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.player.connection != null) {
                    mc.player.connection.sendChat(arg.checkjstring());
                }
                return LuaValue.NIL;
            }
        });

        globals.set("player", lib);
    }

    private void registerModulesLib() {
        LuaValue lib = LuaValue.tableOf();

        lib.set("isEnabled", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                Module m = ModuleManager.INSTANCE.getByName(arg.tojstring());
                return LuaValue.valueOf(m != null && m.getEnabled());
            }
        });
        lib.set("toggle", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                Module m = ModuleManager.INSTANCE.getByName(arg.tojstring());
                if (m != null) { m.toggle(); return LuaValue.valueOf(m.getEnabled()); }
                return LuaValue.FALSE;
            }
        });
        lib.set("enable", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                Module m = ModuleManager.INSTANCE.getByName(arg.tojstring());
                if (m != null) m.setEnabled(true);
                return LuaValue.NIL;
            }
        });
        lib.set("disable", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                Module m = ModuleManager.INSTANCE.getByName(arg.tojstring());
                if (m != null) m.setEnabled(false);
                return LuaValue.NIL;
            }
        });
        lib.set("list", new ZeroArgFunction() {
            @Override public LuaValue call() {
                LuaValue tbl = LuaValue.tableOf();
                int i = 1;
                for (Module m : ModuleManager.INSTANCE.getModules()) {
                    tbl.set(i++, LuaValue.valueOf(m.getName()));
                }
                return tbl;
            }
        });
        lib.set("enabledCount", new ZeroArgFunction() {
            @Override public LuaValue call() {
                int count = 0;
                for (Module m : ModuleManager.INSTANCE.getModules()) {
                    if (m.getEnabled()) count++;
                }
                return LuaValue.valueOf(count);
            }
        });

        globals.set("modules", lib);
    }

    private void registerDiscordLib() {
        LuaValue lib = LuaValue.tableOf();
        LuaManager self = this;

        lib.set("connect", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(self.discordConnect());
            }
        });

        lib.set("setActivity", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                String details  = args.optjstring(1, "Playing Minecraft");
                String state    = args.optjstring(2, "");
                long   startMs  = args.optlong(3, 0L);
                return LuaValue.valueOf(self.discordSetActivity(details, state, startMs));
            }
        });

        lib.set("clearActivity", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(self.discordClearActivity());
            }
        });

        lib.set("disconnect", new ZeroArgFunction() {
            @Override public LuaValue call() {
                self.discordDisconnect();
                return LuaValue.NIL;
            }
        });

        lib.set("isConnected", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(self.discordChannel != null
                        && self.discordChannel.isOpen());
            }
        });

        globals.set("discord", lib);
    }

    private void registerTimerLib() {
        LuaValue lib = LuaValue.tableOf();
        LuaManager self = this;

        lib.set("setInterval", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                String     id       = args.checkjstring(1);
                long       interval = args.checklong(2);
                LuaFunction fn      = (LuaFunction) args.checkfunction(3);
                self.luaTimers.put(id, new LuaTimer(interval, fn));
                return LuaValue.NIL;
            }
        });

        lib.set("clearInterval", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                self.luaTimers.remove(arg.tojstring());
                return LuaValue.NIL;
            }
        });

        globals.set("timer", lib);
    }

    public boolean discordConnect() {
        for (int i = 0; i <= 9; i++) {
            for (String path : getIpcPaths(i)) {
                try {
                    var addr = UnixDomainSocketAddress.of(path);
                    discordChannel = SocketChannel.open(java.net.StandardProtocolFamily.UNIX);
                    discordChannel.connect(addr);
                    discordOut = java.nio.channels.Channels.newOutputStream(discordChannel);

                    String hs = "{\"v\":1,\"client_id\":\"" + DISCORD_CLIENT_ID + "\"}";
                    sendDiscordFrame(0, hs);
                    java.nio.channels.Channels.newInputStream(discordChannel).readNBytes(64);

                    ravex.RaveX.LOGGER.info("[RichPresence] Discord IPC connected via " + path);
                    return true;
                } catch (Exception e) {
                    discordDisconnect();
                }
            }
        }
        ravex.RaveX.LOGGER.warn("[RichPresence] Could not connect to Discord IPC.");
        return false;
    }

    public boolean discordSetActivity(String details, String state, long startMs) {
        if (discordChannel == null || !discordChannel.isOpen()) return false;
        try {
            String nonce = String.valueOf(discordNonce++);
            String startBlock = (startMs > 0)
                ? ",\"timestamps\":{\"start\":" + (startMs / 1000L) + "}"
                : "";
            String payload = "{"
                + "\"cmd\":\"SET_ACTIVITY\","
                + "\"args\":{"
                + "\"pid\":" + ProcessHandle.current().pid() + ","
                + "\"activity\":{"
                + "\"details\":\"" + escJson(details) + "\","
                + "\"state\":\"" + escJson(state) + "\""
                + startBlock
                + ",\"assets\":{\"large_image\":\"icon\",\"large_text\":\"RaveX\"}"
                + "}},"
                + "\"nonce\":\"" + nonce + "\"}";
            sendDiscordFrame(1, payload);
            return true;
        } catch (Exception e) {
            discordDisconnect();
            return false;
        }
    }

    public boolean discordClearActivity() {
        if (discordChannel == null || !discordChannel.isOpen()) return false;
        try {
            String nonce = String.valueOf(discordNonce++);
            String payload = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":"
                + ProcessHandle.current().pid() + "},\"nonce\":\"" + nonce + "\"}";
            sendDiscordFrame(1, payload);
            return true;
        } catch (Exception e) {
            discordDisconnect();
            return false;
        }
    }

    public void discordDisconnect() {
        try {
            if (discordChannel != null) discordChannel.close();
        } catch (Exception ignored) {}
        discordChannel = null;
        discordOut     = null;
    }

    private synchronized void sendDiscordFrame(int opcode, String json) throws Exception {
        if (discordOut == null) return;
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(opcode);
        buf.putInt(data.length);
        buf.put(data);
        discordOut.write(buf.array());
        discordOut.flush();
    }

    private static List<String> getIpcPaths(int i) {
        List<String> paths = new ArrayList<>();
        String xdg = System.getenv("XDG_RUNTIME_DIR");
        if (xdg != null && !xdg.isEmpty()) {
            paths.add(xdg + "/discord-ipc-" + i);
            paths.add(xdg + "/app/com.discordapp.Discord/discord-ipc-" + i);
            paths.add(xdg + "/.flatpak/com.discordapp.Discord/xdg-run/discord-ipc-" + i);
            paths.add(xdg + "/snap.discord/discord-ipc-" + i);
        }
        String[] envVars = { "TMPDIR", "TMP", "TEMP" };
        for (String v : envVars) {
            String dir = System.getenv(v);
            if (dir != null && !dir.isEmpty()) {
                paths.add(dir + "/discord-ipc-" + i);
            }
        }
        paths.add("/tmp/discord-ipc-" + i);
        return paths;
    }

    private static String escJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public void onDisableRichPresence() {
        luaTimers.remove("rich_presence");
        discordClearActivity();
        discordDisconnect();
    }

    public void onTick() {
        if (luaTimers.isEmpty()) return;
        int tick = tickCounter.incrementAndGet();

        long now = System.currentTimeMillis();
        for (LuaTimer t : luaTimers.values()) {
            if (tick % t.getThrottle() == 0) {
                t.tick(now);
            }
        }

        if (tickCounter.get() > 1000000) {
            tickCounter.set(0);
        }
    }

    /**
     * Загружает зашифрованные скрипты из ресурсов JAR (ravex/scripts/*.dat).
     * Расшифровка происходит через C++ JNI — исключительно в оперативной памяти.
     * На диск расшифрованный код НИКОГДА не записывается.
     */
    public void loadEncryptedScripts() {
        String[] scriptNames = {
            "init",
            "rich_presence",
            "lib_discord",
            "lib_player",
            "examples_hello",
            "examples_module_info",
            "spammer"
        };
        for (String name : scriptNames) {
            String resourcePath = "/lua/ravex/scripts/" + name + ".dat";
            try (InputStream in = LuaManager.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    ravex.RaveX.LOGGER.warn("[Lua] Encrypted script not found: " + resourcePath);
                    continue;
                }
                byte[] encryptedBytes = in.readAllBytes();
                String luaSource;
                if (nativeAvailable) {
                    luaSource = nativeDecryptScript(encryptedBytes, SCRIPT_KEY);
                } else {
                    byte[] key = SCRIPT_KEY.getBytes(StandardCharsets.UTF_8);
                    for (int i = 0; i < encryptedBytes.length; i++) {
                        encryptedBytes[i] ^= key[i % key.length];
                    }
                    luaSource = new String(encryptedBytes, StandardCharsets.UTF_8);
                }
                globals.load(luaSource, name + ".lua").call();
                ravex.RaveX.LOGGER.info("[Lua] Loaded encrypted script: " + name);
            } catch (Exception e) {
                ravex.RaveX.LOGGER.error("[Lua] Failed to load encrypted script '" + name + "': " + e.getMessage());
            }
        }
    }

    public void loadAndRunScripts() {
        Minecraft mc = Minecraft.getInstance();
        File scriptsFolder = new File(mc.gameDirectory, "ravex/scripts");
        if (!scriptsFolder.exists()) {
            scriptsFolder.mkdirs();
        }

        File[] files = scriptsFolder.listFiles((dir, name) -> name.endsWith(".lua"));
        if (files != null) {
            for (File f : files) {
                runScript(mc, f);
            }
        }
    }

    private void runScript(Minecraft mc, File f) {
        try {
            LuaValue chunk = globals.loadfile(f.getAbsolutePath());
            chunk.call();
        } catch (Exception e) {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("§7[§5Lua Error§7] §c" + f.getName() + ": " + e.getMessage()),
                    false);
            }
            ravex.RaveX.LOGGER.error("[Lua] Error in " + f.getName() + ": " + e.getMessage());
        }
    }

    // ── Native JNI binding ────────────────────────────────────────────────────
    private static native String nativeDecryptScript(byte[] encryptedData, String key);

    private static class LuaTimer {
        private final long     intervalMs;
        private final LuaFunction fn;
        private long lastFired = 0;
        private final int throttle;

        LuaTimer(long intervalMs, LuaFunction fn) {
            this.intervalMs = intervalMs;
            this.fn         = fn;
            this.lastFired  = System.currentTimeMillis();
            this.throttle   = Math.max(1, (int)(intervalMs / 50));
        }

        int getThrottle() { return throttle; }

        void tick(long now) {
            if (now - lastFired >= intervalMs) {
                lastFired = now;
                try { fn.call(); } catch (Exception ignored) {}
            }
        }
    }
}
