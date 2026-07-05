package ravex.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.modules.render.Ambient;
import ravex.modules.render.ESP;
import ravex.modules.render.NameTags;
import ravex.modules.player.MobOwner;
import net.minecraft.core.BlockPos;

@Mixin(Gui.class)
public abstract class MixinInGameHUD {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        if (Ambient.INSTANCE.getEnabled()) {
            int rVal = Ambient.INSTANCE.r.getValue().intValue();
            int gVal = Ambient.INSTANCE.g.getValue().intValue();
            int bVal = Ambient.INSTANCE.b.getValue().intValue();
            int aVal = Ambient.INSTANCE.a.getValue().intValue();
            int color = ((aVal & 0xFF) << 24) | ((rVal & 0xFF) << 16) | ((gVal & 0xFF) << 8) | (bVal & 0xFF);
            context.fill(0, 0, context.guiWidth(), context.guiHeight(), color);
        }

        if (mc.level == null || mc.player == null) {
            renderHud(context, tickCounter);
            return;
        }

        float pt = tickCounter.getGameTimeDeltaTicks();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        boolean espEnabled = ESP.INSTANCE.getEnabled();
        boolean nameTagsEnabled = NameTags.INSTANCE.getEnabled();
        boolean mobOwnerEnabled = MobOwner.INSTANCE.getEnabled() && MobOwner.INSTANCE.animals.getValue();

        int guiWidth = context.guiWidth();
        int guiHeight = context.guiHeight();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        Vec3 playerViewVec = mc.player != null ? mc.player.getViewVector(pt) : null;
        org.joml.Quaternionf cRotation = mc.gameRenderer.getMainCamera().rotation();
        org.joml.Vector3f lookVec = new org.joml.Vector3f(0.0F, 0.0F, -1.0F).rotate(cRotation);
        Vec3 cameraLook = new Vec3(lookVec.x(), lookVec.y(), lookVec.z());
        boolean tracersEnabled = ravex.modules.render.Tracers.INSTANCE.getEnabled();

        java.util.List<Entity> candidates = new java.util.ArrayList<>();
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

            double maxDist = ESP.INSTANCE.maxDistance.getValue();
            if (nameTagsEnabled) {
                maxDist = Math.max(maxDist, NameTags.INSTANCE.range.getValue());
            }
            if (tracersEnabled) {
                maxDist = Math.max(maxDist, ravex.modules.render.Tracers.INSTANCE.maxDistance.getValue());
            }
            if (dist > maxDist) continue;

            
            if (firstPerson && dist < 1.2 && !nameTagsEnabled) continue;

            boolean isPlayer = target instanceof Player;
            boolean isMonster = target instanceof LivingEntity le && ravex.utility.misc.MobUtility.isHostile(le);
            boolean isAnimal = target instanceof net.minecraft.world.entity.animal.Animal || 
                                target instanceof net.minecraft.world.entity.ambient.AmbientCreature;
            boolean isItem = target instanceof net.minecraft.world.entity.item.ItemEntity;
            boolean isFrame = target instanceof net.minecraft.world.entity.decoration.ItemFrame;

            if (isPlayer && !ESP.INSTANCE.players.getValue() && !(tracersEnabled && ravex.modules.render.Tracers.INSTANCE.players.getValue())) continue;
            if (isMonster && !ESP.INSTANCE.monsters.getValue() && !(tracersEnabled && ravex.modules.render.Tracers.INSTANCE.monsters.getValue())) continue;
            if (isAnimal && !ESP.INSTANCE.animals.getValue() && !(tracersEnabled && ravex.modules.render.Tracers.INSTANCE.animals.getValue())) continue;
            if (isItem && !ESP.INSTANCE.items.getValue() && !(tracersEnabled && ravex.modules.render.Tracers.INSTANCE.items.getValue())) continue;
            if (isFrame && !ESP.INSTANCE.frames.getValue()) continue;

            if (!isPlayer && !isMonster && !isAnimal && !isItem && !isFrame) continue;

