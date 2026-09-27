package ravex.render.hud;

import net.minecraft.client.DeltaTracker;
import ravex.mcwrapper.MinecraftWrapper;
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
import ravex.modules.render.Ambient;
import ravex.modules.render.Crosshair;
import ravex.modules.render.ESP;
import ravex.modules.render.NameTags;
import ravex.modules.render.Tracers;
import ravex.modules.render.Waypoint;
import ravex.modules.player.MobOwner;
import ravex.modules.combat.BasePlace;
import ravex.modules.combat.AnchorAura;
import ravex.modules.combat.AutoCrystal;
import ravex.modules.player.PacketMine;
import ravex.modules.world.PVEUtils;
import ravex.modules.client.Hud;
import ravex.utility.misc.GuiOptimizerUtility;

import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.HudRendererUtility;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import ravex.utility.render.Render2DUtility;
import ravex.utility.render.ColorUtility;

import java.util.ArrayList;
import java.util.List;

import ravex.utility.misc.EntityUtility;
import ravex.modules.Modules;
public final class HudRenderer {
    public static final HudRenderer INSTANCE = new HudRenderer();

    private double acAnimX = 0, acAnimY = 0, acAnimZ = 0;
    private float acAlpha = 0f;
    private double aaAnimX = 0, aaAnimY = 0, aaAnimZ = 0;
    private float aaAlpha = 0f;
    private double bpAnimX = 0, bpAnimY = 0, bpAnimZ = 0;
    private float bpAlpha = 0f;
    private double rotAnimX = 0, rotAnimY = 0, rotAnimZ = 0;
    private float rotAlpha = 0f;
    private long lastLabelTime = 0;

    private HudRenderer() {}

    public void render(GuiGraphics context, DeltaTracker tickCounter) {
        var mc = MinecraftWrapper.getWrapper();

        if (Modules.enabled(Ambient.class)) {
            int color = ModuleManager.get(Ambient.class).color;
            Render2DUtility.drawRect(context, 0, 0, context.guiWidth(), context.guiHeight(), color);
        }

        if (mc.getLevel() == null || mc.getPlayer() == null) {
            renderHud(context, tickCounter);
            return;
        }

        float pt = tickCounter.getGameTimeDeltaPartialTick(true);
        Vec3 cameraPos = mc.getGameRenderer().getMainCamera().position();
        boolean espEnabled = Modules.enabled(ESP.class);
        boolean nameTagsEnabled = Modules.enabled(NameTags.class);
        boolean mobOwnerEnabled = Modules.enabled(MobOwner.class) && ModuleManager.get(MobOwner.class).animals;

        int guiWidth = context.guiWidth();
        int guiHeight = context.guiHeight();
        boolean firstPerson = mc.getOptions().getCameraType().isFirstPerson();
        Vec3 playerViewVec = mc.getPlayer() != null ? mc.getPlayer().getViewVector(pt) : null;
        Quaternionf cRotation = mc.getGameRenderer().getMainCamera().rotation();
        Vector4f lookVec = new Vector4f(0.0F, 0.0F, -1.0F, 0.0F).rotate(cRotation);
        Vec3 cameraLook = new Vec3(lookVec.x(), lookVec.y(), lookVec.z());
        boolean tracersEnabled = Modules.enabled(Tracers.class);

        List<Entity> candidates = buildCandidates(mc, espEnabled, nameTagsEnabled, mobOwnerEnabled, tracersEnabled, pt, firstPerson);

        renderTracers(context, mc, candidates, tracersEnabled, pt, cameraPos, cameraLook, guiWidth, guiHeight);

        renderNameTagsAndESP(context, mc, candidates, cameraPos, cameraLook, cRotation, pt, guiWidth, guiHeight, espEnabled, nameTagsEnabled, mobOwnerEnabled, playerViewVec);

        renderDamageLabels(context, mc, pt, cameraPos, cameraLook, guiWidth, guiHeight);
        renderPacketMine(context, mc);
        renderWaypoints(context, mc);
        NotificationManager.render(context);

        if (Modules.enabled(Crosshair.class)) {
            try { ModuleManager.get(Crosshair.class).render(context); } catch (Throwable ignored) {}
        }

        renderHud(context, tickCounter);
    }

    private List<Entity> buildCandidates(MinecraftWrapper mc, boolean espEnabled, boolean nameTagsEnabled, boolean mobOwnerEnabled, boolean tracersEnabled, float pt, boolean firstPerson) {
        List<Entity> candidates = new ArrayList<>();
        for (Entity target : mc.getLevel().entitiesForRendering()) {
            if (target == mc.getPlayer()) continue;
            if (target instanceof LivingEntity living && !living.isAlive()) continue;

            double dist = mc.getPlayer().distanceTo(target);

            double maxDist = ModuleManager.get(ESP.class).maxDistance;
            if (nameTagsEnabled) maxDist = Math.max(maxDist, ModuleManager.get(NameTags.class).range);
            if (tracersEnabled) maxDist = Math.max(maxDist, ModuleManager.get(Tracers.class).maxDistance);
            if (dist > maxDist) continue;

            if (firstPerson && dist < 1.2 && !nameTagsEnabled && !tracersEnabled) continue;

            boolean isPlayer = target instanceof Player;
            boolean isMonster = target instanceof LivingEntity le && EntityUtility.isHostile(le);
            boolean isAnimal = target instanceof net.minecraft.world.entity.animal.Animal || target instanceof net.minecraft.world.entity.ambient.AmbientCreature;
            boolean isItem = target instanceof net.minecraft.world.entity.item.ItemEntity;
            boolean isFrame = target instanceof net.minecraft.world.entity.decoration.ItemFrame;

            boolean showESP = espEnabled && (
                (isPlayer && ModuleManager.get(ESP.class).players) ||
                (isMonster && ModuleManager.get(ESP.class).monsters) ||
                (isAnimal && ModuleManager.get(ESP.class).animals) ||
                (isItem && ModuleManager.get(ESP.class).items) ||
                (isFrame && ModuleManager.get(ESP.class).frames)
            );
            boolean showTracers = tracersEnabled && (
                (isPlayer && ModuleManager.get(Tracers.class).players) ||
                (isMonster && ModuleManager.get(Tracers.class).monsters) ||
                (isAnimal && ModuleManager.get(Tracers.class).animals) ||
                (isItem && ModuleManager.get(Tracers.class).items)
            );
            boolean showNameTags = nameTagsEnabled && Modules.get(NameTags.class).shouldDraw(target);
            boolean showOwner = mobOwnerEnabled && (target instanceof LivingEntity living) && MobOwner.getOwnerName(living) != null;

            if (!showESP && !showTracers && !showNameTags && !showOwner) continue;
            candidates.add(target);
        }
        return candidates;
    }

