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
            if (dist > ESP.INSTANCE.maxDistance.getValue()) continue;

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
                int tw = mc.font.width(tagText);
                int textY = y - 13;

                boolean showArmor = drawNametags && NameTags.INSTANCE.armor.getValue();
                boolean showHands = drawNametags && NameTags.INSTANCE.handItems.getValue();

                if (!showArmor && !showHands) {
                    int tx = bx - (tw / 2);
                    boolean drawMobOwnerBg = hasOwner && MobOwner.INSTANCE.background.getValue();
                    int tagHeight = (drawNametags && drawMobOwnerBg) ? 19 : (drawNametags ? 9 : (drawMobOwnerBg ? 19 : 0));
                    int tagColor = hasOwner ? 0x44FFAA00 : (isPlayer ? 0xFFFF5555 : 0xFF55FF55);
                    
                    if (drawNametags || drawMobOwnerBg) {
                        context.fill(tx - 3, textY - 1, tx + tw + 3, textY + tagHeight, 0xBB000000);
                        context.fill(tx - 3, textY - 1, tx + tw + 3, textY, tagColor);
                    }
                    if (drawNametags) {
                        context.drawString(mc.font, tagText, tx, textY, 0xFFFFFFFF, false);
                    }
                    if (hasOwner) {
                        String ownerText = "Owner: " + ownerName;
                        int txOwner = drawNametags ? tx : bx - (mc.font.width(ownerText) / 2);
                        int tyOwner = drawNametags ? textY + 10 : textY + 3;
                        context.drawString(mc.font, ownerText, txOwner, tyOwner, MobOwner.INSTANCE.textColor.getValue(), false);
                    }
                } else {
                    ItemStack mainHand = livingTarget.getMainHandItem();
                    ItemStack offHand = livingTarget.getOffhandItem();
                    ItemStack boots = livingTarget.getItemBySlot(EquipmentSlot.FEET);
                    ItemStack leggings = livingTarget.getItemBySlot(EquipmentSlot.LEGS);
                    ItemStack chestplate = livingTarget.getItemBySlot(EquipmentSlot.CHEST);
                    ItemStack helmet = livingTarget.getItemBySlot(EquipmentSlot.HEAD);

                    int is = 16;
                    int gap = 2;

                    int mainRowW = drawNametags ? tw : 0;
                    if (drawNametags && showHands && !mainHand.isEmpty()) mainRowW += is + gap;
                    if (drawNametags && showHands && !offHand.isEmpty()) mainRowW += gap + is;

                    int ownerAddW = hasOwner ? mc.font.width("Owner: " + ownerName) : 0;
                    int armorW = 0;
                    if (drawNametags && showArmor) {
                        if (!boots.isEmpty()) armorW += is + gap;
                        if (!leggings.isEmpty()) armorW += is + gap;
                        if (!chestplate.isEmpty()) armorW += is + gap;
                        if (!helmet.isEmpty()) armorW += is + gap;
                        if (armorW > 0) armorW -= gap;
                    }

                    int totalW = Math.max(mainRowW, armorW);
                    totalW = Math.max(totalW, ownerAddW);
                    int armorRowY = textY - is - gap;
                    
                    boolean drawMobOwnerBg = hasOwner && MobOwner.INSTANCE.background.getValue();
                    int extraRows = drawMobOwnerBg ? 10 : 0;
                    boolean drawBg = drawNametags || drawMobOwnerBg;

                    int bgLeft = bx - totalW / 2 - 3;
                    int bgTop = drawNametags && showArmor && armorW > 0 ? armorRowY - 1 : textY - 1;
                    int bgRight = bx + totalW / 2 + 3;
                    int bgBottom = textY + (drawNametags ? 9 : 0) + extraRows;

                    int tagColor = hasOwner ? 0x44FFAA00 : (isPlayer ? 0xFFFF5555 : 0xFF55FF55);
                    if (drawBg) {
                        context.fill(bgLeft, bgTop, bgRight, bgBottom, 0xBB000000);
                        context.fill(bgLeft, bgTop, bgRight, bgTop + 1, tagColor);
                    }

                    if (drawNametags && showArmor && armorW > 0) {
                        int ax = bx - armorW / 2;
                        for (ItemStack stack : new ItemStack[]{boots, leggings, chestplate, helmet}) {
                            if (!stack.isEmpty()) {
                                context.renderItem(stack, ax, armorRowY);
                                ax += is + gap;
                            }
                        }
                    }

                    if (drawNametags) {
                        int rx = bx - mainRowW / 2;
                        if (showHands && !mainHand.isEmpty()) {
                            context.renderItem(mainHand, rx, textY);
                            rx += is + gap;
                        }
                        context.drawString(mc.font, tagText, rx, textY, 0xFFFFFFFF, false);
                        rx += tw;
                        if (showHands && !offHand.isEmpty()) {
                            rx += gap;
                            context.renderItem(offHand, rx, textY);
                        }
                    }

                    if (hasOwner) {
                        String ownerText = "Owner: " + ownerName;
                        int otx = bx - ownerAddW / 2;
                        int oty = drawNametags ? textY + 10 : textY + 3;
                        context.drawString(mc.font, ownerText, otx, oty, MobOwner.INSTANCE.textColor.getValue(), false);
                    }
                }
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
}