            candidates.add(target);
        }

        
        if (tracersEnabled) {
            ravex.modules.render.Tracers tracers = ravex.modules.render.Tracers.INSTANCE;

            java.util.List<Entity> tracerEntities = new java.util.ArrayList<>();
            java.util.List<Integer> tracerColors = new java.util.ArrayList<>();

            for (Entity target : candidates) {
                boolean isPlayer = target instanceof Player;
                boolean isMonster = target instanceof LivingEntity le && ravex.utility.misc.MobUtility.isHostile(le);
                boolean isAnimal = target instanceof net.minecraft.world.entity.animal.Animal ||
                                   target instanceof net.minecraft.world.entity.ambient.AmbientCreature;
                boolean isItem = target instanceof net.minecraft.world.entity.item.ItemEntity;

                boolean show = false;
                int color = 0;
                if (isPlayer && tracers.players.getValue()) {
                    show = true;
                    color = tracers.playerColor.getValue();
                } else if (isMonster && tracers.monsters.getValue()) {
                    show = true;
                    color = tracers.mobColor.getValue();
                } else if (isAnimal && tracers.animals.getValue()) {
                    show = true;
                    color = tracers.animalColor.getValue();
                } else if (isItem && tracers.items.getValue()) {
                    show = true;
                    color = tracers.itemColor.getValue();
                }

                if (show) {
                    tracerEntities.add(target);
                    tracerColors.add(color);
                }
            }

            String mode = tracers.mode.getValue();

            if (mode.equals("Arrows")) {
                ravex.modules.render.Tracers.renderArrows(context, tracerEntities, tracerColors, pt, cameraPos, cameraLook, guiWidth, guiHeight);
            } else {
                float width = tracers.lineWidth.getValue().floatValue();
                int tracerCount = tracerEntities.size();

                for (int i = 0; i < tracerCount; i++) {
                    Entity target = tracerEntities.get(i);
                    int color = tracerColors.get(i);
                    Vec3 basePos = target.getPosition(pt);
                    float bbHeight = target.getBbHeight();
                    Vec3 headPos = basePos.add(0, bbHeight, 0);
                    Vec3 baseProjUnbobbed = projectPointToScreenUnbobbed(basePos);
                    Vec3 headProjUnbobbed = projectPointToScreenUnbobbed(headPos);
                    if (baseProjUnbobbed != null && headProjUnbobbed != null) {
                        double cx = guiWidth / 2.0;
                        double cy = guiHeight / 2.0;
                        Vec3 toEntity = basePos.subtract(cameraPos);
                        boolean isBehind = cameraLook.dot(toEntity) < 0;

                        double ex = (baseProjUnbobbed.x + 1.0) / 2.0 * guiWidth;
                        double ey_base = (1.0 - baseProjUnbobbed.y) / 2.0 * guiHeight;
                        double ey_head = (1.0 - headProjUnbobbed.y) / 2.0 * guiHeight;
                        double ey = (ey_base + ey_head) / 2.0;

                        if (isBehind) {
                            ex = guiWidth - ex;
                            ey = guiHeight - ey;
                        }
                        boolean isOffscreen = isBehind || ex < 0 || ex > guiWidth || ey < 0 || ey > guiHeight;

                        if (isOffscreen) {
                            double dx = ex - cx;
                            double dy = ey - cy;
                            double tX = Double.MAX_VALUE;
                            double tY = Double.MAX_VALUE;
                            double borderPadding = 2.0;

                            if (dx > 0) {
                                tX = (guiWidth - borderPadding - cx) / dx;
                            } else if (dx < 0) {
                                tX = (borderPadding - cx) / dx;
                            }

                            if (dy > 0) {
                                tY = (guiHeight - borderPadding - cy) / dy;
                            } else if (dy < 0) {
                                tY = (borderPadding - cy) / dy;
                            }

                            double t = Math.min(tX, tY);
                            if (t > 0 && t < 1.0) {
                                ex = cx + t * dx;
                                ey = cy + t * dy;
                            } else {
                                double len = Math.sqrt(dx * dx + dy * dy);
                                if (len > 0) {
                                    ex = cx + (dx / len) * (cx - borderPadding);
                                    ey = cy + (dy / len) * (cy - borderPadding);
                                }
                            }
                        }
                        drawTracerLine2D(context, (float) cx, (float) cy, (float) ex, (float) ey, color, width);
                    }
                }
            }
        }

        
        int count = candidates.size();
        boolean nativeSuccess = false;
        int renderedCount = 0;
        double[] outLayouts = null;
        int[] outIndices = null;

        if (false && count > 0 && ravex.utility.nativelib.NativeLoader.isNativeAvailable()) {
            try {
                double[] cameraPosArr = new double[] { cameraPos.x, cameraPos.y, cameraPos.z };
                org.joml.Matrix4f projMatrix = ravex.manager.ShaderManager.INSTANCE.getProjectionMatrix();
                org.joml.Quaternionf cameraRotation = new org.joml.Quaternionf(mc.gameRenderer.getMainCamera().rotation());
                org.joml.Matrix4f mvMatrix = new org.joml.Matrix4f().rotation(cameraRotation.conjugate());

                float[] projectionArr = new float[16];
                float[] modelViewArr = new float[16];
                projMatrix.get(projectionArr);
                mvMatrix.get(modelViewArr);

                double[] playerViewVecArr = new double[] {
                    playerViewVec != null ? playerViewVec.x : 0.0,
                    playerViewVec != null ? playerViewVec.y : 0.0,
                    playerViewVec != null ? playerViewVec.z : 0.0
                };

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

                    positions[i * 9 + 0] = basePos.x;
                    positions[i * 9 + 1] = basePos.y;
                    positions[i * 9 + 2] = basePos.z;
                    positions[i * 9 + 3] = headPos.x;
                    positions[i * 9 + 4] = headPos.y;
                    positions[i * 9 + 5] = headPos.z;
                    positions[i * 9 + 6] = sidePos.x;
                    positions[i * 9 + 7] = sidePos.y;
                    positions[i * 9 + 8] = sidePos.z;

                    boolean drawNametags = nameTagsEnabled && (target instanceof LivingEntity);
                    String ownerName = (mobOwnerEnabled && target instanceof LivingEntity living) ? MobOwner.getOwnerName(living) : null;
                    boolean hasOwner = ownerName != null;

                    double tw = 0.0;
                    double ow = 0.0;
                    boolean showArmor = false;
                    boolean showHands = false;
                    boolean hasMainHand = false;
                    boolean hasOffHand = false;
                    int armorCnt = 0;

                    if (drawNametags || hasOwner) {
                        LivingEntity livingTarget = (LivingEntity) target;
                        if (drawNametags) {
                            String displayName = livingTarget.getDisplayName().getString();
                            int health = (int) Math.ceil(livingTarget.getHealth());
                            int maxHealth = (int) Math.ceil(livingTarget.getMaxHealth());
                            String tagText = displayName + " §a" + health + "§7/§a" + maxHealth;
                            tw = NameTags.INSTANCE.customFont.getValue() ? 
                                 ravex.utility.render.FontRenderUtility.getStringWidth(tagText) : 
                                 mc.font.width(tagText);
                            showArmor = NameTags.INSTANCE.armor.getValue();
                            showHands = NameTags.INSTANCE.handItems.getValue();
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
                            String ownerText = "Owner: " + ownerName;
                            ow = NameTags.INSTANCE.customFont.getValue() ? 
                                 ravex.utility.render.FontRenderUtility.getStringWidth(ownerText) : 
                                 mc.font.width(ownerText);
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

                renderedCount = ravex.utility.misc.GuiOptimizer.nativeOptimizeNameTags(
                    cameraPosArr,
                    modelViewArr,
                    projectionArr,
                    playerViewVecArr,
                    positions,
                    textWidths,
                    booleans,
                    armorCounts,
                    count,
                    NameTags.INSTANCE.scale.getValue(),
                    NameTags.INSTANCE.distanceScaling.getValue(),
                    Math.max(ESP.INSTANCE.maxDistance.getValue(), nameTagsEnabled ? NameTags.INSTANCE.range.getValue() : 0.0),
                    guiWidth,
                    guiHeight,
                    outLayouts,
                    outIndices
                );
                nativeSuccess = true;
            } catch (Throwable t) {
                nativeSuccess = false;
            }
        }

        if (nativeSuccess) {
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

                int bx = (int) sx_base;
                int by = (int) sy_base;
                int hy = (int) sy_head;
                int sx = (int) sx_side;

                int boxH = Math.abs(by - hy);
                int halfBoxW = Math.max(2, Math.abs(sx - bx));
                int boxW = halfBoxW * 2;
                int boxX = bx - halfBoxW;
                int y = Math.min(by, hy);

                boolean isPlayer = target instanceof Player;
                boolean isMonster = target instanceof LivingEntity le && ravex.utility.misc.MobUtility.isHostile(le);
                boolean isAnimal = target instanceof net.minecraft.world.entity.animal.Animal || 
                                   target instanceof net.minecraft.world.entity.ambient.AmbientCreature;
                boolean isItem = target instanceof net.minecraft.world.entity.item.ItemEntity;

                String ownerName = (mobOwnerEnabled && target instanceof LivingEntity living) ? MobOwner.getOwnerName(living) : null;
                boolean hasOwner = ownerName != null;

                if (espEnabled) {
                    String mode = ESP.INSTANCE.mode.getValue();
                    int espColor = isPlayer ? ESP.INSTANCE.playerColor.getValue() :
                                   (isMonster ? ESP.INSTANCE.mobColor.getValue() :
                                   (isAnimal ? ESP.INSTANCE.animalColor.getValue() :
                                   (isItem ? ESP.INSTANCE.itemColor.getValue() :
                                   ESP.INSTANCE.frameColor.getValue())));

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

                boolean withinRange = NameTags.isNativeAvailable() ? 
                    NameTags.nativeIsWithinRange(dist, NameTags.INSTANCE.range.getValue()) : 
                    (dist <= NameTags.INSTANCE.range.getValue());
                boolean drawNametags = nameTagsEnabled && (target instanceof LivingEntity) && withinRange;
                if (drawNametags || hasOwner) {
                    LivingEntity livingTarget = (LivingEntity) target;
                    String displayName = livingTarget.getDisplayName().getString();
                    int health = (int) Math.ceil(livingTarget.getHealth());
                    int maxHealth = (int) Math.ceil(livingTarget.getMaxHealth());
                    String tagText = displayName + " §a" + health + "§7/§a" + maxHealth;
                    double tw = drawNametags ? (NameTags.INSTANCE.customFont.getValue() ? ravex.utility.render.FontRenderUtility.getStringWidth(tagText) : mc.font.width(tagText)) : 0.0;
                    double ow = hasOwner ? (NameTags.INSTANCE.customFont.getValue() ? ravex.utility.render.FontRenderUtility.getStringWidth("Owner: " + ownerName) : mc.font.width("Owner: " + ownerName)) : 0.0;

                    boolean showArmor = drawNametags && NameTags.INSTANCE.armor.getValue();
                    boolean showHands = drawNametags && NameTags.INSTANCE.handItems.getValue();

                    context.pose().pushMatrix();
                    context.pose().translate((float) bx, (float) y);
                    context.pose().scale((float) scale, (float) scale);

                    boolean bgEnabled = drawNametags ? NameTags.INSTANCE.background.getValue() : (hasOwner && MobOwner.INSTANCE.background.getValue());
                    if (bgEnabled) {
                        int bgColor = drawNametags ? NameTags.INSTANCE.backgroundColor.getValue() : 0xBB000000;
                        int bgLeft = (int)(-totalW / 2.0 - 3.0);
                        int bgRight = (int)(totalW / 2.0 + 3.0);
                        int bgBottom = -2;
                        int bgTop = (int)(-2.0 - totalH);

                        context.fill(bgLeft, bgTop, bgRight, bgBottom, bgColor);

                        boolean drawTopLine = false;
                        int lineCol = 0;
                        if (drawNametags) {
                            drawTopLine = NameTags.INSTANCE.topLine.getValue();
                            lineCol = NameTags.INSTANCE.topLineColor.getValue();
                        } else if (hasOwner) {
                            drawTopLine = MobOwner.INSTANCE.background.getValue();
                            lineCol = 0x44FFAA00;
                        }
                        if (drawTopLine) {
                            context.fill(bgLeft, bgTop, bgRight, bgTop + 1, lineCol);
                        }
                    }

                    int armorCount = 0;
                    if (showArmor) {
                        if (!livingTarget.getItemBySlot(EquipmentSlot.FEET).isEmpty()) armorCount++;
                        if (!livingTarget.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) armorCount++;
                        if (!livingTarget.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) armorCount++;
                        if (!livingTarget.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) armorCount++;
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
                            if (!stack.isEmpty()) {
                                context.renderItem(stack, ax, ay);
                                ax += 16 + 2;
                            }
                        }
                    }

                    if (drawNametags) {
                        int rx = (int)(-mainRowW / 2.0);
                        int mY = (int) mainRowY;
                        if (showHands && !livingTarget.getMainHandItem().isEmpty()) {
                            context.renderItem(livingTarget.getMainHandItem(), rx, mY);
                            rx += 16 + 2;
                        }
                        if (NameTags.INSTANCE.customFont.getValue()) {
                            ravex.utility.render.FontRenderUtility.drawString(context, tagText, rx, (int)(mY + textYOff), 0xFFFFFFFF, false);
                        } else {
                            context.drawString(mc.font, tagText, rx, (int)(mY + textYOff), 0xFFFFFFFF, false);
                        }
                        rx += (int) tw;
                        if (showHands && !livingTarget.getOffhandItem().isEmpty()) {
                            rx += 2;
                            context.renderItem(livingTarget.getOffhandItem(), rx, mY);
                        }
                    }

                    if (hasOwner) {
                        String ownerText = "Owner: " + ownerName;
                        int otx = (int)(-ow / 2.0);
                        int oty = (int) ownerRowY;
                        if (NameTags.INSTANCE.customFont.getValue()) {
                            ravex.utility.render.FontRenderUtility.drawString(context, ownerText, otx, oty, MobOwner.INSTANCE.textColor.getValue(), false);
                        } else {
                            context.drawString(mc.font, ownerText, otx, oty, MobOwner.INSTANCE.textColor.getValue(), false);
                        }
                    }

                    context.pose().popMatrix();
                }
            }
        } else {
            
            for (Entity target : candidates) {
                boolean isPlayer = target instanceof Player;
                boolean isMonster = target instanceof LivingEntity le && ravex.utility.misc.MobUtility.isHostile(le);
                boolean isAnimal = target instanceof net.minecraft.world.entity.animal.Animal || 
                                   target instanceof net.minecraft.world.entity.ambient.AmbientCreature;
                boolean isItem = target instanceof net.minecraft.world.entity.item.ItemEntity;

                Vec3 basePos = target.getPosition(pt);
                double dist = mc.player.distanceTo(target);
                float bbHeight = target.getBbHeight();
                float bbWidth = target.getBbWidth();
                Vec3 headPos = basePos.add(0, bbHeight, 0);
                Vec3 sidePos = basePos.add(bbWidth / 2.0f, bbHeight / 2.0f, 0);
                double basePosX = basePos.x, basePosY = basePos.y, basePosZ = basePos.z;

                Vec3 baseProj = mc.gameRenderer.projectPointToScreen(basePos);
                Vec3 headProj = mc.gameRenderer.projectPointToScreen(headPos);
                Vec3 sideProj = mc.gameRenderer.projectPointToScreen(sidePos);

                if (baseProj == null || headProj == null || sideProj == null) continue;

                Vec3 dir = (new Vec3(basePosX - cameraPos.x, basePosY - cameraPos.y, basePosZ - cameraPos.z)).normalize();
                double dot = dir.dot(cameraLook);
                if (dot <= 0.0) continue;

                double sx_base = (baseProj.x + 1.0) / 2.0 * guiWidth;
                double sy_base = (1.0 - baseProj.y) / 2.0 * guiHeight;
                double sx_head = (headProj.x + 1.0) / 2.0 * guiWidth;
                double sy_head = (1.0 - headProj.y) / 2.0 * guiHeight;
                double sx_side = (sideProj.x + 1.0) / 2.0 * guiWidth;

                if ((sx_base < 0 || sx_base > guiWidth || sy_base < 0 || sy_base > guiHeight) &&
                    (sx_head < 0 || sx_head > guiWidth || sy_head < 0 || sy_head > guiHeight)) {
                    continue;
                }

                int bx = (int) sx_base;
                int by = (int) sy_base;
                int hy = (int) sy_head;
                int sx = (int) sx_side;

                int boxH = Math.abs(by - hy);
                int halfBoxW = Math.max(2, Math.abs(sx - bx));
                int boxW = halfBoxW * 2;
                int boxX = bx - halfBoxW;
                int y = Math.min(by, hy);

                String ownerName = (mobOwnerEnabled && target instanceof LivingEntity living) ? MobOwner.getOwnerName(living) : null;
                boolean hasOwner = ownerName != null;

                if (espEnabled) {
                    String mode = ESP.INSTANCE.mode.getValue();
                    int espColor = isPlayer ? ESP.INSTANCE.playerColor.getValue() :
                                   (isMonster ? ESP.INSTANCE.mobColor.getValue() :
                                   (isAnimal ? ESP.INSTANCE.animalColor.getValue() :
                                   (isItem ? ESP.INSTANCE.itemColor.getValue() :
                                   ESP.INSTANCE.frameColor.getValue())));

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

                boolean withinRange = NameTags.isNativeAvailable() ? 
                    NameTags.nativeIsWithinRange(dist, NameTags.INSTANCE.range.getValue()) : 
                    (dist <= NameTags.INSTANCE.range.getValue());
                boolean drawNametags = nameTagsEnabled && (target instanceof LivingEntity) && withinRange;
                if (drawNametags || hasOwner) {
                    LivingEntity livingTarget = (LivingEntity) target;
                    String displayName = livingTarget.getDisplayName().getString();
                    int health = (int) Math.ceil(livingTarget.getHealth());
                    int maxHealth = (int) Math.ceil(livingTarget.getMaxHealth());
                    String tagText = displayName + " §a" + health + "§7/§a" + maxHealth;
                    double tw = drawNametags ? (NameTags.INSTANCE.customFont.getValue() ? ravex.utility.render.FontRenderUtility.getStringWidth(tagText) : mc.font.width(tagText)) : 0.0;
                    double ow = hasOwner ? (NameTags.INSTANCE.customFont.getValue() ? ravex.utility.render.FontRenderUtility.getStringWidth("Owner: " + ownerName) : mc.font.width("Owner: " + ownerName)) : 0.0;

                    boolean showArmor = drawNametags && NameTags.INSTANCE.armor.getValue();
                    boolean showHands = drawNametags && NameTags.INSTANCE.handItems.getValue();

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
                        layout = NameTags.nativeCalculateLayout(
                            dist,
                            NameTags.INSTANCE.scale.getValue(),
                            NameTags.INSTANCE.distanceScaling.getValue(),
                            showArmor,
                            showHands,
                            hasOwner,
                            tw,
                            ow,
                            hasMainHand,
                            hasOffHand,
                            armorCount
                        );
                    } else {
                        layout = NameTags.javaFallbackCalculate(
                            dist,
                            NameTags.INSTANCE.scale.getValue(),
                            NameTags.INSTANCE.distanceScaling.getValue(),
                            showArmor,
                            showHands,
                            hasOwner,
                            tw,
                            ow,
                            hasMainHand,
                            hasOffHand,
                            armorCount
                        );
                    }

                    double scale = layout[0];
                    double totalW = layout[1];
                    double totalH = layout[2];
                    double armorRowY = layout[3];
                    double mainRowY = layout[4];
                    double ownerRowY = layout[5];
                    double textYOff = layout[6];
                    double mainRowW = layout[7];
                    double armorRowW = layout[8];

                    context.pose().pushMatrix();
                    context.pose().translate((float) bx, (float) y);
                    context.pose().scale((float) scale, (float) scale);

                    boolean bgEnabled = drawNametags ? NameTags.INSTANCE.background.getValue() : (hasOwner && MobOwner.INSTANCE.background.getValue());
                    if (bgEnabled) {
                        int bgColor = drawNametags ? NameTags.INSTANCE.backgroundColor.getValue() : 0xBB000000;
                        int bgLeft = (int)(-totalW / 2.0 - 3.0);
                        int bgRight = (int)(totalW / 2.0 + 3.0);
                        int bgBottom = -2;
                        int bgTop = (int)(-2.0 - totalH);

                        context.fill(bgLeft, bgTop, bgRight, bgBottom, bgColor);

                        boolean drawTopLine = false;
                        int lineCol = 0;
                        if (drawNametags) {
                            drawTopLine = NameTags.INSTANCE.topLine.getValue();
                            lineCol = NameTags.INSTANCE.topLineColor.getValue();
                        } else if (hasOwner) {
                            drawTopLine = MobOwner.INSTANCE.background.getValue();
                            lineCol = 0x44FFAA00;
                        }
                        if (drawTopLine) {
                            context.fill(bgLeft, bgTop, bgRight, bgTop + 1, lineCol);
                        }
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
                            if (!stack.isEmpty()) {
                                context.renderItem(stack, ax, ay);
                                ax += 16 + 2;
                            }
                        }
                    }

                    if (drawNametags) {
                        int rx = (int)(-mainRowW / 2.0);
                        int mY = (int) mainRowY;
                        if (showHands && !livingTarget.getMainHandItem().isEmpty()) {
                            context.renderItem(livingTarget.getMainHandItem(), rx, mY);
                            rx += 16 + 2;
                        }
                        if (NameTags.INSTANCE.customFont.getValue()) {
                            ravex.utility.render.FontRenderUtility.drawString(context, tagText, rx, (int)(mY + textYOff), 0xFFFFFFFF, false);
                        } else {
                            context.drawString(mc.font, tagText, rx, (int)(mY + textYOff), 0xFFFFFFFF, false);
                        }
                        rx += (int) tw;
                        if (showHands && !livingTarget.getOffhandItem().isEmpty()) {
                            rx += 2;
                            context.renderItem(livingTarget.getOffhandItem(), rx, mY);
                        }
                    }

                    if (hasOwner) {
                        String ownerText = "Owner: " + ownerName;
                        int otx = (int)(-ow / 2.0);
                        int oty = (int) ownerRowY;
                        if (NameTags.INSTANCE.customFont.getValue()) {
                            ravex.utility.render.FontRenderUtility.drawString(context, ownerText, otx, oty, MobOwner.INSTANCE.textColor.getValue(), false);
                        } else {
                            context.drawString(mc.font, ownerText, otx, oty, MobOwner.INSTANCE.textColor.getValue(), false);
                        }
                    }

                    context.pose().popMatrix();
                }
            }
        }

        
        ravex.modules.combat.BasePlace bp = ravex.modules.combat.BasePlace.INSTANCE;
        if (bp.getEnabled() && ravex.modules.combat.BasePlace.getSimulatedPlacementBlock() != null) {
            BlockPos p = ravex.modules.combat.BasePlace.getSimulatedPlacementBlock();
            Vec3 pos3d = new Vec3(p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5);
            Vec3 proj = mc.gameRenderer.projectPointToScreen(pos3d);
            if (proj != null) {
                Vec3 dir = pos3d.subtract(cameraPos).normalize();
                Vec3 look = mc.player.getViewVector(pt);
                double dot = dir.dot(look);
                if (dot > 0.0) {
                    double sx = (proj.x + 1.0) / 2.0 * context.guiWidth();
                    double sy = (1.0 - proj.y) / 2.0 * context.guiHeight();

                    int x = (int) sx;
                    int y = (int) sy;

                    String dmgText = String.format("Dmg: %.1f | Self: %.1f", 
                            ravex.modules.combat.BasePlace.currentTargetDamage,
                            ravex.modules.combat.BasePlace.currentSelfDamage);

                    int w = mc.font.width(dmgText);
                    int left = x - w / 2 - 4;
                    int top = y - 5;
                    int right = x + w / 2 + 4;
                    int bottom = y + 5;

                    context.fill(left, top, right, bottom, 0xAA000000);
                    context.fill(left, top, right, top + 1, 0xFF00FF00); 

                    context.drawString(mc.font, dmgText, x - w / 2, y - 4, 0xFFFFFFFF, false);
                }
            }
        }

        
        ravex.modules.combat.AnchorAura aa = ravex.modules.combat.AnchorAura.INSTANCE;
        if (aa.getEnabled() && ravex.modules.combat.AnchorAura.simulatedPlacementBlock != null) {
            BlockPos p = ravex.modules.combat.AnchorAura.simulatedPlacementBlock;
            Vec3 pos3d = new Vec3(p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5);
            Vec3 proj = mc.gameRenderer.projectPointToScreen(pos3d);
            if (proj != null) {
                Vec3 dir = pos3d.subtract(cameraPos).normalize();
                Vec3 look = mc.player.getViewVector(pt);
                double dot = dir.dot(look);
                if (dot > 0.0) {
                    double sx = (proj.x + 1.0) / 2.0 * context.guiWidth();
                    double sy = (1.0 - proj.y) / 2.0 * context.guiHeight();

                    int x = (int) sx;
                    int y = (int) sy;

                    String dmgText = String.format("Dmg: %.1f | Self: %.1f", 
                            ravex.modules.combat.AnchorAura.currentTargetDamage,
                            ravex.modules.combat.AnchorAura.currentSelfDamage);

                    int w = mc.font.width(dmgText);
                    int left = x - w / 2 - 4;
                    int top = y - 5;
                    int right = x + w / 2 + 4;
                    int bottom = y + 5;

                    context.fill(left, top, right, bottom, 0xAA000000);
                    context.fill(left, top, right, top + 1, 0xFF00FFFF); 

                    context.drawString(mc.font, dmgText, x - w / 2, y - 4, 0xFFFFFFFF, false);
                }
            }
        }

        
        ravex.modules.combat.AutoCrystal ac = ravex.modules.combat.AutoCrystal.INSTANCE;
        if (ac.getEnabled() && ac.renderDamage.getValue() && ravex.modules.combat.AutoCrystal.currentPlacementBlock != null) {

            BlockPos p = ravex.modules.combat.AutoCrystal.currentPlacementBlock;
            Vec3 pos3d = new Vec3(p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5);
            Vec3 proj = mc.gameRenderer.projectPointToScreen(pos3d);
            if (proj != null) {
                Vec3 dir = pos3d.subtract(cameraPos).normalize();
                Vec3 look = mc.player.getViewVector(pt);
                double dot = dir.dot(look);
                if (dot > 0.0) {
                    double sx = (proj.x + 1.0) / 2.0 * context.guiWidth();
                    double sy = (1.0 - proj.y) / 2.0 * context.guiHeight();

                    int x = (int) sx;
                    int y = (int) sy;

                    String dmgText = String.format("Target: %.1f | Self: %.1f", 
                            ravex.modules.combat.AutoCrystal.currentTargetDamage,
                            ravex.modules.combat.AutoCrystal.currentSelfDamage);
                    String totemsText = String.format("Totems: %d", ravex.modules.combat.AutoCrystal.currentTargetTotems);

                    int w1 = mc.font.width(dmgText);
                    int w2 = mc.font.width(totemsText);
                    int w = Math.max(w1, w2);

                    int left = x - w / 2 - 4;
                    int top = y - 10;
                    int right = x + w / 2 + 4;
                    int bottom = y + 10;

                    context.fill(left, top, right, bottom, 0xAA000000);
                    context.fill(left, top, right, top + 1, 0xFF00DDFF);

                    context.drawString(mc.font, dmgText, x - w1 / 2, y - 8, 0xFFFFFFFF, false);
                    context.drawString(mc.font, totemsText, x - w2 / 2, y + 1, 0xFFFFCC00, false);
                }
            }
        }

        
        ravex.modules.world.AutoSmelt asm = ravex.modules.world.AutoSmelt.INSTANCE;
        if (asm.getEnabled() && asm.render.getValue() && ravex.modules.world.AutoSmelt.currentTarget != null) {
            BlockPos p = ravex.modules.world.AutoSmelt.currentTarget;
            Vec3 pos3d = new Vec3(p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5);
            Vec3 proj = mc.gameRenderer.projectPointToScreen(pos3d);
            if (proj != null) {
                Vec3 dir = pos3d.subtract(cameraPos).normalize();
                Vec3 look = mc.player.getViewVector(pt);
                if (dir.dot(look) > 0.0) {
                    double sx = (proj.x + 1.0) / 2.0 * context.guiWidth();
                    double sy = (1.0 - proj.y) / 2.0 * context.guiHeight();
                    int x = (int) sx;
                    int y = (int) sy;

                    String statusText = "";
                    if (mc.player.containerMenu instanceof net.minecraft.world.inventory.AbstractFurnaceMenu furnace) {
                        var resultStack = furnace.getSlot(2).getItem();
                        var inputStack = furnace.getSlot(0).getItem();
                        if (!resultStack.isEmpty()) {
                            statusText = "§fResult: §e" + resultStack.getHoverName().getString() + " §7x" + resultStack.getCount();
                        } else if (!inputStack.isEmpty()) {
                            float progress = furnace.getBurnProgress();
                            statusText = "§7Smelting: §f" + inputStack.getHoverName().getString() + " §7(" + (int)(progress * 100) + "%)";
                        } else {
                            statusText = "§7Idle";
                        }
                    } else {
                        statusText = "§7Furnace";
                    }

                    int w = mc.font.width(statusText);
                    context.drawString(mc.font, statusText, x - w / 2, y - 4, 0xFFFFFFFF, true);
                }
            }
        }

        
        ravex.modules.world.AutoBrew ab = ravex.modules.world.AutoBrew.INSTANCE;
        if (ab.getEnabled() && ab.render.getValue() && ravex.modules.world.AutoBrew.currentTarget != null) {
            BlockPos p = ravex.modules.world.AutoBrew.currentTarget;
            Vec3 pos3d = new Vec3(p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5);
            Vec3 proj = mc.gameRenderer.projectPointToScreen(pos3d);
            if (proj != null) {
                Vec3 dir = pos3d.subtract(cameraPos).normalize();
                Vec3 look = mc.player.getViewVector(pt);
                if (dir.dot(look) > 0.0) {
                    double sx = (proj.x + 1.0) / 2.0 * context.guiWidth();
                    double sy = (1.0 - proj.y) / 2.0 * context.guiHeight();
                    int x = (int) sx;
                    int y = (int) sy;

                    String statusText = "";
                    if (mc.player.containerMenu instanceof net.minecraft.world.inventory.BrewingStandMenu brew) {
                        int ticks = brew.getBrewingTicks();
                        int fuel = brew.getFuel();
                        var ingr = brew.getSlot(3).getItem();

                        if (ticks > 0) {
                            statusText = "§dBrewing... §7(" + (400 - ticks) / 20 + "s)";
                        } else if (!ingr.isEmpty()) {
                            statusText = "§dReady: §f" + ingr.getHoverName().getString();
                        } else {
                            statusText = "§7Idle";
                        }

                        String fuelText = "§7Fuel: " + fuel;

                        String totalText = statusText + " | " + fuelText;
                        int w = mc.font.width(totalText);
                        context.drawString(mc.font, totalText, x - w / 2, y - 4, 0xFFFFFFFF, true);
                    }
                }
            }
        }

        
        ravex.modules.player.PacketMine pm = ravex.modules.player.PacketMine.INSTANCE;
        if (pm.getEnabled() && pm.render.getValue()) {
            for (var mb : ravex.modules.player.PacketMine.miningBlocks) {
                if (mb == null || mb.pos == null) continue;
                long now = System.currentTimeMillis();
                boolean expired = mb.done && now > mb.visibleUntil;
                if (expired) continue;
                Vec3 pos3d = new Vec3(mb.pos.getX() + 0.5, mb.pos.getY() + 1.4, mb.pos.getZ() + 0.5);
                Vec3 proj = projectPointToScreenUnbobbed(pos3d);
                if (proj != null) {
                    double sx = (proj.x + 1.0) / 2.0 * context.guiWidth();
                    double sy = (1.0 - proj.y) / 2.0 * context.guiHeight();
                    int x = (int) sx;
                    int y = (int) sy;

                    long elapsed = now - mb.startTime;
                    int pct = mb.done ? 100 : (int)((float)elapsed / Math.max(1, mb.breakAt) * 100);
                    pct = Math.min(100, pct);

                    if (mb.done) {
                        context.drawString(mc.font, "Done", x - mc.font.width("Done") / 2, y - 4, 0xFFFFFFFF, true);
                    } else {
                        context.drawString(mc.font, pct + "%", x - mc.font.width(pct + "%") / 2, y - 4, 0xFFFFFFFF, true);
                    }
                }
            }
        }

        
        if (ravex.modules.render.Waypoint.INSTANCE.getEnabled()) {
            int wpColor = ravex.modules.render.Waypoint.INSTANCE.color.getValue();
            String currentDim = mc.level != null ? mc.level.dimension().identifier().toString() : null;
            boolean showDist = ravex.modules.render.Waypoint.INSTANCE.showDistance.getValue();
            boolean showNm = ravex.modules.render.Waypoint.INSTANCE.showName.getValue();
            for (var wp : ravex.modules.render.Waypoint.getWaypoints()) {
                if (currentDim != null && !wp.dimension().equals(currentDim)) continue;

                Vec3 pos3d = new Vec3(wp.x() + 0.5, wp.y() + 1.5, wp.z() + 0.5);
                Vec3 proj = mc.gameRenderer.projectPointToScreen(pos3d);
                if (proj != null && proj.z > 0.0) {
                    double sx = (proj.x + 1.0) / 2.0 * context.guiWidth();
                    double sy = (1.0 - proj.y) / 2.0 * context.guiHeight();
                    int ix = (int) sx;
                    int iy = (int) sy;

                    double dist = mc.player.distanceToSqr(wp.x(), wp.y(), wp.z());
                    dist = Math.sqrt(dist);

                    String text = "";
                    if (showNm) text = wp.name();
                    if (showDist) {
                        if (!text.isEmpty()) text += " ";
                        text += "§7" + (int)dist + "m";
                    }
                    if (text.isEmpty()) text = wp.name();

                    int tw = mc.font.width(text);
                    int th = mc.font.lineHeight;
                    int pad = 3;

                    context.pose().pushMatrix();
                    context.pose().translate(ix, iy);

                    int bgLeft = -tw / 2 - pad;
                    int bgRight = tw / 2 + pad;
                    int bgTop = -th / 2 - pad;
                    int bgBottom = th / 2 + pad;

                    context.fill(bgLeft, bgTop, bgRight, bgBottom, 0xAA000000);
                    context.drawString(mc.font, text, -tw / 2, -th / 2, wpColor, false);

                    context.pose().popMatrix();
                }
            }
        }

        ravex.manager.NotificationManager.render(context);

        if (ravex.modules.render.Crosshair.INSTANCE.getEnabled()) {
            try {
                ravex.modules.render.Crosshair.INSTANCE.render(context);
            } catch (Throwable ignored) {}
        }

        renderHud(context, tickCounter);
    }

    private void renderHud(GuiGraphics context, DeltaTracker tickCounter) {
        java.util.List<Module> enabledModules = new java.util.ArrayList<>();
        for (Module hud : ModuleManager.INSTANCE.getHudModules()) {
            if (hud.getEnabled()) {
                enabledModules.add(hud);
            }
        }

        
        ravex.utility.misc.GuiOptimizer.optimizeHudAnimations(enabledModules);

        for (Module hud : enabledModules) {
            try {
                hud.render(context, tickCounter.getGameTimeDeltaTicks());
            } catch (Throwable ignored) {}
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderPortalOverlay(GuiGraphics guiGraphics, float f, CallbackInfo ci) {
        if (ravex.modules.render.NoRender.INSTANCE.getEnabled() && ravex.modules.render.NoRender.INSTANCE.portal.getValue()) {
            ci.cancel();
        }
    }



    private void drawTracerLine2D(GuiGraphics context, float x1, float y1, float x2, float y2, int color, float width) {
        float dx = x2 - x1;
        float dy = y2 - y1;
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
        org.joml.Matrix4f projectionMatrix = ravex.manager.ShaderManager.INSTANCE.getProjectionMatrix();
        if (projectionMatrix == null) return null;

        org.joml.Quaternionf cameraRotation = new org.joml.Quaternionf(camera.rotation());
        org.joml.Matrix4f modelViewMatrix = new org.joml.Matrix4f().rotation(cameraRotation.conjugate());

        Vec3 camPos = camera.position();
        org.joml.Vector4f vector4f = new org.joml.Vector4f(
            (float) (pos.x - camPos.x),
            (float) (pos.y - camPos.y),
            (float) (pos.z - camPos.z),
            1.0F
        );

        modelViewMatrix.transform(vector4f);
        projectionMatrix.transform(vector4f);

        if (vector4f.w <= 0.0F) {
            return null;
        } else {
            vector4f.div(vector4f.w);
            return new Vec3(vector4f.x, vector4f.y, vector4f.z);
        }
    }
}
