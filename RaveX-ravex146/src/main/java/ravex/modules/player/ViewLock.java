package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class ViewLock extends Module {
    public static final ViewLock INSTANCE = new ViewLock();

    public final BooleanParameter lockYaw = new BooleanParameter("Lock Yaw", true);
    public final BooleanParameter lockPitch = new BooleanParameter("Lock Pitch", true);
    public final BooleanParameter useLua = new BooleanParameter("Use Lua", true);

    private ViewLock() {
        super("ViewLock", Category.PLAYER);
        addParameter(lockYaw);
        addParameter(lockPitch);
        addParameter(useLua);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (useLua.getValue() && p != null) {
            java.io.File dir = new java.io.File(mc.gameDirectory, "ravex/scripts");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            java.io.File scriptFile = new java.io.File(dir, "view_lock.lua");
            if (!scriptFile.exists()) {
                try (java.io.FileWriter writer = new java.io.FileWriter(scriptFile)) {
                    writer.write("-- ViewLock custom hook\n" +
                                 "-- This function is called whenever the player moves their mouse\n" +
                                 "-- yRot: change in horizontal rotation (yaw)\n" +
                                 "-- xRot: change in vertical rotation (pitch)\n" +
                                 "-- Returns: lockYaw (boolean), lockPitch (boolean)\n" +
                                 "function viewLockTurn(yRot, xRot)\n" +
                                 "    -- Default behavior: lock both yaw and pitch entirely\n" +
                                 "    local lockYaw = true\n" +
                                 "    local lockPitch = true\n" +
                                 "    \n" +
                                 "    -- Example customization: only lock yaw if moving, or lock based on some condition\n" +
                                 "    -- if player.getHealth() < 10 then\n" +
                                 "    --     lockYaw = false\n" +
                                 "    -- end\n" +
                                 "    \n" +
                                 "    return lockYaw, lockPitch\n" +
                                 "end\n" +
                                 "\n" +
                                 "client.print(\"ViewLock default Lua script initialized! (Modify in ravex/scripts/view_lock.lua)\")\n");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            try {
                org.luaj.vm2.LuaValue chunk = ravex.utility.lua.LuaManager.INSTANCE.getGlobals().loadfile(scriptFile.getAbsolutePath());
                chunk.call();
            } catch (Exception e) {
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("§7[§5Lua Error§7] §cFailed to load view_lock.lua: " + e.getMessage()), false);
            }
        }
    }

    public boolean shouldLockYaw(double yRot, double xRot) {
        if (!getEnabled()) return false;
        if (useLua.getValue()) {
            org.luaj.vm2.LuaValue fn = ravex.utility.lua.LuaManager.INSTANCE.getGlobals().get("viewLockTurn");
            if (fn != null && fn.isfunction()) {
                try {
                    org.luaj.vm2.Varargs res = fn.invoke(org.luaj.vm2.LuaValue.varargsOf(new org.luaj.vm2.LuaValue[]{
                        org.luaj.vm2.LuaValue.valueOf(yRot),
                        org.luaj.vm2.LuaValue.valueOf(xRot)
                    }));
                    return res.arg(1).optboolean(true);
                } catch (Exception ignored) {}
            }
        }
        return lockYaw.getValue();
    }

    public boolean shouldLockPitch(double yRot, double xRot) {
        if (!getEnabled()) return false;
        if (useLua.getValue()) {
            org.luaj.vm2.LuaValue fn = ravex.utility.lua.LuaManager.INSTANCE.getGlobals().get("viewLockTurn");
            if (fn != null && fn.isfunction()) {
                try {
                    org.luaj.vm2.Varargs res = fn.invoke(org.luaj.vm2.LuaValue.varargsOf(new org.luaj.vm2.LuaValue[]{
                        org.luaj.vm2.LuaValue.valueOf(yRot),
                        org.luaj.vm2.LuaValue.valueOf(xRot)
                    }));
                    return res.arg(2).optboolean(true);
                } catch (Exception ignored) {}
            }
        }
        return lockPitch.getValue();
    }
}
