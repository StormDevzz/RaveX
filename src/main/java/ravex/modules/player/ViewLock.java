package ravex.modules.player;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import net.minecraft.client.Minecraft;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import ravex.manager.LuaManager;
import java.io.*;
public class ViewLock extends Module {
    public static final ViewLock INSTANCE = new ViewLock();
    public final BooleanParameter lockYaw = new BooleanParameter("LockYaw", true);
    public final BooleanParameter lockPitch = new BooleanParameter("LockPitch", true);
    public final BooleanParameter useLua = new BooleanParameter("UseLua", true);
    private static final String LUA_SCRIPT =
        "-- ViewLock custom hook\n" +
        "function viewLockTurn(yRot, xRot)\n" +
        "    local lockYaw = true\n" +
        "    local lockPitch = true\n" +
        "    return lockYaw, lockPitch\n" +
        "end\n" +
        "client.print(\"ViewLock default Lua script initialized! (Modify in ravex/scripts/view_lock.lua)\")\n";

    private File getScriptFile() {
        return new File(Minecraft.getInstance().gameDirectory, "ravex/scripts/view_lock.lua");
    }

    @Override
    protected void onEnable() {
        if (!useLua.getValue()) return;
        try {
            LuaManager.INSTANCE.getGlobals().loadfile(ensureScriptFile().getAbsolutePath()).call();
        } catch (Exception e) {
            if (Minecraft.getInstance().player != null)
                Minecraft.getInstance().player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                        "§7[§5Lua Error§7] §cFailed to load view_lock.lua: " + e.getMessage()), false);
        }
    }
    private File ensureScriptFile() {
        File f = getScriptFile();
        f.getParentFile().mkdirs();
        if (!f.exists()) {
            try (FileWriter w = new FileWriter(f)) { w.write(LUA_SCRIPT); }
            catch (IOException e) { e.printStackTrace(); }
        }
        return f;
    }
    public boolean shouldLockYaw(double yRot, double xRot) {
        if (!getEnabled()) return false;
        if (useLua.getValue()) {
            Boolean r = callLua("viewLockTurn", yRot, xRot, 1);
            if (r != null) return r;
        }
        return lockYaw.getValue();
    }
    public boolean shouldLockPitch(double yRot, double xRot) {
        if (!getEnabled()) return false;
        if (useLua.getValue()) {
            Boolean r = callLua("viewLockTurn", yRot, xRot, 2);
            if (r != null) return r;
        }
        return lockPitch.getValue();
    }
    private Boolean callLua(String func, double yRot, double xRot, int argIndex) {
        LuaValue fn = LuaManager.INSTANCE.getGlobals().get(func);
        if (fn == null || !fn.isfunction()) return null;
        try {
            Varargs res = fn.invoke(LuaValue.varargsOf(new LuaValue[]{
                LuaValue.valueOf(yRot), LuaValue.valueOf(xRot)
            }));
            return res.arg(argIndex).optboolean(true);
        } catch (Exception e) { return null; }
    }
}
