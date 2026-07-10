package ravex.render.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import ravex.manager.ModuleManager;
import ravex.manager.NotificationManager;
import ravex.manager.ShaderManager;
import ravex.modules.Module;
import ravex.modules.render.*;
import ravex.modules.combat.*;
import ravex.modules.player.*;
import ravex.modules.world.*;
import ravex.modules.client.Hud;
import ravex.utility.misc.GuiOptimizer;
import ravex.utility.misc.MobUtility;
import ravex.utility.render.FontRenderUtility;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public final class HudRenderer {
    public static final HudRenderer INSTANCE = new HudRenderer();

    private HudRenderer() {}

    public void render(GuiGraphics context, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();

        if (ModuleManager.get(Ambient.class).getEnabled()) {
            int rVal = ModuleManager.get(Ambient.class).r.getValue().intValue();
            int gVal = ModuleManager.get(Ambient.class).g.getValue().intValue();
            int bVal = ModuleManager.get(Ambient.class).b.getValue().intValue();
            int aVal = ModuleManager.get(Ambient.class).a.getValue().intValue();
            int color = ((aVal & 0xFF) << 24) | ((rVal & 0xFF) << 16) | ((gVal & 0xFF) << 8) | (bVal & 0xFF);
            context.fill(0, 0, context.guiWidth(), context.guiHeight(), color);
        }

        if (mc.level == null || mc.player == null) {
            renderHud(context, tickCounter);
            return;
        }

        float pt = tickCounter.getGameTimeDeltaTicks();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        boolean espEnabled = ModuleManager.get(ESP.class).getEnabled();
        boolean nameTagsEnabled = ModuleManager.get(NameTags.class).getEnabled();
        boolean mobOwnerEnabled = ModuleManager.get(MobOwner.class).getEnabled() && ModuleManager.get(MobOwner.class).animals.getValue();

        int guiWidth = context.guiWidth();
        int guiHeight = context.guiHeight();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        Vec3 playerViewVec = mc.player != null ? mc.player.getViewVector(pt) : null;
        Quaternionf cRotation = mc.gameRenderer.getMainCamera().rotation();
        Vector4f lookVec = new Vector4f(0.0F, 0.0F, -1.0F, 0.0F).rotate(cRotation);
        Vec3 cameraLook = new Vec3(lookVec.x(), lookVec.y(), lookVec.z());
        boolean tracersEnabled = ModuleManager.get(Tracers.class).getEnabled();

        List<Entity> candidates = buildCandidates(mc, espEnabled, nameTagsEnabled, mobOwnerEnabled, tracersEnabled, pt, firstPerson);

        renderTracers(context, mc, candidates, tracersEnabled, pt, cameraPos, cameraLook, guiWidth, guiHeight);

        renderNameTagsAndESP(context, mc, candidates, cameraPos, cameraLook, pt, guiWidth, guiHeight, espEnabled, nameTagsEnabled, mobOwnerEnabled, playerViewVec);

        renderDamageLabels(context, mc, pt, cameraPos, cameraLook, guiWidth, guiHeight);
        renderPacketMine(context, mc);
        renderWaypoints(context, mc);
        NotificationManager.render(context);

        if (ModuleManager.get(Crosshair.class).getEnabled()) {
            try { ModuleManager.get(Crosshair.class).render(context); } catch (Throwable ignored) {}
        }

        renderHud(context, tickCounter);
    }

    private List<Entity> buildCandidates(Minecraft mc, boolean espEnabled, boolean nameTagsEnabled, boolean mobOwnerEnabled, boolean tracersEnabled, float pt, boolean firstPerson) {
        List<Entity> candidates = new ArrayList<>();
        for (Entity target : mc.level.entitiesForRendering()) {
            if (target == mc.player) continue;
            if (target instanceof LivingEntity living && !living.isAlive()) continue;

            double dist;
            if (NameTags.isNativeAvailable()) {
                Vec3 pPos = mc.player.position();
                Vec3 tPos = target.position();
                dist = NameTags.nativeGetDistance(pPos.x, pPos.y, pPos.z, tPos.x, tPos.y, tPos.z);
            } else {
                dist = mc.player.distanceTo(target);
            }

            double maxDist = ModuleManager.get(ESP.class).maxDistance.getValue();
            if (nameTagsEnabled) maxDist = Math.max(maxDist, ModuleManager.get(NameTags.class).range.getValue());
            if (tracersEnabled) maxDist = Math.max(maxDist, ModuleManager.get(Tracers.class).maxDistance.getValue());
            if (dist > maxDist) continue;

            if (firstPerson && dist < 1.2 && !nameTagsEnabled) continue;

            boolean isPlayer = target instanceof Player;
            boolean isMonster = target instanceof LivingEntity le && MobUtility.isHostile(le);
            boolean isAnimal = target instanceof net.minecraft.world.entity.animal.Animal || target instanceof net.minecraft.world.entity.ambient.AmbientCreature;
            boolean isItem = target instanceof net.minecraft.world.entity.item.ItemEntity;
            boolean isFrame = target instanceof net.minecraft.world.entity.decoration.ItemFrame;

            if (isPlayer && !ModuleManager.get(ESP.class).players.getValue() && !(tracersEnabled && ModuleManager.get(Tracers.class).players.getValue())) continue;
            if (isMonster && !ModuleManager.get(ESP.class).monsters.getValue() && !(tracersEnabled && ModuleManager.get(Tracers.class).monsters.getValue())) continue;
            if (isAnimal && !ModuleManager.get(ESP.class).animals.getValue() && !(tracersEnabled && ModuleManager.get(Tracers.class).animals.getValue())) continue;
            if (isItem && !ModuleManager.get(ESP.class).items.getValue() && !(tracersEnabled && ModuleManager.get(Tracers.class).items.getValue())) continue;
            if (isFrame && !ModuleManager.get(ESP.class).frames.getValue()) continue;

            if (!isPlayer && !isMonster && !isAnimal && !isItem && !isFrame) continue;
            candidates.add(target);
        }
        return candidates;
    }

    private void renderTracers(GuiGraphics context, Minecraft mc, List<Entity> candidates, boolean tracersEnabled, float pt, Vec3 cameraPos, Vec3 cameraLook, int guiWidth, int guiHeight) {
        if (!tracersEnabled) return;
        Tracers tracers = ModuleManager.get(Tracers.class);

        List<Entity> tracerEntities = new ArrayList<>();
        List<Integer> tracerColors = new ArrayList<>();

        for (Entity target : candidates) {
            boolean isPlayer = target instanceof Player;
            boolean isMonster = target instanceof LivingEntity le && MobUtility.isHostile(le);
            boolean isAnimal = target instanceof net.minecraft.world.entity.animal.Animal || target instanceof net.minecraft.world.entity.ambient.AmbientCreature;
            boolean isItem = target instanceof net.minecraft.world.entity.item.ItemEntity;

            boolean show = false;
            int color = 0;
            if (isPlayer && tracers.players.getValue()) { show = true; color = tracers.playerColor.getValue(); }
            else if (isMonster && tracers.monsters.getValue()) { show = true; color = tracers.mobColor.getValue(); }
            else if (isAnimal && tracers.animals.getValue()) { show = true; color = tracers.animalColor.getValue(); }
            else if (isItem && tracers.items.getValue()) { show = true; color = tracers.itemColor.getValue(); }

            if (show) {
                tracerEntities.add(target);
                tracerColors.add(color);
            }
        }

        if ("Arrows".equals(tracers.mode.getValue())) {
            Tracers.renderArrows(context, tracerEntities, tracerColors, pt, cameraPos, cameraLook, guiWidth, guiHeight);
        } else {
            float width = tracers.lineWidth.getValue().floatValue();
            for (int i = 0; i < tracerEntities.size(); i++) {
                Entity target = tracerEntities.get(i);
                int color = tracerColors.get(i);
                Vec3 basePos = target.getPosition(pt);
                float bbHeight = target.getBbHeight();
                Vec3 headPos = basePos.add(0, bbHeight, 0);
                Vec3 baseProj = projectPointToScreenUnbobbed(basePos);
                Vec3 headProj = projectPointToScreenUnbobbed(headPos);
                if (baseProj != null && headProj != null) {
                    double cx = guiWidth / 2.0;
                    double cy = guiHeight / 2.0;
                    Vec3 toEntity = basePos.subtract(cameraPos);
                    boolean isBehind = cameraLook.dot(toEntity) < 0;
                    double ex = (baseProj.x + 1.0) / 2.0 * guiWidth;
                    double ey_base = (1.0 - baseProj.y) / 2.0 * guiHeight;
                    double ey_head = (1.0 - headProj.y) / 2.0 * guiHeight;
                    double ey = (ey_base + ey_head) / 2.0;
                    if (isBehind) { ex = guiWidth - ex; ey = guiHeight - ey; }
                    boolean isOffscreen = isBehind || ex < 0 || ex > guiWidth || ey < 0 || ey > guiHeight;
                    if (isOffscreen) {
                        double dx = ex - cx, dy = ey - cy;
                        double tX = Double.MAX_VALUE, tY = Double.MAX_VALUE;
                        double borderPadding = 2.0;
                        if (dx > 0) tX = (guiWidth - borderPadding - cx) / dx;
                        else if (dx < 0) tX = (borderPadding - cx) / dx;
                        if (dy > 0) tY = (guiHeight - borderPadding - cy) / dy;
                        else if (dy < 0) tY = (borderPadding - cy) / dy;
                        double t = Math.min(tX, tY);
                        if (t > 0 && t < 1.0) { ex = cx + t * dx; ey = cy + t * dy; }
                        else {
                            double len = Math.sqrt(dx * dx + dy * dy);
                            if (len > 0) { ex = cx + (dx / len) * (cx - borderPadding); ey = cy + (dy / len) * (cy - borderPadding); }
                        }
                    }
                    drawTracerLine2D(context, (float) cx, (float) cy, (float) ex, (float) ey, color, width);
                }
            }
        }
    }

    private void renderNameTagsAndESP(GuiGraphics context, Minecraft mc, List<Entity> candidates, Vec3 cameraPos, Vec3 cameraLook, float pt, int guiWidth, int guiHeight, boolean espEnabled, boolean nameTagsEnabled, boolean mobOwnerEnabled, Vec3 playerViewVec) {

        boolean nativeSuccess = false;
        int count = candidates.size();
        double[] outLayouts = null;
        int[] outIndices = null;
        int renderedCount = 0;

        if (count > 0 && ravex.utility.nativelib.NativeLoader.isNativeAvailable()) {
            try {
                double[] cameraPosArr = new double[]{cameraPos.x, cameraPos.y, cameraPos.z};
                Matrix4f projMatrix = ShaderManager.INSTANCE.getProjectionMatrix();
                Quaternionf cameraRotation = new Quaternionf(mc.gameRenderer.getMainCamera().rotation());
                Matrix4f mvMatrix = new Matrix4f().rotation(cameraRotation.conjugate());
                float[] projectionArr = new float[16];
                float[] modelViewArr = new float[16];
                projMatrix.get(projectionArr);
                mvMatrix.get(modelViewArr);
                double[] playerViewVecArr = new double[]{playerViewVec != null ? playerViewVec.x : 0.0, playerViewVec != null ? playerViewVec.y : 0.0, playerViewVec != null ? playerViewVec.z : 0.0};

                double[] positions = new double[count * 9];
                double[] textWidths = new double[count * 2];
                int[] booleans = new int[count * 5];
                int[] armorCounts = new int[count];

                for (int i = 0; i < count; i++) {
                    Entity target = candidates.get(i);
                    Vec3 basePos = target.getPosition(pt);
                    float bbHeight = target.getBbHeight();
                    float bbWidth = target.getBbWidth();
                    Vec3 headPos = basePos.add(0, bbHeight, 0);
                    Vec3 sidePos = basePos.add(bbWidth / 2.0f, bbHeight / 2.0f, 0);
                    positions[i * 9 + 0] = basePos.x; positions[i * 9 + 1] = basePos.y; positions[i * 9 + 2] = basePos.z;
                    positions[i * 9 + 3] = headPos.x; positions[i * 9 + 4] = headPos.y; positions[i * 9 + 5] = headPos.z;
                    positions[i * 9 + 6] = sidePos.x; positions[i * 9 + 7] = sidePos.y; positions[i * 9 + 8] = sidePos.z;

                    boolean drawNametags = nameTagsEnabled && (target instanceof LivingEntity);
                    String ownerName = (mobOwnerEnabled && target instanceof LivingEntity living) ? MobOwner.getOwnerName(living) : null;
                    boolean hasOwner = ownerName != null;

                    double tw = 0, ow = 0;
                    boolean showArmor = false, showHands = false, hasMainHand = false, hasOffHand = false;
                    int armorCnt = 0;

                    if (drawNametags || hasOwner) {
                        LivingEntity livingTarget = (LivingEntity) target;
                        if (drawNametags) {
                            String displayName = livingTarget.getDisplayName().getString();
                            int health = (int) Math.ceil(livingTarget.getHealth());
                            int maxHealth = (int) Math.ceil(livingTarget.getMaxHealth());
                            String tagText = displayName + " §a" + health + "§7/§a" + maxHealth;
                            tw = ModuleManager.get(NameTags.class).customFont.getValue() ? FontRenderUtility.getStringWidth(tagText) : mc.font.width(tagText);
                            showArmor = ModuleManager.get(NameTags.class).armor.getValue();
                            showHands = ModuleManager.get(NameTags.class).handItems.getValue();
                            hasMainHand = !livingTarget.getMainHandItem().isEmpty();
                            hasOffHand = !livingTarget.getOffhandItem().isEmpty();
                            if (showArmor) {
                                if (!livingTarget.getItemBySlot(EquipmentSlot.FEET).isEmpty()) armorCnt++;
                                if (!livingTarget.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) armorCnt++;
                                if (!livingTarget.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) armorCnt++;
                                if (!livingTarget.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) armorCnt++;
                            }
                        }
                        if (hasOwner) {
                            ow = ModuleManager.get(NameTags.class).customFont.getValue() ? FontRenderUtility.getStringWidth("Owner: " + ownerName) : mc.font.width("Owner: " + ownerName);
                        }
                    }
                    textWidths[i * 2 + 0] = tw;
                    textWidths[i * 2 + 1] = ow;
                    booleans[i * 5 + 0] = showArmor ? 1 : 0;
                    booleans[i * 5 + 1] = showHands ? 1 : 0;
                    booleans[i * 5 + 2] = hasOwner ? 1 : 0;
                    booleans[i * 5 + 3] = hasMainHand ? 1 : 0;
                    booleans[i * 5 + 4] = hasOffHand ? 1 : 0;
                    armorCounts[i] = armorCnt;
                }

                outLayouts = new double[count * 16];
                outIndices = new int[count];
                renderedCount = GuiOptimizer.nativeOptimizeNameTags(
                    cameraPosArr, modelViewArr, projectionArr, playerViewVecArr,
                    positions, textWidths, booleans, armorCounts, count,
                    ModuleManager.get(NameTags.class).scale.getValue(), ModuleManager.get(NameTags.class).distanceScaling.getValue(),
                    Math.max(ModuleManager.get(ESP.class).maxDistance.getValue(), nameTagsEnabled ? ModuleManager.get(NameTags.class).range.getValue() : 0.0),
                    guiWidth, guiHeight, outLayouts, outIndices
                );
                nativeSuccess = true;
            } catch (Throwable t) { nativeSuccess = false; }
        }

        if (nativeSuccess) {
            renderNativeLayout(context, mc, candidates, renderedCount, outIndices, outLayouts, espEnabled, nameTagsEnabled, mobOwnerEnabled);
        } else {
            renderFallbackLayout(context, mc, candidates, cameraPos, cameraLook, pt, guiWidth, guiHeight, espEnabled, nameTagsEnabled, mobOwnerEnabled);
        }
    }

    private void renderNativeLayout(GuiGraphics context, Minecraft mc, List<Entity> candidates, int renderedCount, int[] outIndices, double[] outLayouts, boolean espEnabled, boolean nameTagsEnabled, boolean mobOwnerEnabled) {
        for (int k = 0; k < renderedCount; k++) {
            int idx = outIndices[k];
            Entity target = candidates.get(idx);

            double scale = outLayouts[k * 16 + 0];
            double totalW = outLayouts[k * 16 + 1];
            double totalH = outLayouts[k * 16 + 2];
            double armorRowY = outLayouts[k * 16 + 3];
            double mainRowY = outLayouts[k * 16 + 4];
            double ownerRowY = outLayouts[k * 16 + 5];
            double textYOff = outLayouts[k * 16 + 6];
            double mainRowW = outLayouts[k * 16 + 7];
            double armorRowW = outLayouts[k * 16 + 8];
            double sx_base = outLayouts[k * 16 + 9];
            double sy_base = outLayouts[k * 16 + 10];
            double sy_head = outLayouts[k * 16 + 11];
            double sx_side = outLayouts[k * 16 + 12];
            double dist = outLayouts[k * 16 + 13];

            int bx = (int) sx_base, by = (int) sy_base, hy = (int) sy_head, sx = (int) sx_side;
            int boxH = Math.abs(by - hy);
            int halfBoxW = Math.max(2, Math.abs(sx - bx));
            int boxW = halfBoxW * 2;
            int boxX = bx - halfBoxW;
            int y = Math.min(by, hy);

            boolean isPlayer = target instanceof Player;
            boolean isMonster = target instanceof LivingEntity le && MobUtility.isHostile(le);
            boolean isAnimal = target instanceof net.minecraft.world.entity.animal.Animal || target instanceof net.minecraft.world.entity.ambient.AmbientCreature;
            boolean isItem = target instanceof net.minecraft.world.entity.item.ItemEntity;

            String ownerName = (mobOwnerEnabled && target instanceof LivingEntity living) ? MobOwner.getOwnerName(living) : null;
            boolean hasOwner = ownerName != null;

            if (espEnabled) {
                renderESPBox(context, mc, target, boxX, y, boxW, boxH, isPlayer, isMonster, isAnimal, isItem);
            }

            boolean withinRange = NameTags.isNativeAvailable() ? NameTags.nativeIsWithinRange(dist, ModuleManager.get(NameTags.class).range.getValue()) : (dist <= ModuleManager.get(NameTags.class).range.getValue());
            boolean drawNametags = nameTagsEnabled && (target instanceof LivingEntity) && withinRange;
            if (drawNametags || hasOwner) {
                renderNametag(context, mc, target, bx, y, scale, totalW, totalH, armorRowY, mainRowY, ownerRowY, textYOff, mainRowW, armorRowW, drawNametags, hasOwner, ownerName);
            }
        }
    }

    private void renderFallbackLayout(GuiGraphics context, Minecraft mc, List<Entity> candidates, Vec3 cameraPos, Vec3 cameraLook, float pt, int guiWidth, int guiHeight, boolean espEnabled, boolean nameTagsEnabled, boolean mobOwnerEnabled) {
        for (Entity target : candidates) {
            boolean isPlayer = target instanceof Player;
            boolean isMonster = target instanceof LivingEntity le && MobUtility.isHostile(le);
            boolean isAnimal = target instanceof net.minecraft.world.entity.animal.Animal || target instanceof net.minecraft.world.entity.ambient.AmbientCreature;
            boolean isItem = target instanceof net.minecraft.world.entity.item.ItemEntity;

            Vec3 basePos = target.getPosition(pt);
            double dist = mc.player.distanceTo(target);
            float bbHeight = target.getBbHeight();
            float bbWidth = target.getBbWidth();
            Vec3 headPos = basePos.add(0, bbHeight, 0);
            Vec3 sidePos = basePos.add(bbWidth / 2.0f, bbHeight / 2.0f, 0);

            Vec3 baseProj = mc.gameRenderer.projectPointToScreen(basePos);
            Vec3 headProj = mc.gameRenderer.projectPointToScreen(headPos);
            Vec3 sideProj = mc.gameRenderer.projectPointToScreen(sidePos);
            if (baseProj == null || headProj == null || sideProj == null) continue;

            Vec3 dir = (new Vec3(basePos.x - cameraPos.x, basePos.y - cameraPos.y, basePos.z - cameraPos.z)).normalize();
            if (dir.dot(cameraLook) <= 0.0) continue;

            double sx_base = (baseProj.x + 1.0) / 2.0 * guiWidth;
            double sy_base = (1.0 - baseProj.y) / 2.0 * guiHeight;
            double sx_head = (headProj.x + 1.0) / 2.0 * guiWidth;
            double sy_head = (1.0 - headProj.y) / 2.0 * guiHeight;
            double sx_side = (sideProj.x + 1.0) / 2.0 * guiWidth;

            if ((sx_base < 0 || sx_base > guiWidth || sy_base < 0 || sy_base > guiHeight) &&
                (sx_head < 0 || sx_head > guiWidth || sy_head < 0 || sy_head > guiHeight)) continue;

            int bx = (int) sx_base, by = (int) sy_base, hy = (int) sy_head, sx = (int) sx_side;
            int boxH = Math.abs(by - hy);
            int halfBoxW = Math.max(2, Math.abs(sx - bx));
            int boxW = halfBoxW * 2;
            int boxX = bx - halfBoxW;
            int y = Math.min(by, hy);

            String ownerName = (mobOwnerEnabled && target instanceof LivingEntity living) ? MobOwner.getOwnerName(living) : null;
            boolean hasOwner = ownerName != null;

            if (espEnabled) {
                renderESPBox(context, mc, target, boxX, y, boxW, boxH, isPlayer, isMonster, isAnimal, isItem);
            }

            boolean withinRange = NameTags.isNativeAvailable() ? NameTags.nativeIsWithinRange(dist, ModuleManager.get(NameTags.class).range.getValue()) : (dist <= ModuleManager.get(NameTags.class).range.getValue());
            boolean drawNametags = nameTagsEnabled && (target instanceof LivingEntity) && withinRange;
            if (drawNametags || hasOwner) {
                LivingEntity livingTarget = (LivingEntity) target;
                String displayName = livingTarget.getDisplayName().getString();
                int health = (int) Math.ceil(livingTarget.getHealth());
                int maxHealth = (int) Math.ceil(livingTarget.getMaxHealth());
                String tagText = displayName + " §a" + health + "§7/§a" + maxHealth;
                double tw = drawNametags ? (ModuleManager.get(NameTags.class).customFont.getValue() ? FontRenderUtility.getStringWidth(tagText) : mc.font.width(tagText)) : 0.0;
                double ow = hasOwner ? (ModuleManager.get(NameTags.class).customFont.getValue() ? FontRenderUtility.getStringWidth("Owner: " + ownerName) : mc.font.width("Owner: " + ownerName)) : 0.0;

                boolean showArmor = drawNametags && ModuleManager.get(NameTags.class).armor.getValue();
                boolean showHands = drawNametags && ModuleManager.get(NameTags.class).handItems.getValue();
                boolean hasMainHand = drawNametags && !livingTarget.getMainHandItem().isEmpty();
                boolean hasOffHand = drawNametags && !livingTarget.getOffhandItem().isEmpty();

                int armorCount = 0;
                if (showArmor) {
                    if (!livingTarget.getItemBySlot(EquipmentSlot.FEET).isEmpty()) armorCount++;
                    if (!livingTarget.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) armorCount++;
                    if (!livingTarget.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) armorCount++;
                    if (!livingTarget.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) armorCount++;
                }

                double[] layout;
                if (NameTags.isNativeAvailable()) {
                    layout = NameTags.nativeCalculateLayout(dist, ModuleManager.get(NameTags.class).scale.getValue(), ModuleManager.get(NameTags.class).distanceScaling.getValue(), showArmor, showHands, hasOwner, tw, ow, hasMainHand, hasOffHand, armorCount);
                } else {
                    layout = NameTags.javaFallbackCalculate(dist, ModuleManager.get(NameTags.class).scale.getValue(), ModuleManager.get(NameTags.class).distanceScaling.getValue(), showArmor, showHands, hasOwner, tw, ow, hasMainHand, hasOffHand, armorCount);
                }

                double scale = layout[0], totalW = layout[1], totalH = layout[2];
                double armorRowY = layout[3], mainRowY = layout[4], ownerRowY = layout[5];
                double textYOff = layout[6], mainRowW = layout[7], armorRowW = layout[8];

                renderNametagAt(context, mc, bx, y, scale, totalW, totalH, armorRowY, mainRowY, ownerRowY, textYOff, mainRowW, armorRowW, drawNametags, hasOwner, livingTarget, ownerName, tagText, tw, ow, showArmor, showHands, armorCount, hasMainHand, hasOffHand);
            }
        }
    }

    private void renderESPBox(GuiGraphics context, Minecraft mc, Entity target, int boxX, int y, int boxW, int boxH, boolean isPlayer, boolean isMonster, boolean isAnimal, boolean isItem) {
        String mode = ModuleManager.get(ESP.class).mode.getValue();
        int espColor = isPlayer ? ModuleManager.get(ESP.class).playerColor.getValue()
            : (isMonster ? ModuleManager.get(ESP.class).mobColor.getValue()
            : (isAnimal ? ModuleManager.get(ESP.class).animalColor.getValue()
            : (isItem ? ModuleManager.get(ESP.class).itemColor.getValue()
            : ModuleManager.get(ESP.class).frameColor.getValue())));

        if ("Box2D".equals(mode)) {
            context.fill(boxX, y, boxX + boxW, y + 1, espColor);
            context.fill(boxX, y + boxH, boxX + boxW, y + boxH + 1, espColor);
            context.fill(boxX, y, boxX + 1, y + boxH, espColor);
            context.fill(boxX + boxW - 1, y, boxX + boxW, y + boxH, espColor);
            int ca = 4;
            context.fill(boxX, y, boxX + ca, y + 1, espColor);
            context.fill(boxX + boxW - ca, y, boxX + boxW, y + 1, espColor);
            context.fill(boxX, y + boxH, boxX + ca, y + boxH + 1, espColor);
            context.fill(boxX + boxW - ca, y + boxH, boxX + boxW, y + boxH + 1, espColor);
        }
    }

    private void renderNametag(GuiGraphics context, Minecraft mc, Entity target, int bx, int y, double scale, double totalW, double totalH, double armorRowY, double mainRowY, double ownerRowY, double textYOff, double mainRowW, double armorRowW, boolean drawNametags, boolean hasOwner, String ownerName) {
        LivingEntity livingTarget = (LivingEntity) target;
        String displayName = livingTarget.getDisplayName().getString();
        int health = (int) Math.ceil(livingTarget.getHealth());
        int maxHealth = (int) Math.ceil(livingTarget.getMaxHealth());
        String tagText = displayName + " §a" + health + "§7/§a" + maxHealth;
        double tw = drawNametags ? (ModuleManager.get(NameTags.class).customFont.getValue() ? FontRenderUtility.getStringWidth(tagText) : mc.font.width(tagText)) : 0.0;
        double ow = hasOwner ? (ModuleManager.get(NameTags.class).customFont.getValue() ? FontRenderUtility.getStringWidth("Owner: " + ownerName) : mc.font.width("Owner: " + ownerName)) : 0.0;
        boolean showArmor = drawNametags && ModuleManager.get(NameTags.class).armor.getValue();
        boolean showHands = drawNametags && ModuleManager.get(NameTags.class).handItems.getValue();
        boolean hasMainHand = drawNametags && !livingTarget.getMainHandItem().isEmpty();
        boolean hasOffHand = drawNametags && !livingTarget.getOffhandItem().isEmpty();
        int armorCount = 0;
        if (showArmor) {
            if (!livingTarget.getItemBySlot(EquipmentSlot.FEET).isEmpty()) armorCount++;
            if (!livingTarget.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) armorCount++;
            if (!livingTarget.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) armorCount++;
            if (!livingTarget.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) armorCount++;
        }
        renderNametagAt(context, mc, bx, y, scale, totalW, totalH, armorRowY, mainRowY, ownerRowY, textYOff, mainRowW, armorRowW, drawNametags, hasOwner, livingTarget, ownerName, tagText, tw, ow, showArmor, showHands, armorCount, hasMainHand, hasOffHand);
    }

    private void renderNametagAt(GuiGraphics context, Minecraft mc, int bx, int y, double scale, double totalW, double totalH, double armorRowY, double mainRowY, double ownerRowY, double textYOff, double mainRowW, double armorRowW, boolean drawNametags, boolean hasOwner, LivingEntity livingTarget, String ownerName, String tagText, double tw, double ow, boolean showArmor, boolean showHands, int armorCount, boolean hasMainHand, boolean hasOffHand) {
        context.pose().pushMatrix();
        context.pose().translate((float) bx, (float) y);
        context.pose().scale((float) scale, (float) scale);

        boolean bgEnabled = drawNametags ? ModuleManager.get(NameTags.class).background.getValue() : (hasOwner && ModuleManager.get(MobOwner.class).background.getValue());
        if (bgEnabled) {
            int bgColor = drawNametags ? ModuleManager.get(NameTags.class).backgroundColor.getValue() : 0xBB000000;
            int bgLeft = (int)(-totalW / 2.0 - 3.0);
            int bgRight = (int)(totalW / 2.0 + 3.0);
            int bgBottom = -2;
            int bgTop = (int)(-2.0 - totalH);
            context.fill(bgLeft, bgTop, bgRight, bgBottom, bgColor);
            boolean drawTopLine = false;
            int lineCol = 0;
            if (drawNametags) { drawTopLine = ModuleManager.get(NameTags.class).topLine.getValue(); lineCol = ModuleManager.get(NameTags.class).topLineColor.getValue(); }
            else if (hasOwner) { drawTopLine = ModuleManager.get(MobOwner.class).background.getValue(); lineCol = 0x44FFAA00; }
            if (drawTopLine) context.fill(bgLeft, bgTop, bgRight, bgTop + 1, lineCol);
        }

        if (showArmor && armorCount > 0) {
            int ax = (int)(-armorRowW / 2.0);
            int ay = (int) armorRowY;
            ItemStack[] armorItems = new ItemStack[]{
                livingTarget.getItemBySlot(EquipmentSlot.FEET),
                livingTarget.getItemBySlot(EquipmentSlot.LEGS),
                livingTarget.getItemBySlot(EquipmentSlot.CHEST),
                livingTarget.getItemBySlot(EquipmentSlot.HEAD)
            };
            for (ItemStack stack : armorItems) {
                if (!stack.isEmpty()) { context.renderItem(stack, ax, ay); ax += 16 + 2; }
            }
        }

        if (drawNametags) {
            int rx = (int)(-mainRowW / 2.0);
            int mY = (int) mainRowY;
            if (showHands && hasMainHand) { context.renderItem(livingTarget.getMainHandItem(), rx, mY); rx += 16 + 2; }
            if (ModuleManager.get(NameTags.class).customFont.getValue()) {
                FontRenderUtility.drawString(context, tagText, rx, (int)(mY + textYOff), 0xFFFFFFFF, false);
            } else {
                context.drawString(mc.font, tagText, rx, (int)(mY + textYOff), 0xFFFFFFFF, false);
            }
            rx += (int) tw;
            if (showHands && hasOffHand) { rx += 2; context.renderItem(livingTarget.getOffhandItem(), rx, mY); }
        }

        if (hasOwner) {
            int otx = (int)(-ow / 2.0);
            int oty = (int) ownerRowY;
            if (ModuleManager.get(NameTags.class).customFont.getValue()) {
                FontRenderUtility.drawString(context, "Owner: " + ownerName, otx, oty, ModuleManager.get(MobOwner.class).textColor.getValue(), false);
            } else {
                context.drawString(mc.font, "Owner: " + ownerName, otx, oty, ModuleManager.get(MobOwner.class).textColor.getValue(), false);
            }
        }

        context.pose().popMatrix();
    }

    private void renderDamageLabels(GuiGraphics context, Minecraft mc, float pt, Vec3 cameraPos, Vec3 cameraLook, int guiWidth, int guiHeight) {

        BasePlace bp = ModuleManager.get(BasePlace.class);
        if (bp.getEnabled() && BasePlace.getSimulatedPlacementBlock() != null) {
            renderDamageLabel(context, mc, BasePlace.getSimulatedPlacementBlock(), "Dmg: %.1f | Self: %.1f", BasePlace.currentTargetDamage, BasePlace.currentSelfDamage, 0xFF00FF00, pt, cameraPos, cameraLook, guiWidth, guiHeight);
        }


        AnchorAura aa = ModuleManager.get(AnchorAura.class);
        if (aa.getEnabled() && AnchorAura.simulatedPlacementBlock != null) {
            renderDamageLabel(context, mc, AnchorAura.simulatedPlacementBlock, "Dmg: %.1f | Self: %.1f", AnchorAura.currentTargetDamage, AnchorAura.currentSelfDamage, 0xFF00FFFF, pt, cameraPos, cameraLook, guiWidth, guiHeight);
        }


        AutoCrystal ac = ModuleManager.get(AutoCrystal.class);
        if (ac.getEnabled() && ac.renderDamage.getValue() && AutoCrystal.currentPlacementBlock != null) {
            BlockPos p = AutoCrystal.currentPlacementBlock;
            Vec3 pos3d = new Vec3(p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5);
            Vec3 proj = mc.gameRenderer.projectPointToScreen(pos3d);
            if (proj != null) {
                Vec3 dir = pos3d.subtract(cameraPos).normalize();
                Vec3 look = mc.player.getViewVector(pt);
                if (dir.dot(look) > 0.0) {
                    double sx = (proj.x + 1.0) / 2.0 * guiWidth;
                    double sy = (1.0 - proj.y) / 2.0 * guiHeight;
                    int x = (int) sx, y = (int) sy;
                    String dmgText = String.format("Target: %.1f | Self: %.1f", AutoCrystal.currentTargetDamage, AutoCrystal.currentSelfDamage);
                    String totemsText = String.format("Totems: %d", AutoCrystal.currentTargetTotems);
                    renderLabelBox(context, mc, x, y, dmgText, totemsText, 0xFF00DDFF);
                }
            }
        }


        PVEUtils asm = ModuleManager.get(PVEUtils.class);
        if (asm.getEnabled() && asm.mode.getValue().equals("AutoSmelt") && asm.smeltRender.getValue() && PVEUtils.smeltTarget != null) {
            BlockPos p = PVEUtils.smeltTarget;
            Vec3 pos3d = new Vec3(p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5);
            Vec3 proj = mc.gameRenderer.projectPointToScreen(pos3d);
            if (proj != null) {
                Vec3 dir = pos3d.subtract(cameraPos).normalize();
                Vec3 look = mc.player.getViewVector(pt);
                if (dir.dot(look) > 0.0) {
                    double sx = (proj.x + 1.0) / 2.0 * guiWidth;
                    double sy = (1.0 - proj.y) / 2.0 * guiHeight;
                    int x = (int) sx, y = (int) sy;
                    String statusText;
                    if (mc.player.containerMenu instanceof net.minecraft.world.inventory.AbstractFurnaceMenu furnace) {
                        var result = furnace.getSlot(2).getItem();
                        var input = furnace.getSlot(0).getItem();
                        if (!result.isEmpty()) statusText = "§fResult: §e" + result.getHoverName().getString() + " §7x" + result.getCount();
                        else if (!input.isEmpty()) statusText = "§7Smelting: §f" + input.getHoverName().getString() + " §7(" + (int)(furnace.getBurnProgress() * 100) + "%)";
                        else statusText = "§7Idle";
                    } else statusText = "§7Furnace";
                    context.drawString(mc.font, statusText, x - mc.font.width(statusText) / 2, y - 4, 0xFFFFFFFF, true);
                }
            }
        }


        if (asm.getEnabled() && asm.mode.getValue().equals("AutoBrew") && asm.brewRender.getValue()) {
            BlockPos p = PVEUtils.getBrewTarget();
            if (p != null) {
                Vec3 pos3d = new Vec3(p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5);
                Vec3 proj = mc.gameRenderer.projectPointToScreen(pos3d);
                if (proj != null) {
                    Vec3 dir = pos3d.subtract(cameraPos).normalize();
                    Vec3 look = mc.player.getViewVector(pt);
                    if (dir.dot(look) > 0.0) {
                        double sx = (proj.x + 1.0) / 2.0 * guiWidth;
                        double sy = (1.0 - proj.y) / 2.0 * guiHeight;
                        int x = (int) sx, y = (int) sy;
                        String statusText;
                        if (mc.player.containerMenu instanceof net.minecraft.world.inventory.BrewingStandMenu brew) {
                            int ticks = brew.getBrewingTicks();
                            int fuel = brew.getFuel();
                            var ingr = brew.getSlot(3).getItem();
                            if (ticks > 0) statusText = "§dBrewing... §7(" + (400 - ticks) / 20 + "s)";
                            else if (!ingr.isEmpty()) statusText = "§dReady: §f" + ingr.getHoverName().getString();
                            else statusText = "§7Idle";
                            String fuelText = "§7Fuel: " + fuel;
                            String total = statusText + " | " + fuelText;
                            context.drawString(mc.font, total, x - mc.font.width(total) / 2, y - 4, 0xFFFFFFFF, true);
                        }
                    }
                }
            }
        }
    }

    private void renderDamageLabel(GuiGraphics context, Minecraft mc, BlockPos p, String format, double targetDmg, double selfDmg, int borderColor, float pt, Vec3 cameraPos, Vec3 cameraLook, int guiWidth, int guiHeight) {
        Vec3 pos3d = new Vec3(p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5);
        Vec3 proj = mc.gameRenderer.projectPointToScreen(pos3d);
        if (proj != null) {
            Vec3 dir = pos3d.subtract(cameraPos).normalize();
            Vec3 look = mc.player.getViewVector(pt);
            if (dir.dot(look) > 0.0) {
                double sx = (proj.x + 1.0) / 2.0 * guiWidth;
                double sy = (1.0 - proj.y) / 2.0 * guiHeight;
                int x = (int) sx, y = (int) sy;
                String text = String.format(format, targetDmg, selfDmg);
                int w = mc.font.width(text);
                context.fill(x - w / 2 - 4, y - 5, x + w / 2 + 4, y + 5, 0xAA000000);
                context.fill(x - w / 2 - 4, y - 5, x + w / 2 + 4, y - 4, borderColor);
                context.drawString(mc.font, text, x - w / 2, y - 4, 0xFFFFFFFF, false);
            }
        }
    }

    private void renderLabelBox(GuiGraphics context, Minecraft mc, int x, int y, String line1, String line2, int borderColor) {
        int w1 = mc.font.width(line1);
        int w2 = mc.font.width(line2);
        int w = Math.max(w1, w2);
        int left = x - w / 2 - 4, top = y - 10, right = x + w / 2 + 4, bottom = y + 10;
        context.fill(left, top, right, bottom, 0xAA000000);
        context.fill(left, top, right, top + 1, borderColor);
        context.drawString(mc.font, line1, x - w1 / 2, y - 8, 0xFFFFFFFF, false);
        context.drawString(mc.font, line2, x - w2 / 2, y + 1, 0xFFFFCC00, false);
    }

    private void renderPacketMine(GuiGraphics context, Minecraft mc) {
        PacketMine pm = ModuleManager.get(PacketMine.class);
        if (pm.getEnabled() && pm.render.getValue()) {
            for (var mb : PacketMine.miningBlocks) {
                if (mb == null || mb.pos == null) continue;
                long now = System.currentTimeMillis();
                if (mb.done && now > mb.visibleUntil) continue;
                Vec3 pos3d = new Vec3(mb.pos.getX() + 0.5, mb.pos.getY() + 1.4, mb.pos.getZ() + 0.5);
                Vec3 proj = projectPointToScreenUnbobbed(pos3d);
                if (proj != null) {
                    double sx = (proj.x + 1.0) / 2.0 * context.guiWidth();
                    double sy = (1.0 - proj.y) / 2.0 * context.guiHeight();
                    int x = (int) sx, y = (int) sy;
                    long elapsed = now - mb.startTime;
                    int pct = mb.done ? 100 : (int)((float)elapsed / Math.max(1, mb.breakAt) * 100);
                    pct = Math.min(100, pct);
                    String text = mb.done ? "Done" : pct + "%";
                    context.drawString(mc.font, text, x - mc.font.width(text) / 2, y - 4, 0xFFFFFFFF, true);
                }
            }
        }
    }

    private void renderWaypoints(GuiGraphics context, Minecraft mc) {
        if (ModuleManager.get(Waypoint.class).getEnabled()) {
            int wpColor = ModuleManager.get(Waypoint.class).color.getValue();
            String currentDim = mc.level != null ? mc.level.dimension().identifier().toString() : null;
            boolean showDist = ModuleManager.get(Waypoint.class).showDistance.getValue();
            boolean showNm = ModuleManager.get(Waypoint.class).showName.getValue();
            for (var wp : Waypoint.getWaypoints()) {
                if (currentDim != null && !wp.dimension().equals(currentDim)) continue;
                Vec3 pos3d = new Vec3(wp.x() + 0.5, wp.y() + 1.5, wp.z() + 0.5);
                Vec3 proj = mc.gameRenderer.projectPointToScreen(pos3d);
                if (proj != null && proj.z > 0.0) {
                    double sx = (proj.x + 1.0) / 2.0 * context.guiWidth();
                    double sy = (1.0 - proj.y) / 2.0 * context.guiHeight();
                    int ix = (int) sx, iy = (int) sy;
                    double dist = Math.sqrt(mc.player.distanceToSqr(wp.x(), wp.y(), wp.z()));
                    String text = showNm ? wp.name() : "";
                    if (showDist) text += (text.isEmpty() ? "" : " ") + "§7" + (int)dist + "m";
                    if (text.isEmpty()) text = wp.name();
                    int tw = mc.font.width(text);
                    int th = mc.font.lineHeight;
                    int pad = 3;
                    context.pose().pushMatrix();
                    context.pose().translate(ix, iy);
                    context.fill(-tw / 2 - pad, -th / 2 - pad, tw / 2 + pad, th / 2 + pad, 0xAA000000);
                    context.drawString(mc.font, text, -tw / 2, -th / 2, wpColor, false);
                    context.pose().popMatrix();
                }
            }
        }
    }

    private void renderHud(GuiGraphics context, DeltaTracker tickCounter) {
        List<Module> enabledModules = new ArrayList<>();
        for (Module hud : ModuleManager.INSTANCE.getHudModules()) {
            if (hud.getEnabled()) enabledModules.add(hud);
        }
        GuiOptimizer.optimizeHudAnimations(enabledModules);

        Module dragHud = Hud.draggingHud;
        if (dragHud != null) {
            Minecraft mc = Minecraft.getInstance();
            double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getWidth();
            double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getHeight();
            int nx = (int)mx - Hud.dragOffX;
            int ny = (int)my - Hud.dragOffY;
            nx = Math.max(0, Math.min(mc.getWindow().getGuiScaledWidth() - dragHud.getWidth(), nx));
            ny = Math.max(0, Math.min(mc.getWindow().getGuiScaledHeight() - dragHud.getHeight(), ny));
            dragHud.setX(nx);
            dragHud.setY(ny);
            dragHud.setDisplayX(nx);
            dragHud.setDisplayY(ny);
        }

        for (Module hud : enabledModules) {
            try { hud.render(context, tickCounter.getGameTimeDeltaTicks()); } catch (Throwable ignored) {}
        }
    }

    private void drawTracerLine2D(GuiGraphics context, float x1, float y1, float x2, float y2, int color, float width) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.atan2(dy, dx);
        context.pose().pushMatrix();
        context.pose().translate(x1, y1);
        context.pose().rotate(angle);
        context.pose().scale(1.0f, width);
        context.pose().translate(0.0f, -0.5f);
        context.fill(0, 0, (int) len, 1, color);
        context.pose().popMatrix();
    }

    private Vec3 projectPointToScreenUnbobbed(Vec3 pos) {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.Camera camera = mc.gameRenderer.getMainCamera();
        Matrix4f projectionMatrix = ShaderManager.INSTANCE.getProjectionMatrix();
        if (projectionMatrix == null) return null;
        Quaternionf cameraRotation = new Quaternionf(camera.rotation());
        Matrix4f modelViewMatrix = new Matrix4f().rotation(cameraRotation.conjugate());
        Vec3 camPos = camera.position();
        Vector4f vector4f = new Vector4f((float) (pos.x - camPos.x), (float) (pos.y - camPos.y), (float) (pos.z - camPos.z), 1.0F);
        modelViewMatrix.transform(vector4f);
        projectionMatrix.transform(vector4f);
        if (vector4f.w <= 0.0F) return null;
        vector4f.div(vector4f.w);
        return new Vec3(vector4f.x, vector4f.y, vector4f.z);
    }
}
