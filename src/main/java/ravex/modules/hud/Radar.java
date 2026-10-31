package ravex.modules.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import ravex.gui.hudeditor.HudEditorScreen;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Module;
import ravex.modules.Modules;
import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import ravex.modules.client.Hud;
import ravex.utility.render.Render2DUtility;

// minimap, arrow is you, dots rotate with your view
@HudModule("Radar")
public class Radar extends Module {
    @Parameter(name = "Range", min = 8, max = 32, step = 2)
    public double range = 32;
    @Parameter(name = "Players")
    public boolean showPlayers = true;
    @Parameter(name = "Monsters")
    public boolean showMonsters = true;
    @Parameter(name = "Animals")
    public boolean showAnimals = true;

    public Radar() {
        super("Radar", 10, 10, 92, 92);
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        var level = mc.getLevel();
        boolean editor = mc.getScreen() instanceof HudEditorScreen;
        if ((p == null || level == null) && !editor) return;

        int size = 92;
        int x = (int) getX();
        int y = (int) getY();
        setWidth(size);
        setHeight(size);
        float cx = x + size / 2f;
        float cy = y + size / 2f;
        float half = size / 2f - 5f;

        Render2DUtility.drawRound(graphics, x, y, size, size, size / 2, 0xC00D1117);
        Render2DUtility.drawRound(graphics, x + 3, y + 3, size - 6, size - 6, (size - 6) / 2, 0x30101820);
        Render2DUtility.drawRoundBorder(graphics, x + 1, y + 1, size - 2, size - 2, (size - 2) / 2, 1, 0x50FFFFFF);
        int tickC = 0x40FFFFFF;
        int cxi = Math.round(cx);
        int cyi = Math.round(cy);
        Render2DUtility.drawRect(graphics, cxi - 1, y + 5, 2, 5, tickC);
        Render2DUtility.drawRect(graphics, cxi - 1, y + size - 10, 2, 5, tickC);
        Render2DUtility.drawRect(graphics, x + 5, cyi - 1, 5, 2, tickC);
        Render2DUtility.drawRect(graphics, x + size - 10, cyi - 1, 5, 2, tickC);

        if (p == null || level == null) return;

        float yaw = (float) Math.toRadians(p.getYRot());
        float sin = (float) Math.sin(yaw);
        float cos = (float) Math.cos(yaw);
        double scale = half / Math.max(4.0, range);

        for (Entity e : level.entitiesForRendering()) {
            if (e == p || !e.isAlive()) continue;
            boolean isPlayer = e instanceof Player;
            boolean isMonster = !isPlayer && e instanceof Enemy;
            boolean isAnimal = !isPlayer && !isMonster && e instanceof Animal;
            if (isPlayer && !showPlayers) continue;
            if (isMonster && !showMonsters) continue;
            if (isAnimal && !showAnimals) continue;
            if (!isPlayer && !isMonster && !isAnimal) continue;

            double dx = e.getX() - p.getX();
            double dz = e.getZ() - p.getZ();
            double maxR = Math.max(4.0, range);
            // skip far entities, blips would pile up on the rim
            if (dx * dx + dz * dz > maxR * maxR) continue;
            // rotate so forward is up, flip keeps right on the right
            float rx = (float) -(dx * cos + dz * sin);
            float rz = (float) (-dx * sin + dz * cos);
            float px2 = cx + rx * (float) scale;
            float py2 = cy - rz * (float) scale;

            int color = isPlayer ? 0xFFFFFFFF : isMonster ? 0xFFFF5555 : 0xFF55DD66;
            float dot = isPlayer ? 6.4f : 5.2f;
            Render2DUtility.drawRadarBlip(graphics, px2, py2, dot, color);
        }

        Render2DUtility.drawRadarChevron(graphics, cx, cy, 16f, 0xFFFFFFFF);
    }
}