    private void renderTracers(GuiGraphics context, MinecraftWrapper mc, List<Entity> candidates, boolean tracersEnabled, float pt, Vec3 cameraPos, Vec3 cameraLook, int guiWidth, int guiHeight) {
        if (!tracersEnabled) return;
        Tracers tracers = ModuleManager.get(Tracers.class);

        List<Entity> tracerEntities = new ArrayList<>();
        List<Integer> tracerColors = new ArrayList<>();

        for (Entity target : candidates) {
            boolean isPlayer = target instanceof Player;
            boolean isMonster = target instanceof LivingEntity le && EntityUtility.isHostile(le);
            boolean isAnimal = target instanceof net.minecraft.world.entity.animal.Animal || target instanceof net.minecraft.world.entity.ambient.AmbientCreature;
            boolean isItem = target instanceof net.minecraft.world.entity.item.ItemEntity;

            boolean show = false;
            int color = 0;
            if (isPlayer && tracers.players) { show = true; color = tracers.playerColor; }
            else if (isMonster && tracers.monsters) { show = true; color = tracers.mobColor; }
            else if (isAnimal && tracers.animals) { show = true; color = tracers.animalColor; }
            else if (isItem && tracers.items) { show = true; color = tracers.itemColor; }

            if (show) {
                tracerEntities.add(target);
                tracerColors.add(color);
            }
        }

        if ("ArrowOld".equals(tracers.mode) || "ArrowNew".equals(tracers.mode)) {
            Tracers.renderArrows(context, tracerEntities, tracerColors, pt, cameraPos, cameraLook, guiWidth, guiHeight);
        } else {
            float width = 1.5f;
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
                    Render2DUtility.drawLine(context, (float) cx, (float) cy, (float) ex, (float) ey, width, color);
                }
            }
        }
    }

    private void renderNameTagsAndESP(GuiGraphics context, MinecraftWrapper mc, List<Entity> candidates, Vec3 cameraPos, Vec3 cameraLook, Quaternionf cRotation, float pt, int guiWidth, int guiHeight, boolean espEnabled, boolean nameTagsEnabled, boolean mobOwnerEnabled, Vec3 playerViewVec) {

        boolean nativeSuccess = false;
        int count = candidates.size();
        double[] outLayouts = null;
        int[] outIndices = null;
        int renderedCount = 0;

        if (count > 0 && ravex.utility.nativelib.NativeLoader.isNativeAvailable() && !nameTagsEnabled) {
            try {
                double[] cameraPosArr = new double[]{cameraPos.x, cameraPos.y, cameraPos.z};
                Matrix4f projMatrix = ShaderManager.INSTANCE.getProjectionMatrix();
                Quaternionf cameraRotation = new Quaternionf(mc.getGameRenderer().getMainCamera().rotation());
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

                    boolean drawNametags = nameTagsEnabled && (target instanceof LivingEntity) && Modules.get(NameTags.class).shouldDraw(target);
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
                            tw = ModuleManager.get(NameTags.class).customFont ? FontRenderUtility.getStringWidth(tagText) : mc.getFont().width(tagText);
                            showArmor = ModuleManager.get(NameTags.class).armor;
                            showHands = ModuleManager.get(NameTags.class).handItems;
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
                            ow = ModuleManager.get(NameTags.class).customFont ? FontRenderUtility.getStringWidth("Owner: " + ownerName) : mc.getFont().width("Owner: " + ownerName);
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
                renderedCount = GuiOptimizerUtility.nativeOptimizeNameTags(
                    cameraPosArr, modelViewArr, projectionArr, playerViewVecArr,
                    positions, textWidths, booleans, armorCounts, count,
                    ModuleManager.get(NameTags.class).size, true,
                    Math.max(ModuleManager.get(ESP.class).maxDistance, nameTagsEnabled ? ModuleManager.get(NameTags.class).range : 0.0),
                    guiWidth, guiHeight, outLayouts, outIndices
                );
                nativeSuccess = true;
            } catch (Throwable t) { nativeSuccess = false; }
        }

        if (nativeSuccess) {
            renderNativeLayout(context, mc, candidates, renderedCount, outIndices, outLayouts, espEnabled, nameTagsEnabled, mobOwnerEnabled);
        } else {
            renderFallbackLayout(context, mc, candidates, cameraPos, cameraLook, cRotation, pt, guiWidth, guiHeight, espEnabled, nameTagsEnabled, mobOwnerEnabled);
        }
    }

    private void renderNativeLayout(GuiGraphics context, MinecraftWrapper mc, List<Entity> candidates, int renderedCount, int[] outIndices, double[] outLayouts, boolean espEnabled, boolean nameTagsEnabled, boolean mobOwnerEnabled) {
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

            float bxF = (float) sx_base;
            float byF = (float) sy_base;
            float hyF = (float) sy_head;
            float sxF = (float) sx_side;
            int boxH = Math.round(Math.abs(byF - hyF));
            int halfBoxW = Math.max(2, Math.round(Math.abs(sxF - bxF)));
            int boxW = halfBoxW * 2;
            int boxX = Math.round(bxF) - halfBoxW;
            float anchorY = Math.min(byF, hyF);
            int y = Math.round(anchorY);

            boolean isPlayer = target instanceof Player;
            boolean isMonster = target instanceof LivingEntity le && EntityUtility.isHostile(le);
            boolean isAnimal = target instanceof net.minecraft.world.entity.animal.Animal || target instanceof net.minecraft.world.entity.ambient.AmbientCreature;
            boolean isItem = target instanceof net.minecraft.world.entity.item.ItemEntity;

            String ownerName = (mobOwnerEnabled && target instanceof LivingEntity living) ? MobOwner.getOwnerName(living) : null;
            boolean hasOwner = ownerName != null;

            if (espEnabled) {
                renderESPBox(context, mc, target, boxX, y, boxW, boxH, isPlayer, isMonster, isAnimal, isItem);
            }

            boolean withinRange = dist <= ModuleManager.get(NameTags.class).range;
            boolean drawNametags = nameTagsEnabled && (target instanceof LivingEntity) && withinRange && Modules.get(NameTags.class).shouldDraw(target);
            if (drawNametags || hasOwner) {
                renderNametag(context, mc, target, bxF, anchorY, scale, totalW, totalH, armorRowY, mainRowY, ownerRowY, textYOff, mainRowW, armorRowW, drawNametags, hasOwner, ownerName);
            }
        }
    }

    private void renderFallbackLayout(GuiGraphics context, MinecraftWrapper mc, List<Entity> candidates, Vec3 cameraPos, Vec3 cameraLook, Quaternionf cRotation, float pt, int guiWidth, int guiHeight, boolean espEnabled, boolean nameTagsEnabled, boolean mobOwnerEnabled) {
        Matrix4f worldProj = ravex.manager.ShaderManager.INSTANCE.getWorldProjectionMatrix();
        for (Entity target : candidates) {
            boolean isPlayer = target instanceof Player;
            boolean isMonster = target instanceof LivingEntity le && EntityUtility.isHostile(le);
            boolean isAnimal = target instanceof net.minecraft.world.entity.animal.Animal || target instanceof net.minecraft.world.entity.ambient.AmbientCreature;
            boolean isItem = target instanceof net.minecraft.world.entity.item.ItemEntity;

            Vec3 basePos = target.getPosition(pt);
            double dist = mc.getPlayer().getPosition(pt).distanceTo(basePos);
            float bbHeight = target.getBbHeight();
            float bbWidth = target.getBbWidth();
            Vec3 headPos = basePos.add(0, bbHeight, 0);
            Vec3 sidePos = basePos.add(bbWidth / 2.0f, bbHeight / 2.0f, 0);

            Vec3 baseProj = projectWorld(worldProj, basePos, cRotation, cameraPos, mc);
            Vec3 headProj = projectWorld(worldProj, headPos, cRotation, cameraPos, mc);
            Vec3 sideProj = projectWorld(worldProj, sidePos, cRotation, cameraPos, mc);
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

            float bxF = (float) sx_base;
            float byF = (float) sy_base;
            float hyF = (float) sy_head;
            float sxF = (float) sx_side;
            int boxH = Math.round(Math.abs(byF - hyF));
            int halfBoxW = Math.max(2, Math.round(Math.abs(sxF - bxF)));
            int boxW = halfBoxW * 2;
            int boxX = Math.round(bxF) - halfBoxW;
            float anchorY = Math.min(byF, hyF);
            int y = Math.round(anchorY);

            String ownerName = (mobOwnerEnabled && target instanceof LivingEntity living) ? MobOwner.getOwnerName(living) : null;
            boolean hasOwner = ownerName != null;

            if (espEnabled) {
                renderESPBox(context, mc, target, boxX, y, boxW, boxH, isPlayer, isMonster, isAnimal, isItem);
            }

            boolean withinRange = dist <= ModuleManager.get(NameTags.class).range;
            boolean drawNametags = nameTagsEnabled && (target instanceof LivingEntity) && withinRange && Modules.get(NameTags.class).shouldDraw(target);
            if (drawNametags || hasOwner) {
                LivingEntity livingTarget = (LivingEntity) target;
                String displayName = livingTarget.getDisplayName().getString();
                int health = (int) Math.ceil(livingTarget.getHealth());
                int maxHealth = (int) Math.ceil(livingTarget.getMaxHealth());
                String tagText = displayName + " §a" + health + "§7/§a" + maxHealth;
                double tw = drawNametags ? (ModuleManager.get(NameTags.class).customFont ? FontRenderUtility.getStringWidth(tagText) : mc.getFont().width(tagText)) : 0.0;
                double ow = hasOwner ? (ModuleManager.get(NameTags.class).customFont ? FontRenderUtility.getStringWidth("Owner: " + ownerName) : mc.getFont().width("Owner: " + ownerName)) : 0.0;

                boolean showArmor = drawNametags && ModuleManager.get(NameTags.class).armor;
                boolean showHands = drawNametags && ModuleManager.get(NameTags.class).handItems;
                boolean hasMainHand = drawNametags && !livingTarget.getMainHandItem().isEmpty();
                boolean hasOffHand = drawNametags && !livingTarget.getOffhandItem().isEmpty();

                int armorCount = 0;
                if (showArmor) {
                    if (!livingTarget.getItemBySlot(EquipmentSlot.FEET).isEmpty()) armorCount++;
                    if (!livingTarget.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) armorCount++;
                    if (!livingTarget.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) armorCount++;
                    if (!livingTarget.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) armorCount++;
                }

                boolean alwaysShowSlots = (livingTarget instanceof net.minecraft.world.entity.player.Player)
                    || (livingTarget instanceof net.minecraft.world.entity.monster.zombie.Zombie)
                    || (livingTarget instanceof net.minecraft.world.entity.monster.skeleton.AbstractSkeleton)
                    || (livingTarget instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin);

                double[] layout = NameTags.calculateLayout(dist, ModuleManager.get(NameTags.class).size, Modules.get(NameTags.class).distScale, showArmor, showHands, hasOwner, tw, ow, hasMainHand, hasOffHand, armorCount, alwaysShowSlots);

                double scale = layout[0], totalW = layout[1], totalH = layout[2];
                double armorRowY = layout[3], mainRowY = layout[4], ownerRowY = layout[5];
                double textYOff = layout[6], mainRowW = layout[7], armorRowW = layout[8];

                renderNametagAt(context, mc, bxF, anchorY, scale, totalW, totalH, armorRowY, mainRowY, ownerRowY, textYOff, mainRowW, armorRowW, drawNametags, hasOwner, livingTarget, ownerName, tagText, tw, ow, showArmor, showHands, armorCount, hasMainHand, hasOffHand);
            }
        }
    }

    private Vec3 projectWorld(Matrix4f worldProj, Vec3 pos, Quaternionf cRotation, Vec3 cameraPos, MinecraftWrapper mc) {
        if (worldProj == null) {
            return mc.getGameRenderer().projectPointToScreen(pos);
        }
        Quaternionf conjugate = new Quaternionf(cRotation).conjugate();
        Matrix4f matrix = new Matrix4f(worldProj).mul(new Matrix4f().rotation(conjugate));
        Vec3 rel = pos.subtract(cameraPos);
        org.joml.Vector3f projected = matrix.transformProject(new org.joml.Vector3f((float) rel.x, (float) rel.y, (float) rel.z));
        return new Vec3(projected.x, projected.y, projected.z);
    }

    private void renderESPBox(GuiGraphics context, MinecraftWrapper mc, Entity target, int boxX, int y, int boxW, int boxH, boolean isPlayer, boolean isMonster, boolean isAnimal, boolean isItem) {
        String mode = ModuleManager.get(ESP.class).mode;
        int espColor = isPlayer ? ModuleManager.get(ESP.class).playerColor
            : (isMonster ? ModuleManager.get(ESP.class).mobColor
            : (isAnimal ? ModuleManager.get(ESP.class).animalColor
            : (isItem ? ModuleManager.get(ESP.class).itemColor
            : ModuleManager.get(ESP.class).frameColor)));

        if ("Box2D".equals(mode)) {
            Render2DUtility.drawBorder(context, boxX, y, boxW, boxH, 1, espColor);
        }
    }

    private void renderNametag(GuiGraphics context, MinecraftWrapper mc, Entity target, float bx, float y, double scale, double totalW, double totalH, double armorRowY, double mainRowY, double ownerRowY, double textYOff, double mainRowW, double armorRowW, boolean drawNametags, boolean hasOwner, String ownerName) {
        LivingEntity livingTarget = (LivingEntity) target;
        String displayName = livingTarget.getDisplayName().getString();
        int health = (int) Math.ceil(livingTarget.getHealth());
        int maxHealth = (int) Math.ceil(livingTarget.getMaxHealth());
        String tagText = displayName + " §a" + health + "§7/§a" + maxHealth;
        double tw = drawNametags ? (ModuleManager.get(NameTags.class).customFont ? FontRenderUtility.getStringWidth(tagText) : mc.getFont().width(tagText)) : 0.0;
        double ow = hasOwner ? (ModuleManager.get(NameTags.class).customFont ? FontRenderUtility.getStringWidth("Owner: " + ownerName) : mc.getFont().width("Owner: " + ownerName)) : 0.0;
        boolean showArmor = drawNametags && ModuleManager.get(NameTags.class).armor;
        boolean showHands = drawNametags && ModuleManager.get(NameTags.class).handItems;
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

    private void renderNametagAt(GuiGraphics context, MinecraftWrapper mc, float bx, float y, double scale, double totalW, double totalH, double armorRowY, double mainRowY, double ownerRowY, double textYOff, double mainRowW, double armorRowW, boolean drawNametags, boolean hasOwner, LivingEntity livingTarget, String ownerName, String tagText, double tw, double ow, boolean showArmor, boolean showHands, int armorCount, boolean hasMainHand, boolean hasOffHand) {
        context.pose().pushMatrix();
        context.pose().translate(bx, y);
        context.pose().scale((float) scale, (float) scale);

        boolean bgEnabled = drawNametags ? ModuleManager.get(NameTags.class).background : (hasOwner && ModuleManager.get(MobOwner.class).background);
        if (bgEnabled) {
            int bgColor = drawNametags ? ModuleManager.get(NameTags.class).backgroundColor : 0x7005050A;
            int bgLeft = (int)(-totalW / 2.0 - 4.0);
            int bgRight = (int)(totalW / 2.0 + 4.0);
            int bgBottom = -1;
            int bgTop = (int)(-2.0 - totalH);
            int bgW = bgRight - bgLeft;
            int bgH = bgBottom - bgTop;

            Render2DUtility.drawRound(context, bgLeft, bgTop, bgW, bgH, 4, bgColor);

            boolean drawBorder = false;
            int borderCol = 0;
            if (drawNametags) {
                drawBorder = true;
                int activeColor = ravex.utility.render.ColorUtility.getActiveColor();
                borderCol = ravex.utility.render.ColorUtility.withAlpha(activeColor, 120);
            } else if (hasOwner) {
                drawBorder = ModuleManager.get(MobOwner.class).background;
                borderCol = 0x44FFAA00;
            }
            if (drawBorder) {
                Render2DUtility.drawRoundBorder(context, bgLeft, bgTop, bgW, bgH, 4, 1, borderCol);
            }
        }

        boolean alwaysShowSlots = (livingTarget instanceof net.minecraft.world.entity.player.Player)
            || (livingTarget instanceof net.minecraft.world.entity.monster.zombie.Zombie)
            || (livingTarget instanceof net.minecraft.world.entity.monster.skeleton.AbstractSkeleton)
            || (livingTarget instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin);

        int topItemsCount = 0;
        if (alwaysShowSlots) {
            if (showHands) topItemsCount += 2;
            if (showArmor) topItemsCount += 4;
        } else {
            if (showHands && hasMainHand) topItemsCount++;
            if (showArmor) {
                if (!livingTarget.getItemBySlot(EquipmentSlot.FEET).isEmpty()) topItemsCount++;
                if (!livingTarget.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) topItemsCount++;
                if (!livingTarget.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) topItemsCount++;
                if (!livingTarget.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) topItemsCount++;
            }
            if (showHands && hasOffHand) topItemsCount++;
        }

        if (topItemsCount > 0) {
            int ax = (int)(-armorRowW / 2.0);
            int ay = (int) armorRowY;

            if (showHands) {
                ItemStack stack = livingTarget.getMainHandItem();
                if (alwaysShowSlots || !stack.isEmpty()) {
                    Render2DUtility.drawRound(context, ax, ay, 18, 18, 3, 0x15FFFFFF);
                    Render2DUtility.drawRoundBorder(context, ax, ay, 18, 18, 3, 0.5f, 0x20FFFFFF);
                    if (!stack.isEmpty()) {
                        context.pose().pushMatrix();
                        context.pose().translate((float)(ax + 1), (float)(ay + 1));
                        context.renderItem(stack, 0, 0);
                        context.renderItemDecorations(mc.getFont(), stack, 0, 0);
                        context.pose().popMatrix();
                    }
                    ax += 18 + 3;
                }
            }
            if (showArmor) {
                EquipmentSlot[] armorItems = new EquipmentSlot[]{
                    EquipmentSlot.FEET,
                    EquipmentSlot.LEGS,
                    EquipmentSlot.CHEST,
                    EquipmentSlot.HEAD
                };
                for (EquipmentSlot slot : armorItems) {
                    ItemStack stack = livingTarget.getItemBySlot(slot);
                    if (alwaysShowSlots || !stack.isEmpty()) {
                        Render2DUtility.drawRound(context, ax, ay, 18, 18, 3, 0x15FFFFFF);
                        Render2DUtility.drawRoundBorder(context, ax, ay, 18, 18, 3, 0.5f, 0x20FFFFFF);
                        if (!stack.isEmpty()) {
                            context.pose().pushMatrix();
                            context.pose().translate((float)(ax + 1), (float)(ay + 1));
                            context.renderItem(stack, 0, 0);
                            context.renderItemDecorations(mc.getFont(), stack, 0, 0);
                            context.pose().popMatrix();
                        }
                        ax += 18 + 3;
                    }
                }
            }
            if (showHands) {
                ItemStack stack = livingTarget.getOffhandItem();
                if (alwaysShowSlots || !stack.isEmpty()) {
                    Render2DUtility.drawRound(context, ax, ay, 18, 18, 3, 0x15FFFFFF);
                    Render2DUtility.drawRoundBorder(context, ax, ay, 18, 18, 3, 0.5f, 0x20FFFFFF);
                    if (!stack.isEmpty()) {
                        context.pose().pushMatrix();
                        context.pose().translate((float)(ax + 1), (float)(ay + 1));
                        context.renderItem(stack, 0, 0);
                        context.renderItemDecorations(mc.getFont(), stack, 0, 0);
                        context.pose().popMatrix();
                    }
                }
            }
        }

        if (drawNametags) {
            int rx = (int)(-tw / 2.0);
            int mY = (int) mainRowY;
            if (ModuleManager.get(NameTags.class).customFont) {
                FontRenderUtility.drawString(context, tagText, rx, mY, 0xFFFFFFFF, false);
            } else {
                context.drawString(mc.getFont(), tagText, rx, mY, 0xFFFFFFFF, false);
            }
        }

        if (hasOwner) {
            int otx = (int)(-ow / 2.0);
            int oty = (int) ownerRowY;
            if (ModuleManager.get(NameTags.class).customFont) {
                FontRenderUtility.drawString(context, "Owner: " + ownerName, otx, oty, ModuleManager.get(MobOwner.class).textColor, false);
            } else {
                context.drawString(mc.getFont(), "Owner: " + ownerName, otx, oty, ModuleManager.get(MobOwner.class).textColor, false);
            }
        }

        context.pose().popMatrix();
    }

    private void renderDamageLabels(GuiGraphics context, MinecraftWrapper mc, float pt, Vec3 cameraPos, Vec3 cameraLook, int guiWidth, int guiHeight) {
        long now = System.currentTimeMillis();
        if (lastLabelTime == 0) lastLabelTime = now;
        float delta = (now - lastLabelTime) / 1000f;
        lastLabelTime = now;
        if (delta > 0.1f) delta = 0.016f;

        BasePlace bp = Modules.get(BasePlace.class);
        boolean bpActive = Modules.enabled(BasePlace.class) && BasePlace.getSimulatedPlacementBlock() != null;
        if (bpActive) {
            BlockPos p = BasePlace.getSimulatedPlacementBlock();
            double tx = p.getX() + 0.5, ty = p.getY() + 1.2, tz = p.getZ() + 0.5;
            if (bpAlpha <= 0.01f) {
                bpAnimX = tx; bpAnimY = ty; bpAnimZ = tz;
            } else {
                bpAnimX += (tx - bpAnimX) * Math.min(1.0, delta * 12.0);
                bpAnimY += (ty - bpAnimY) * Math.min(1.0, delta * 12.0);
                bpAnimZ += (tz - bpAnimZ) * Math.min(1.0, delta * 12.0);
            }
            bpAlpha = Math.min(1.0f, bpAlpha + delta * 6.0f);
        } else {
            bpAlpha = Math.max(0.0f, bpAlpha - delta * 6.0f);
        }
        if (bpAlpha > 0.01f) {
            Vec3 pos3d = new Vec3(bpAnimX, bpAnimY, bpAnimZ);
            Vec3 proj = mc.getGameRenderer().projectPointToScreen(pos3d);
            if (proj != null) {
                Vec3 dir = pos3d.subtract(cameraPos).normalize();
                Vec3 look = mc.getPlayer().getViewVector(pt);
                if (dir.dot(look) > 0.0) {
                    double sx = (proj.x + 1.0) / 2.0 * guiWidth;
                    double sy = (1.0 - proj.y) / 2.0 * guiHeight;
                    renderModernDamageCard(context, mc, (int) sx, (int) sy, bpAlpha, "BASE", "§a", BasePlace.currentTargetDamage, BasePlace.currentSelfDamage, 0, 0xFF00FF88);
                }
            }
        }

        AnchorAura aa = Modules.get(AnchorAura.class);
        boolean aaActive = Modules.enabled(AnchorAura.class) && AnchorAura.simulatedPlacementBlock != null;
        if (aaActive) {
            BlockPos p = AnchorAura.simulatedPlacementBlock;
            double tx = p.getX() + 0.5, ty = p.getY() + 1.2, tz = p.getZ() + 0.5;
            if (aaAlpha <= 0.01f) {
                aaAnimX = tx; aaAnimY = ty; aaAnimZ = tz;
            } else {
                aaAnimX += (tx - aaAnimX) * Math.min(1.0, delta * 12.0);
                aaAnimY += (ty - aaAnimY) * Math.min(1.0, delta * 12.0);
                aaAnimZ += (tz - aaAnimZ) * Math.min(1.0, delta * 12.0);
            }
            aaAlpha = Math.min(1.0f, aaAlpha + delta * 6.0f);
        } else {
            aaAlpha = Math.max(0.0f, aaAlpha - delta * 6.0f);
        }
        if (aaAlpha > 0.01f) {
            Vec3 pos3d = new Vec3(aaAnimX, aaAnimY, aaAnimZ);
            Vec3 proj = mc.getGameRenderer().projectPointToScreen(pos3d);
            if (proj != null) {
                Vec3 dir = pos3d.subtract(cameraPos).normalize();
                Vec3 look = mc.getPlayer().getViewVector(pt);
                if (dir.dot(look) > 0.0) {
                    double sx = (proj.x + 1.0) / 2.0 * guiWidth;
                    double sy = (1.0 - proj.y) / 2.0 * guiHeight;
                    renderModernDamageCard(context, mc, (int) sx, (int) sy, aaAlpha, "ANCHOR", "§d", AnchorAura.currentTargetDamage, AnchorAura.currentSelfDamage, 0, 0xFFFF00CC);
                }
            }
        }

        AutoCrystal ac = Modules.get(AutoCrystal.class);
        boolean acActive = Modules.enabled(AutoCrystal.class) && ac.renderDamage && AutoCrystal.currentPlacementBlock != null;
        if (acActive) {
            BlockPos p = AutoCrystal.currentPlacementBlock;
            double tx = p.getX() + 0.5, ty = p.getY() + 1.2, tz = p.getZ() + 0.5;
            if (acAlpha <= 0.01f) {
                acAnimX = tx; acAnimY = ty; acAnimZ = tz;
            } else {
                acAnimX += (tx - acAnimX) * Math.min(1.0, delta * 12.0);
                acAnimY += (ty - acAnimY) * Math.min(1.0, delta * 12.0);
                acAnimZ += (tz - acAnimZ) * Math.min(1.0, delta * 12.0);
            }
            acAlpha = Math.min(1.0f, acAlpha + delta * 6.0f);
        } else {
            acAlpha = Math.max(0.0f, acAlpha - delta * 6.0f);
        }
        if (acAlpha > 0.01f) {
            Vec3 pos3d = new Vec3(acAnimX, acAnimY, acAnimZ);
            Vec3 proj = mc.getGameRenderer().projectPointToScreen(pos3d);
            if (proj != null) {
                Vec3 dir = pos3d.subtract(cameraPos).normalize();
                Vec3 look = mc.getPlayer().getViewVector(pt);
                if (dir.dot(look) > 0.0) {
                    double sx = (proj.x + 1.0) / 2.0 * guiWidth;
                    double sy = (1.0 - proj.y) / 2.0 * guiHeight;
                    renderModernDamageCard(context, mc, (int) sx, (int) sy, acAlpha, "CRYSTAL", "§b", AutoCrystal.currentTargetDamage, AutoCrystal.currentSelfDamage, AutoCrystal.currentTargetTotems, 0xFF00DDFF);
                }
            }
        }

        AutoCrystal acModule = Modules.get(AutoCrystal.class);
        boolean rotActive = Modules.enabled(AutoCrystal.class) && acModule.visualRotate && AutoCrystal.currentRotationTarget != null;
        if (rotActive) {
            Vec3 target = AutoCrystal.currentRotationTarget;
            if (rotAlpha <= 0.01f) {
                rotAnimX = target.x; rotAnimY = target.y; rotAnimZ = target.z;
            } else {
                rotAnimX += (target.x - rotAnimX) * Math.min(1.0, delta * 16.0);
                rotAnimY += (target.y - rotAnimY) * Math.min(1.0, delta * 16.0);
                rotAnimZ += (target.z - rotAnimZ) * Math.min(1.0, delta * 16.0);
            }
            rotAlpha = Math.min(1.0f, rotAlpha + delta * 8.0f);
        } else {
            rotAlpha = Math.max(0.0f, rotAlpha - delta * 8.0f);
        }
        if (rotAlpha > 0.01f) {
            Vec3 pos3d = new Vec3(rotAnimX, rotAnimY, rotAnimZ);
            Vec3 proj = mc.getGameRenderer().projectPointToScreen(pos3d);
            if (proj != null) {
                Vec3 dir = pos3d.subtract(cameraPos).normalize();
                Vec3 look = mc.getPlayer().getViewVector(pt);
                if (dir.dot(look) > 0.0) {
                    double sx = (proj.x + 1.0) / 2.0 * guiWidth;
                    double sy = (1.0 - proj.y) / 2.0 * guiHeight;
                    renderRotationReticle(context, (float) sx, (float) sy, rotAlpha);
                }
            }
        }

        PVEUtils asm = Modules.get(PVEUtils.class);
        if (Modules.enabled(PVEUtils.class) && asm.mode.equals("AutoSmelt") && asm.smeltRender && PVEUtils.smeltTarget != null) {
            BlockPos p = PVEUtils.smeltTarget;
            Vec3 pos3d = new Vec3(p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5);
            Vec3 proj = mc.getGameRenderer().projectPointToScreen(pos3d);
            if (proj != null) {
                Vec3 dir = pos3d.subtract(cameraPos).normalize();
                Vec3 look = mc.getPlayer().getViewVector(pt);
                if (dir.dot(look) > 0.0) {
                    double sx = (proj.x + 1.0) / 2.0 * guiWidth;
                    double sy = (1.0 - proj.y) / 2.0 * guiHeight;
                    int x = (int) sx, y = (int) sy;
                    String statusText;
                    if (mc.getPlayer().containerMenu instanceof net.minecraft.world.inventory.AbstractFurnaceMenu furnace) {
                        var result = furnace.getSlot(2).getItem();
                        var input = furnace.getSlot(0).getItem();
                        if (!result.isEmpty()) statusText = "§fResult: §e" + result.getHoverName().getString() + " §7x" + result.getCount();
                        else if (!input.isEmpty()) statusText = "§7Smelting: §f" + input.getHoverName().getString() + " §7(" + (int)(furnace.getBurnProgress() * 100) + "%)";
                        else statusText = "§7Idle";
                    } else statusText = "§7Furnace";
                    FontRenderUtility.drawString(context, statusText, x - mc.getFont().width(statusText) / 2, y - 4, 0xFFFFFFFF, true);
                }
            }
        }

        if (Modules.enabled(PVEUtils.class) && asm.mode.equals("AutoBrew") && asm.brewRender) {
            BlockPos p = PVEUtils.getBrewTarget();
            if (p != null) {
                Vec3 pos3d = new Vec3(p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5);
                Vec3 proj = mc.getGameRenderer().projectPointToScreen(pos3d);
                if (proj != null) {
                    Vec3 dir = pos3d.subtract(cameraPos).normalize();
                    Vec3 look = mc.getPlayer().getViewVector(pt);
                    if (dir.dot(look) > 0.0) {
                        double sx = (proj.x + 1.0) / 2.0 * guiWidth;
                        double sy = (1.0 - proj.y) / 2.0 * guiHeight;
                        int x = (int) sx, y = (int) sy;
                        String statusText;
                        if (mc.getPlayer().containerMenu instanceof net.minecraft.world.inventory.BrewingStandMenu brew) {
                            int ticks = brew.getBrewingTicks();
                            int fuel = brew.getFuel();
                            var ingr = brew.getSlot(3).getItem();
                            if (ticks > 0) statusText = "§dBrewing... §7(" + (400 - ticks) / 20 + "s)";
                            else if (!ingr.isEmpty()) statusText = "§dReady: §f" + ingr.getHoverName().getString();
                            else statusText = "§7Idle";
                            String fuelText = "§7Fuel: " + fuel;
                            String total = statusText + " | " + fuelText;
                            FontRenderUtility.drawString(context, total, x - mc.getFont().width(total) / 2, y - 4, 0xFFFFFFFF, true);
                        }
                    }
                }
            }
        }
    }

    private void renderModernDamageCard(GuiGraphics context, MinecraftWrapper mc, int x, int y, float alpha, String title, String colorPrefix, double targetDmg, double selfDmg, int totems, int accentColor) {
        if (alpha <= 0.01f) return;

        String line1 = colorPrefix + title + " §8• §a" + String.format("%.1f", targetDmg) + " §7dmg";
        String line2 = "§7Self: §c" + String.format("%.1f", selfDmg) + (totems > 0 ? " §8• §e" + totems + " §7pop" : "");

        int w1 = FontRenderUtility.getStringWidth(line1);
        int w2 = FontRenderUtility.getStringWidth(line2);
        int maxTextWidth = Math.max(w1, w2);
        int fontH = FontRenderUtility.getFontHeight();
        if (fontH < 9) fontH = 9;

        int padX = 8;
        int padY = 5;
        int lineGap = 3;

        int w = maxTextWidth + padX * 2;
        int h = fontH * 2 + lineGap + padY * 2;

        float scale = 0.90f + 0.10f * alpha;
        context.pose().pushMatrix();
        context.pose().translate((float) x, (float) y);
        context.pose().scale(scale, scale);
        context.pose().translate((float) -x, (float) -y);

        int left = x - w / 2;
        int top = y - h / 2;

        int bgAlpha = (int) (185 * alpha);
        int bgColor = ColorUtility.setAlpha(0x0C0D14, bgAlpha);
        int borderColor = ColorUtility.withAlpha(accentColor, (int) (140 * alpha));

        Render2DUtility.drawRoundedRectWithBorder(context, left, top, w, h, 4, bgColor, borderColor, 1);

        int textAlpha = (int) (255 * alpha);
        int textSubAlpha = (int) (220 * alpha);

        int line1Y = top + padY;
        int line2Y = top + padY + fontH + lineGap;

        FontRenderUtility.drawString(context, line1, left + padX, line1Y, ColorUtility.withAlpha(0xFFFFFFFF, textAlpha), false);
        FontRenderUtility.drawString(context, line2, left + padX, line2Y, ColorUtility.withAlpha(0xFFFFFFFF, textSubAlpha), false);

        context.pose().popMatrix();
    }

    private void renderRotationReticle(GuiGraphics context, float x, float y, float alpha) {
        if (alpha <= 0.01f) return;
        int ix = Math.round(x);
        int iy = Math.round(y);
        int ringColor = ColorUtility.withAlpha(0xFF00DDFF, (int) (160 * alpha));
        int dotColor = ColorUtility.withAlpha(0xFFFFFFFF, (int) (240 * alpha));
        int tickColor = ColorUtility.withAlpha(0xFF00DDFF, (int) (220 * alpha));

        Render2DUtility.drawRoundBorder(context, ix - 5, iy - 5, 10, 10, 3, 1, ringColor);
        context.fill(ix - 1, iy - 1, ix + 1, iy + 1, dotColor);

        context.fill(ix - 8, iy, ix - 5, iy + 1, tickColor);
        context.fill(ix + 6, iy, ix + 9, iy + 1, tickColor);
        context.fill(ix, iy - 8, ix + 1, iy - 5, tickColor);
        context.fill(ix, iy + 6, ix + 1, iy + 9, tickColor);
    }

    private void renderPacketMine(GuiGraphics context, MinecraftWrapper mc) {
        PacketMine pm = Modules.get(PacketMine.class);
        if (Modules.enabled(PacketMine.class) && pm.render) {
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
                    FontRenderUtility.drawString(context, text, x - mc.getFont().width(text) / 2, y - 4, 0xFFFFFFFF, true);
                }
            }
        }
    }

    private void renderWaypoints(GuiGraphics context, MinecraftWrapper mc) {
        if (Modules.enabled(Waypoint.class)) {
            int wpColor = ModuleManager.get(Waypoint.class).color;
            String currentDim = mc.getLevel() != null ? mc.getLevel().dimension().identifier().toString() : null;
            boolean showDist = ModuleManager.get(Waypoint.class).showDistance;
            boolean showNm = ModuleManager.get(Waypoint.class).showName;
            for (var wp : Waypoint.getWaypoints()) {
                if (currentDim != null && !wp.dimension().equals(currentDim)) continue;
                Vec3 pos3d = new Vec3(wp.x() + 0.5, wp.y() + 1.5, wp.z() + 0.5);
                Vec3 proj = mc.getGameRenderer().projectPointToScreen(pos3d);
                if (proj != null && proj.z > 0.0) {
                    double sx = (proj.x + 1.0) / 2.0 * context.guiWidth();
                    double sy = (1.0 - proj.y) / 2.0 * context.guiHeight();
                    int ix = (int) sx, iy = (int) sy;
                    double dist = Math.sqrt(mc.getPlayer().distanceToSqr(wp.x(), wp.y(), wp.z()));
                    String text = showNm ? wp.name() : "";
                    if (showDist) text += (text.isEmpty() ? "" : " ") + "§7" + (int)dist + "m";
                    if (text.isEmpty()) text = wp.name();
                    int tw = mc.getFont().width(text);
                    int th = mc.getFont().lineHeight;
                    int pad = 3;
                    context.pose().pushMatrix();
                    context.pose().translate(ix, iy);
                    context.fill(-tw / 2 - pad, -th / 2 - pad, tw / 2 + pad, th / 2 + pad, 0xAA000000);
                    FontRenderUtility.drawString(context, text, -tw / 2, -th / 2, wpColor, false);
                    context.pose().popMatrix();
                }
            }
        }
    }

    private void renderHud(GuiGraphics context, DeltaTracker tickCounter) {
        List<Module> hudModules = ModuleManager.INSTANCE.getHudModules();
        GuiOptimizerUtility.optimizeHudAnimations(hudModules);

        Module dragHud = Hud.draggingHud;
        if (dragHud != null) {
            var mc = MinecraftWrapper.getWrapper();
            double mx = mc.getRaw().mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getWidth();
            double my = mc.getRaw().mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getHeight();
            int nx = (int)mx - Hud.dragOffX;
            int ny = (int)my - Hud.dragOffY;
            nx = Math.max(0, Math.min(mc.getWindow().getGuiScaledWidth() - dragHud.getWidth(), nx));
            ny = Math.max(0, Math.min(mc.getWindow().getGuiScaledHeight() - dragHud.getHeight(), ny));
            dragHud.setX(nx);
            dragHud.setY(ny);
            dragHud.setDisplayX(nx);
            dragHud.setDisplayY(ny);
            dragHud.setHudPositionCustomized(true);
        }

        for (Module hud : hudModules) {
            float animProgress = hud.getHudAnimProgress();
            if (!hud.getEnabled() && animProgress < 0.005f) continue;

            float userScale = hud.getUserScale();
            float entryScale = hud.getHudScale();
            float entryAlpha = hud.getHudAlpha();

            int cx = hud.getX() + Math.round(hud.getWidth() * userScale / 2f);
            int cy = hud.getY() + Math.round(hud.getHeight() * userScale / 2f);

            var pose = context.pose();
            pose.pushMatrix();
            pose.translate(hud.getX(), hud.getY());
            pose.scale(userScale, userScale);
            pose.translate(-hud.getX(), -hud.getY());

            if (entryScale < 0.999f || entryScale > 1.001f) {
                float slideY = (1f - entryAlpha) * 12f;
                pose.translate(cx, cy - slideY);
                pose.scale(entryScale, entryScale);
                pose.translate(-cx, -cy + slideY);
            }

            try { hud.render(context, tickCounter.getGameTimeDeltaTicks()); } catch (Throwable ignored) {}

            pose.popMatrix();
        }
    }

    private Vec3 projectPointToScreenUnbobbed(Vec3 pos) {
        var mc = MinecraftWrapper.getWrapper();
        net.minecraft.client.Camera camera = mc.getGameRenderer().getMainCamera();
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
