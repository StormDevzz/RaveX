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

@Mixin(Gui.class)
public abstract class MixinInGameHUD {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        // 1. Draw Ambient full screen color tint overlay if enabled
        if (Ambient.INSTANCE.getEnabled()) {
            int rVal = Ambient.INSTANCE.r.getValue().intValue();
            int gVal = Ambient.INSTANCE.g.getValue().intValue();
            int bVal = Ambient.INSTANCE.b.getValue().intValue();
            int aVal = Ambient.INSTANCE.a.getValue().intValue();
            int color = ((aVal & 0xFF) << 24) | ((rVal & 0xFF) << 16) | ((gVal & 0xFF) << 8) | (bVal & 0xFF);
            context.fill(0, 0, context.guiWidth(), context.guiHeight(), color);
        }

        // 2. Render custom 2D Box ESP & Holographic NameTags
        if (mc.level != null && mc.player != null) {
            float pt = tickCounter.getGameTimeDeltaTicks();
            Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof LivingEntity target)) continue;
                if (target == mc.player || !target.isAlive()) continue;

                // ESP Filters
                boolean isPlayer = target instanceof Player;
                boolean isMonster = target instanceof Monster;
                if (isPlayer && !ESP.INSTANCE.players.getValue()) continue;
                if (isMonster && !ESP.INSTANCE.monsters.getValue()) continue;

                // Project entity's foot/head positions into screen space
                Vec3 basePos = target.getPosition(pt);
                Vec3 headPos = basePos.add(0, target.getBbHeight(), 0);
                // Also project a side point to get accurate pixel width from entity bounding box
                Vec3 sidePos = basePos.add(target.getBbWidth() / 2.0, target.getBbHeight() / 2.0, 0);

                Vec3 baseProj = mc.gameRenderer.projectPointToScreen(basePos);
                Vec3 headProj = mc.gameRenderer.projectPointToScreen(headPos);
                Vec3 sideProj = mc.gameRenderer.projectPointToScreen(sidePos);

                if (baseProj != null && headProj != null && sideProj != null) {
                    // Check if entity is in front of the camera using dot product
                    Vec3 dir = target.getPosition(pt).subtract(cameraPos).normalize();
                    Vec3 look = mc.player.getViewVector(pt);
                    double dot = dir.dot(look);

                    if (dot > 0.0) {
                        // Project NDC [-1, 1] coordinates into actual scaled screen pixels
                        double sx_base = (baseProj.x + 1.0) / 2.0 * context.guiWidth();
                        double sy_base = (1.0 - baseProj.y) / 2.0 * context.guiHeight();
                        double sx_head = (headProj.x + 1.0) / 2.0 * context.guiWidth();
                        double sy_head = (1.0 - headProj.y) / 2.0 * context.guiHeight();
                        double sx_side = (sideProj.x + 1.0) / 2.0 * context.guiWidth();
                        double sy_side = (1.0 - sideProj.y) / 2.0 * context.guiHeight();

                        int bx = (int) sx_base;
                        int by = (int) sy_base;
                        int hy = (int) sy_head;
                        int sx = (int) sx_side;

                        // Box dimensions: height from foot to head, width from entity hitbox side projection
                        int boxH = Math.abs(by - hy);
                        // Width = 2 * half-width derived from side projection vs center
                        int halfBoxW = Math.max(2, Math.abs(sx - bx));
                        int boxW = halfBoxW * 2;
                        int x = bx - halfBoxW;
                        int y = Math.min(by, hy); // top of box is head

                        // Draw 2D Box ESP if enabled
                        if (ESP.INSTANCE.getEnabled() && ESP.INSTANCE.mode.getValue().equals("Box2D")) {
                            int espColor = isPlayer ? ESP.INSTANCE.playerColor.getValue() : ESP.INSTANCE.mobColor.getValue();
                            // 4 border lines
                            context.fill(x,          y,          x + boxW, y + 1,      espColor); // top
                            context.fill(x,          y + boxH,   x + boxW, y + boxH + 1, espColor); // bottom
                            context.fill(x,          y,          x + 1,    y + boxH,   espColor); // left
                            context.fill(x + boxW-1, y,          x + boxW, y + boxH,   espColor); // right
                            // Corner accents (2px)
                            int ca = 4;
                            context.fill(x, y, x + ca, y + 1, espColor);
                            context.fill(x + boxW - ca, y, x + boxW, y + 1, espColor);
                            context.fill(x, y + boxH, x + ca, y + boxH + 1, espColor);
                            context.fill(x + boxW - ca, y + boxH, x + boxW, y + boxH + 1, espColor);
                        }

                        // Draw Custom NameTags above entity head
                        if (NameTags.INSTANCE.getEnabled()) {
                            String displayName = target.getDisplayName().getString();
                            int health = (int) Math.ceil(target.getHealth());
                            int maxHealth = (int) Math.ceil(target.getMaxHealth());
                            String tagText = displayName + " §a" + health + "§7/§a" + maxHealth;
                            int tw = mc.font.width(tagText);
                            int textY = y - 13; // 13px above the top of the box

                            boolean showArmor = NameTags.INSTANCE.armor.getValue();
                            boolean showHands = NameTags.INSTANCE.handItems.getValue();

                            if (!showArmor && !showHands) {
                                int tx = bx - (tw / 2);
                                context.fill(tx - 3, textY - 1, tx + tw + 3, textY + 9, 0xBB000000);
                                context.fill(tx - 3, textY - 1, tx + tw + 3, textY, isPlayer ? 0xFFFF5555 : 0xFF55FF55);
                                context.drawString(mc.font, tagText, tx, textY, 0xFFFFFFFF, false);
                            } else {
                                ItemStack mainHand = target.getMainHandItem();
                                ItemStack offHand = target.getOffhandItem();
                                ItemStack boots = target.getItemBySlot(EquipmentSlot.FEET);
                                ItemStack leggings = target.getItemBySlot(EquipmentSlot.LEGS);
                                ItemStack chestplate = target.getItemBySlot(EquipmentSlot.CHEST);
                                ItemStack helmet = target.getItemBySlot(EquipmentSlot.HEAD);

                                int is = 16;
                                int gap = 2;

                                // Main row width
                                int mainRowW = tw;
                                if (showHands && !mainHand.isEmpty()) mainRowW += is + gap;
                                if (showHands && !offHand.isEmpty()) mainRowW += gap + is;

                                // Armor row width
                                int armorW = 0;
                                if (showArmor) {
                                    if (!boots.isEmpty()) armorW += is + gap;
                                    if (!leggings.isEmpty()) armorW += is + gap;
                                    if (!chestplate.isEmpty()) armorW += is + gap;
                                    if (!helmet.isEmpty()) armorW += is + gap;
                                    if (armorW > 0) armorW -= gap;
                                }

                                int totalW = Math.max(mainRowW, armorW);
                                int armorRowY = textY - is - gap;

                                // Background
                                int bgLeft = bx - totalW / 2 - 3;
                                int bgTop = showArmor && armorW > 0 ? armorRowY - 1 : textY - 1;
                                int bgRight = bx + totalW / 2 + 3;
                                int bgBottom = textY + 9;

                                context.fill(bgLeft, bgTop, bgRight, bgBottom, 0xBB000000);
                                context.fill(bgLeft, bgTop, bgRight, bgTop + 1, isPlayer ? 0xFFFF5555 : 0xFF55FF55);

                                // Armor items
                                if (showArmor && armorW > 0) {
                                    int ax = bx - armorW / 2;
                                    for (ItemStack stack : new ItemStack[]{boots, leggings, chestplate, helmet}) {
                                        if (!stack.isEmpty()) {
                                            context.renderItem(stack, ax, armorRowY);
                                            ax += is + gap;
                                        }
                                    }
                                }

                                // Main hand + text + offhand
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
                        }
                    }
            }
        }
        }

        // 3. Render each enabled HUD element
        for (HudModule hud : ModuleManager.INSTANCE.getHudModules()) {
            if (hud.getEnabled()) {
                hud.render(context, tickCounter.getGameTimeDeltaTicks());
            }
        }
    }
}
