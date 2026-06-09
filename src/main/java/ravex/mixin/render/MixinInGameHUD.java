package ravex.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.ModuleManager;
import ravex.modules.HudModule;
import ravex.modules.render.Ambient;
import ravex.modules.render.ESP;
import ravex.modules.render.NameTags;
import ravex.modules.render.MobOwner;
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

        for (Entity target : mc.level.entitiesForRendering()) {
            if (target == mc.player) continue;
            if (target instanceof LivingEntity living && !living.isAlive()) continue;

            // Distance limit check
            double dist = mc.player.distanceTo(target);
            double maxDist = ESP.INSTANCE.maxDistance.getValue();
            if (ravex.modules.render.Tracers.INSTANCE.getEnabled()) {
                maxDist = Math.max(maxDist, ravex.modules.render.Tracers.INSTANCE.maxDistance.getValue());
            }
            if (dist > maxDist) continue;

            // Prevent first person overlap when entity is extremely close
            if (mc.options.getCameraType().isFirstPerson() && dist < 1.2) continue;

            boolean isPlayer = target instanceof Player;
            boolean isMonster = target instanceof Monster;
            boolean isAnimal = target instanceof net.minecraft.world.entity.animal.Animal || 
                               target instanceof net.minecraft.world.entity.ambient.AmbientCreature;
            boolean isItem = target instanceof net.minecraft.world.entity.item.ItemEntity;
            boolean isFrame = target instanceof net.minecraft.world.entity.decoration.ItemFrame;

            if (isPlayer && !ESP.INSTANCE.players.getValue()) continue;
            if (isMonster && !ESP.INSTANCE.monsters.getValue()) continue;
            if (isAnimal && !ESP.INSTANCE.animals.getValue()) continue;
            if (isItem && !ESP.INSTANCE.items.getValue()) continue;
            if (isFrame && !ESP.INSTANCE.frames.getValue()) continue;

            if (!isPlayer && !isMonster && !isAnimal && !isItem && !isFrame) continue;

            Vec3 basePos = target.getPosition(pt);
            Vec3 headPos = basePos.add(0, target.getBbHeight(), 0);
            Vec3 sidePos = basePos.add(target.getBbWidth() / 2.0, target.getBbHeight() / 2.0, 0);

            // 1. Render Tracers first without dot/projection/off-screen checks of ESP/Nametags
            boolean tracersEnabled = ravex.modules.render.Tracers.INSTANCE.getEnabled();
            if (tracersEnabled) {
                ravex.modules.render.Tracers tracers = ravex.modules.render.Tracers.INSTANCE;
                if (dist <= tracers.maxDistance.getValue()) {
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
                        Vec3 baseProjUnbobbed = projectPointToScreenUnbobbed(basePos);
                        Vec3 headProjUnbobbed = projectPointToScreenUnbobbed(headPos);
                        if (baseProjUnbobbed != null && headProjUnbobbed != null) {
                            double cx = context.guiWidth() / 2.0;
                            double cy = context.guiHeight() / 2.0;
                            double ex = (baseProjUnbobbed.x + 1.0) / 2.0 * context.guiWidth();
                            double ey_base = (1.0 - baseProjUnbobbed.y) / 2.0 * context.guiHeight();
                            double ey_head = (1.0 - headProjUnbobbed.y) / 2.0 * context.guiHeight();
                            double ey = (ey_base + ey_head) / 2.0;

                            // Check if the target point is behind the camera or offscreen
                            boolean isBehind = baseProjUnbobbed.z < 0;
                            boolean isOffscreen = isBehind || ex < 0 || ex > context.guiWidth() || ey < 0 || ey > context.guiHeight();

                            if (isOffscreen) {
                                double dx = ex - cx;
                                double dy = ey - cy;
                                double tX = Double.MAX_VALUE;
                                double tY = Double.MAX_VALUE;
                                double borderPadding = 2.0;

                                if (dx > 0) {
                                    tX = (context.guiWidth() - borderPadding - cx) / dx;
                                } else if (dx < 0) {
                                    tX = (borderPadding - cx) / dx;
                                }

                                if (dy > 0) {
                                    tY = (context.guiHeight() - borderPadding - cy) / dy;
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
                            float width = tracers.lineWidth.getValue().floatValue();
                            drawTracerLine2D(context, (float) cx, (float) cy, (float) ex, (float) ey, color, width);
                        }
                    }
                }
            }

            // 2. Perform projections and culling checks specifically for ESP / NameTags
            Vec3 baseProj = mc.gameRenderer.projectPointToScreen(basePos);
            Vec3 headProj = mc.gameRenderer.projectPointToScreen(headPos);
            Vec3 sideProj = mc.gameRenderer.projectPointToScreen(sidePos);

            if (baseProj == null || headProj == null || sideProj == null) continue;

            Vec3 dir = target.getPosition(pt).subtract(cameraPos).normalize();
            Vec3 look = mc.player.getViewVector(pt);
            double dot = dir.dot(look);
            if (dot <= 0.0) continue;

            double sx_base = (baseProj.x + 1.0) / 2.0 * context.guiWidth();
            double sy_base = (1.0 - baseProj.y) / 2.0 * context.guiHeight();
            double sx_head = (headProj.x + 1.0) / 2.0 * context.guiWidth();
            double sy_head = (1.0 - headProj.y) / 2.0 * context.guiHeight();
            double sx_side = (sideProj.x + 1.0) / 2.0 * context.guiWidth();

            // Offscreen check: if all projected points are off-screen, skip rendering
            if ((sx_base < 0 || sx_base > context.guiWidth() || sy_base < 0 || sy_base > context.guiHeight()) &&
                (sx_head < 0 || sx_head > context.guiWidth() || sy_head < 0 || sy_head > context.guiHeight())) {
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

            boolean drawNametags = nameTagsEnabled && (target instanceof LivingEntity);
            if (drawNametags || hasOwner) {
                LivingEntity livingTarget = (LivingEntity) target;
                String displayName = livingTarget.getDisplayName().getString();
                int health = (int) Math.ceil(livingTarget.getHealth());
                int maxHealth = (int) Math.ceil(livingTarget.getMaxHealth());
                String tagText = displayName + " §a" + health + "§7/§a" + maxHealth;
                double tw = drawNametags ? mc.font.width(tagText) : 0.0;
                double ow = hasOwner ? mc.font.width("Owner: " + ownerName) : 0.0;

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
                    context.drawString(mc.font, tagText, rx, (int)(mY + textYOff), 0xFFFFFFFF, false);
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
                    context.drawString(mc.font, ownerText, otx, oty, MobOwner.INSTANCE.textColor.getValue(), false);
                }

                context.pose().popMatrix();
            }
        }

        // --- AutoCrystal 2D Overlay ---
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

        renderHud(context, tickCounter);
    }

    private void renderHud(GuiGraphics context, DeltaTracker tickCounter) {
        for (HudModule hud : ModuleManager.INSTANCE.getHudModules()) {
            if (hud.getEnabled()) {
                hud.render(context, tickCounter.getGameTimeDeltaTicks());
            }
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

        vector4f.mul(modelViewMatrix);
        vector4f.mul(projectionMatrix);

        if (vector4f.w == 0.0F) {
            return null;
        } else {
            vector4f.div(vector4f.w);
            return new Vec3(vector4f.x, vector4f.y, vector4f.z);
        }
    }
}
