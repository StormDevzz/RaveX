package ravex.modules.hud;
import ravex.utility.misc.ScreenUtility;

import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

import net.minecraft.world.item.ItemStack;
import ravex.utility.render.ColorUtility;

import ravex.modules.combat.AutoCrystal;
import ravex.modules.combat.KillAura;
import ravex.modules.combat.Trigger;
import ravex.modules.client.Hud;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@HudModule("TargetHud")
public class TargetHud extends ravex.modules.Module {
    @Parameter(name = "Style", modes = {"Classic", "New", "Sexy"})
    public String style = "Classic";
    @Parameter(name = "MainHand")
    public boolean showMainHand = true;
    @Parameter(name = "Armor")
    public boolean showArmor = true;
    @Parameter(name = "ShowOnHover")
    public boolean showOnHover = true;
    @Parameter(name = "Health", modes = {"HP", "%"})
    public String healthDisplay = "HP";
    @Parameter(name = "ColoredHPText")
    public boolean coloredHealthText = false;
    @Parameter(name = "LowHPColor", color = true)
    public int lowHpColor = 0xFFFF3333;
    @Parameter(name = "HighHPColor", color = true)
    public int highHpColor = 0xFF33FF33;

    private static final net.minecraft.world.entity.EquipmentSlot[] SLOTS = {
        net.minecraft.world.entity.EquipmentSlot.MAINHAND,
        net.minecraft.world.entity.EquipmentSlot.OFFHAND,
        net.minecraft.world.entity.EquipmentSlot.HEAD,
        net.minecraft.world.entity.EquipmentSlot.CHEST,
        net.minecraft.world.entity.EquipmentSlot.LEGS,
        net.minecraft.world.entity.EquipmentSlot.FEET
    };

    private static final Identifier CREEPER_TEX = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/creeper/creeper.png");
    private static final Identifier ZOMBIE_TEX = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/zombie/zombie.png");
    private static final Identifier SKELETON_TEX = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/skeleton/skeleton.png");
    private static final Identifier WITHER_SKELETON_TEX = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/skeleton/wither_skeleton.png");
    private static final Identifier PIGLIN_TEX = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/piglin/piglin.png");

    private static final ItemStack CREEPER_HEAD = new ItemStack(net.minecraft.world.item.Items.CREEPER_HEAD);
    private static final ItemStack ZOMBIE_HEAD = new ItemStack(net.minecraft.world.item.Items.ZOMBIE_HEAD);
    private static final ItemStack SKELETON_SKULL = new ItemStack(net.minecraft.world.item.Items.SKELETON_SKULL);
    private static final ItemStack WITHER_SKELETON_SKULL = new ItemStack(net.minecraft.world.item.Items.WITHER_SKELETON_SKULL);
    private static final ItemStack PIGLIN_HEAD = new ItemStack(net.minecraft.world.item.Items.PIGLIN_HEAD);
    private static final ItemStack DRAGON_HEAD = new ItemStack(net.minecraft.world.item.Items.DRAGON_HEAD);
    private static final ItemStack TOTEM_STACK = new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);

    private float hudAlpha = 0f;
    private long lastFrameTime = 0;
    private net.minecraft.world.entity.LivingEntity lastTarget = null;
    private float animatedHpPercent = -1f;
    private float animatedAbsorbPercent = -1f;

    private float lastFormattedHp = -1f;
    private String cachedHpText = "";
    private float targetHurtAnim = 0f;
    private float lastEntityHealth = -1f;
    private int lastEntityId = -1;
    private long lastDamageMs = 0L;
    private long lastTargetSeenAt = 0L;
    private int lastHurtTime = 0;
    private boolean thunderSpawnPending = false;
    private long lastThunderFrame = 0L;
    // {x, y, dx, dy, size, opacity}
    private final java.util.List<double[]> thunderParticles = new java.util.ArrayList<>();
    private final java.util.Map<java.util.UUID, Integer> totemCounts = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Float> totemPopAnim = new java.util.HashMap<>();
    private float killAnim = 0f;
    private boolean lastTargetAlive = true;

    private TargetHud() {
        super("TargetHud", 350, 50, 180, 50);
        setX(10); setY(400); setWidth(175); setHeight(46);
    }

    @ravex.event.Subscribe
    public void onTotemPop(ravex.event.combat.TotemPopEvent event) {
        net.minecraft.world.entity.player.Player p = event.getPlayer();
        if (p == null) return;
        totemCounts.merge(p.getUUID(), 1, Integer::sum);
        totemPopAnim.put(p.getUUID(), 0f);
    }

    @Nullable
    private net.minecraft.world.entity.LivingEntity getTarget(MinecraftWrapper mc) {        if (Modules.enabled(KillAura.class)) {
            net.minecraft.world.entity.LivingEntity target = Modules.get(KillAura.class).getCurrentTarget();
            if (target != null && target.isAlive()) return target;
        }
        if (Modules.enabled(AutoCrystal.class)) {
            net.minecraft.world.entity.LivingEntity target = Modules.get(AutoCrystal.class).getCurrentTarget();
            if (target != null && target.isAlive()) return target;
        }
        if (Modules.enabled(Trigger.class)) {
            net.minecraft.world.entity.LivingEntity target = Modules.get(Trigger.class).getCurrentTarget();
            if (target != null && target.isAlive()) return target;
        }
        return null;
    }

    private Identifier getMobTexture(net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.monster.Creeper) return CREEPER_TEX;
        if (entity instanceof net.minecraft.world.entity.monster.zombie.Zombie) return ZOMBIE_TEX;
        if (entity instanceof net.minecraft.world.entity.monster.skeleton.Skeleton) return SKELETON_TEX;
        if (entity instanceof net.minecraft.world.entity.monster.skeleton.WitherSkeleton) return WITHER_SKELETON_TEX;
        if (entity instanceof net.minecraft.world.entity.monster.piglin.Piglin) return PIGLIN_TEX;
        return SKELETON_TEX;
    }

    private ItemStack getMobHeadItem(net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.monster.Creeper) return CREEPER_HEAD;
        if (entity instanceof net.minecraft.world.entity.monster.zombie.Zombie) return ZOMBIE_HEAD;
        if (entity instanceof net.minecraft.world.entity.monster.skeleton.Skeleton) return SKELETON_SKULL;
        if (entity instanceof net.minecraft.world.entity.monster.skeleton.WitherSkeleton) return WITHER_SKELETON_SKULL;
        if (entity instanceof net.minecraft.world.entity.monster.piglin.Piglin) return PIGLIN_HEAD;
        if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) return DRAGON_HEAD;
        return SKELETON_SKULL;
    }
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;

        long now = System.currentTimeMillis();
        if (lastFrameTime == 0) lastFrameTime = now;
        float delta = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;
        if (delta > 0.1f) delta = 0.016f;

        net.minecraft.world.entity.LivingEntity targetEntity = null;
        net.minecraft.world.entity.LivingEntity target = getTarget(mc);
        boolean hasActiveTarget = false;
        if (target != null) {
            targetEntity = target;
            lastTarget = target;
            lastTargetSeenAt = now;
            hasActiveTarget = true;
        } else if (Modules.enabled(AutoCrystal.class)
                && lastTarget != null
                && lastTarget.isAlive()
                && now - lastTargetSeenAt <= 3000L) {
            targetEntity = lastTarget;
            hasActiveTarget = true;
        } else if (showOnHover && mc.getCrosshairPickEntity() instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive()) {
            targetEntity = living;
            lastTarget = living;
            hasActiveTarget = true;
        } else if (ScreenUtility.isChatScreen(mc) || mc.getCurrentScreen() instanceof ravex.gui.hudeditor.HudEditorScreen) {
            targetEntity = mc.getPlayer();
            lastTarget = mc.getPlayer();
            hasActiveTarget = true;
        } else {
            targetEntity = lastTarget;
        }
        if (hasActiveTarget) {
            hudAlpha = Math.min(1.0f, hudAlpha + delta * 5.0f);
        } else {
            hudAlpha = Math.max(0.0f, hudAlpha - delta * 5.0f);
        }

        if (hudAlpha <= 0.001f) {
            lastTarget = null;
            animatedHpPercent = -1f;
            animatedAbsorbPercent = -1f;
            lastFormattedHp = -1f;
            lastEntityHealth = -1f;
            lastEntityId = -1;
            targetHurtAnim = 0f;
            return;
        }

        int bx = getX();
        int by = getY();
        int w = getWidth();
        int h = getHeight();

        // flash once when the shown target dies
        boolean targetDead = targetEntity != null && (targetEntity.isDeadOrDying() || targetEntity.getHealth() <= 0f);
        if (targetDead && lastTargetAlive) killAnim = 1f;
        lastTargetAlive = !targetDead;
        if (killAnim > 0f) killAnim = Math.max(0f, killAnim - delta * 1.8f);

        // any health drop counts for the new particles
        if (targetEntity != null) {
            if (targetEntity.getId() != lastEntityId) {
                lastEntityId = targetEntity.getId();
                lastEntityHealth = targetEntity.getHealth();
                targetHurtAnim = 0f;
            } else {
                float currentHealth = targetEntity.getHealth();
                if (currentHealth < lastEntityHealth) {
                    targetHurtAnim = 1.0f;
                    lastDamageMs = System.currentTimeMillis();
                    // one burst per hit, as in thunderhack
                    thunderSpawnPending = true;
                }
                lastEntityHealth = currentHealth;
            }
        }

        // hurt anim also bursts, hp may not visibly drop
        if (targetEntity != null) {
            int ht = targetEntity.hurtTime;
            if (ht > lastHurtTime && ht >= 8) {
                thunderSpawnPending = true;
            }
            lastHurtTime = ht;
        }

        if (targetHurtAnim > 0f) {
            targetHurtAnim = Math.max(0f, targetHurtAnim - delta * 2.2f);
        }

        if ("Sexy".equals(style)) {
            renderSexy(graphics, targetEntity, bx, by, delta);
            return;
        }
        if ("New".equals(style)) {
            renderNew(graphics, targetEntity, bx, by, delta);
            return;
        }

        float scale = 0.92f + 0.08f * hudAlpha;
        graphics.pose().pushMatrix();
        float centerX = bx + w / 2f;
        float centerY = by + h / 2f;
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-centerX, -centerY);

        int bgAlpha = (int)(170 * hudAlpha);
        int bgColor = ColorUtility.setAlpha(0x0A0A0E, bgAlpha);
        Render2DUtility.drawPixelPerfectRound(graphics, bx, by, w, h, 6, bgColor);

        float hurtProgress = Math.max(targetHurtAnim, targetEntity != null ? targetEntity.hurtTime / 10f : 0f);
        float headScale = 1.0f - 0.15f * hurtProgress;
        int headTint = ColorUtility.interpolate(0xFFFFFFFF, 0xFFFF4444, hurtProgress);

        int hSize = 32;
        int hx = bx + 6;
        int hy = by + 7;

        if (targetEntity instanceof net.minecraft.client.player.AbstractClientPlayer clientPlayer) {
            Identifier skinTex = clientPlayer.getSkin().body().texturePath();

            graphics.pose().pushMatrix();
            float headCX = hx + hSize / 2f;
            float headCY = hy + hSize / 2f;
            graphics.pose().translate(headCX, headCY);
            graphics.pose().scale(headScale, headScale);
            graphics.pose().translate(-headCX, -headCY);

            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, skinTex, hx, hy, 8.0f, 8.0f, hSize, hSize, 8, 8, 64, 64, ColorUtility.withAlpha(headTint, (int)(255 * hudAlpha)));
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, skinTex, hx, hy, 40.0f, 8.0f, hSize, hSize, 8, 8, 64, 64, ColorUtility.withAlpha(headTint, (int)(255 * hudAlpha)));

            graphics.pose().popMatrix();
        } else if (targetEntity != null) {
            Identifier mobTex = getMobTexture(targetEntity);

            graphics.pose().pushMatrix();
            float headCX = hx + hSize / 2f;
            float headCY = hy + hSize / 2f;
            graphics.pose().translate(headCX, headCY);
            graphics.pose().scale(headScale, headScale);
            graphics.pose().translate(-headCX, -headCY);

            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, mobTex, hx, hy, 8.0f, 8.0f, hSize, hSize, 8, 8, 64, 64, ColorUtility.withAlpha(headTint, (int)(255 * hudAlpha)));

            graphics.pose().popMatrix();
        }

        String displayName = targetEntity.getName().getString();

        int nx = bx + 44;
        int ny = by + 7;
        FontRenderUtility.drawString(graphics, displayName, nx, ny, ColorUtility.withAlpha(0xFFFFFFFF, (int)(255 * hudAlpha)), true);

        float hp = targetEntity.getHealth();
        float maxHp = targetEntity.getMaxHealth();
        float absorb = targetEntity.getAbsorptionAmount();
        float totalCapacity = maxHp + absorb;

        float targetHpFraction = hp / totalCapacity;
        float targetAbsorbFraction = absorb / totalCapacity;

        if (animatedHpPercent < 0f) {
            animatedHpPercent = targetHpFraction;
            animatedAbsorbPercent = targetAbsorbFraction;
        } else {
            animatedHpPercent += (targetHpFraction - animatedHpPercent) * Math.min(1.0f, delta * 8.0f);
            animatedAbsorbPercent += (targetAbsorbFraction - animatedAbsorbPercent) * Math.min(1.0f, delta * 8.0f);
        }

        String hpText = switch (healthDisplay) {
            case "HP" -> String.format("%.0f / %.0f", hp, maxHp);
            default -> maxHp > 0 ? String.format("%d%%", (int)(hp / maxHp * 100)) : "0%";
        };
        int hpTextColor = coloredHealthText ? ColorUtility.interpolate(lowHpColor, highHpColor, maxHp > 0 ? hp / maxHp : 1f) : 0xFFFFFFFF;
        FontRenderUtility.drawString(graphics, hpText, nx, by + 21, ColorUtility.withAlpha(hpTextColor, (int)(255 * hudAlpha)), true);

        int gridX = bx + w - 55;
        int barX = bx + 44;
        int barW = gridX - 4 - barX;
        int barY = by + 32;

        Render2DUtility.drawRound(graphics, barX, barY, barW, 3, 1, ColorUtility.withAlpha(0xFF1A1A2A, (int)(255 * hudAlpha)));

        int fillHpW = (int) (barW * animatedHpPercent);
        if (fillHpW > 0) {
            int hpColor = ColorUtility.interpolate(lowHpColor, highHpColor, hp / maxHp);
            Render2DUtility.drawRound(graphics, barX, barY, fillHpW, 3, 1, ColorUtility.withAlpha(hpColor, (int)(255 * hudAlpha)));
        }

        int fillAbsorbW = (int) (barW * animatedAbsorbPercent);
        if (fillAbsorbW > 0) {
            int absorbX = barX + fillHpW;
            Render2DUtility.drawRound(graphics, absorbX, barY, fillAbsorbW, 3, 1, ColorUtility.withAlpha(0xFFFFD54F, (int)(255 * hudAlpha)));
        }

        int cellSize = 15;
        int cellGap = 2;
        net.minecraft.world.entity.EquipmentSlot[][] gridSlots = {
            { net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST },
            { net.minecraft.world.entity.EquipmentSlot.OFFHAND, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET }
        };

        for (int rIndex = 0; rIndex < 2; rIndex++) {
            for (int cIndex = 0; cIndex < 3; cIndex++) {
                net.minecraft.world.entity.EquipmentSlot slot = gridSlots[rIndex][cIndex];
                int cellX = gridX + cIndex * (cellSize + cellGap);
                int cellY = by + 7 + rIndex * (cellSize + cellGap);

        Render2DUtility.drawPixelPerfectRound(graphics, cellX, cellY, cellSize, cellSize, 3, ColorUtility.withAlpha(0x000000, (int)(120 * hudAlpha)));
        Render2DUtility.drawPixelPerfectRoundBorder(graphics, cellX, cellY, cellSize, cellSize, 3, 1, ColorUtility.withAlpha(0x000000, (int)(60 * hudAlpha)));

                boolean shouldShow = (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND && showMainHand)
                        || (slot == net.minecraft.world.entity.EquipmentSlot.OFFHAND && showMainHand)
                        || (slot != net.minecraft.world.entity.EquipmentSlot.MAINHAND && slot != net.minecraft.world.entity.EquipmentSlot.OFFHAND && showArmor);

                if (shouldShow && targetEntity != null) {
                    ItemStack item = targetEntity.getItemBySlot(slot);
                    if (!item.isEmpty()) {
                        graphics.pose().pushMatrix();
                        float itemScale = 0.5f;
                        float offset = (cellSize - 16 * itemScale) / 2f;
                        graphics.pose().translate(cellX + offset, cellY + offset);
                        graphics.pose().scale(itemScale, itemScale);
                        graphics.renderItem(item, 0, 0);
                        graphics.renderItemDecorations(mc.getFont(), item, 0, 0);
                        graphics.pose().popMatrix();
                    }
                }
            }
        }

        graphics.pose().popMatrix();
    }

    private void renderSexy(GuiGraphics graphics, net.minecraft.world.entity.LivingEntity target, int bx, int by, float delta) {
        var mc = MinecraftWrapper.getWrapper();
        int pw = 140;
        int ph = 48;
        int bgAlpha = (int) (200 * hudAlpha);
        Render2DUtility.drawPixelPerfectRound(graphics, bx, by, pw, ph, 8, ColorUtility.setAlpha(0x0A0A0E, bgAlpha));
        if (target == null) return;

        float hp = target.getHealth();
        float maxHp = target.getMaxHealth();
        if (maxHp <= 0f) maxHp = 20f;
        float hpFraction = Math.max(0f, Math.min(1f, hp / maxHp));
        if (animatedHpPercent < 0f) animatedHpPercent = hpFraction;
        else animatedHpPercent += (hpFraction - animatedHpPercent) * Math.min(1f, delta * 8f);

        float hurtProgress = Math.max(targetHurtAnim, target.hurtTime / 10f);
        float headScale = 1f - 0.12f * hurtProgress;
        int headTint = ColorUtility.interpolate(0xFFFFFFFF, 0xFFFF4444, hurtProgress);

        int hSize = 34;
        int hx = bx + 6;
        int hy = by + 7;
        graphics.pose().pushMatrix();
        float headCX = hx + hSize / 2f;
        float headCY = hy + hSize / 2f;
        graphics.pose().translate(headCX, headCY);
        graphics.pose().scale(headScale, headScale);
        graphics.pose().translate(-headCX, -headCY);
        if (target instanceof net.minecraft.client.player.AbstractClientPlayer clientPlayer) {
            Identifier skinTex = clientPlayer.getSkin().body().texturePath();
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, skinTex, hx, hy, 8f, 8f, hSize, hSize, 8, 8, 64, 64, ColorUtility.withAlpha(headTint, (int) (255 * hudAlpha)));
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, skinTex, hx, hy, 40f, 8f, hSize, hSize, 8, 8, 64, 64, ColorUtility.withAlpha(headTint, (int) (255 * hudAlpha)));
        } else {
            Identifier mobTex = getMobTexture(target);
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, mobTex, hx, hy, 8f, 8f, hSize, hSize, 8, 8, 64, 64, ColorUtility.withAlpha(headTint, (int) (255 * hudAlpha)));
        }
        graphics.pose().popMatrix();

        FontRenderUtility.drawString(graphics, target.getName().getString(), bx + 46, by + 7, ColorUtility.withAlpha(0xFFFFFFFF, (int) (255 * hudAlpha)), true);

        if (target instanceof net.minecraft.world.entity.player.Player) {
            net.minecraft.world.entity.EquipmentSlot[] row = {
                net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET,
                net.minecraft.world.entity.EquipmentSlot.OFFHAND
            };
            float ix = bx + 46;
            for (net.minecraft.world.entity.EquipmentSlot slot : row) {
                boolean show = (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND || slot == net.minecraft.world.entity.EquipmentSlot.OFFHAND) ? showMainHand : showArmor;
                if (show) {
                    ItemStack item = target.getItemBySlot(slot);
                    if (!item.isEmpty()) {
                        graphics.pose().pushMatrix();
                        graphics.pose().translate(ix, by + 19);
                        graphics.pose().scale(0.5f, 0.5f);
                        graphics.renderItem(item, 0, 0);
                        graphics.renderItemDecorations(mc.getFont(), item, 0, 0);
                        graphics.pose().popMatrix();
                    }
                }
                ix += 10;
            }
        }

        int barX = bx + 46;
        int barW = 88;
        int barY = by + 34;
        int barH = 9;
        Render2DUtility.drawRound(graphics, barX, barY, barW, barH, 3, ColorUtility.withAlpha(0xFF14141E, (int) (255 * hudAlpha)));
        int fillW = (int) (barW * Math.max(0f, Math.min(1f, animatedHpPercent)));
        if (fillW > 0) {
            int hpColor = ColorUtility.interpolate(lowHpColor, highHpColor, hpFraction);
            Render2DUtility.drawRound(graphics, barX, barY, fillW, barH, 3, ColorUtility.withAlpha(hpColor, (int) (255 * hudAlpha)));
        }
        String hpText = switch (healthDisplay) {
            case "HP" -> String.format("%.0f", hp);
            default -> String.format("%d%%", (int) (hpFraction * 100));
        };
        float numScale = 0.8f;
        int tw = (int) (FontRenderUtility.getStringWidth(hpText) * numScale);
        FontRenderUtility.drawScaled(graphics, FontRenderUtility.getCurrentFontType(), hpText,
            barX + (barW - tw) / 2, barY + 1, numScale,
            ColorUtility.withAlpha(0xFFFFFFFF, (int) (255 * hudAlpha)), true);
    }

    private void renderNew(GuiGraphics graphics, net.minecraft.world.entity.LivingEntity target, int bx, int by, float delta) {
        var mc = MinecraftWrapper.getWrapper();
        int pw = 172;
        int ph = 54;
        int ac = ColorUtility.getActiveColor();
        int a = (int) (255 * hudAlpha);

        Render2DUtility.drawPixelPerfectRound(graphics, bx, by, pw, ph, 8, ColorUtility.withAlpha(0x0B0B12, (int) (215 * hudAlpha)));
        if (target == null) return;

        float hp = target.getHealth();
        float maxHp = Math.max(1f, target.getMaxHealth());
        float absorb = target.getAbsorptionAmount();
        float frac = Math.max(0f, Math.min(1f, hp / maxHp));
        float absFrac = Math.max(0f, Math.min(1f, absorb / maxHp));
        if (animatedHpPercent < 0f) {
            animatedHpPercent = frac;
            animatedAbsorbPercent = absFrac;
        } else {
            animatedHpPercent += (frac - animatedHpPercent) * Math.min(1f, delta * 9f);
            animatedAbsorbPercent += (absFrac - animatedAbsorbPercent) * Math.min(1f, delta * 9f);
        }

        float hurt = Math.max(targetHurtAnim, target.hurtTime / 6f);
        // red head flash, like the classic style
        float flash = (float) Math.min(1.0, Math.pow(hurt, 0.6));
        int headTint = ColorUtility.interpolate(0xFFFFFFFF, 0xFFFF2222, flash);

        int hSize = 36;
        int hx = bx + 5;
        int hy = by + 5;
        graphics.pose().pushMatrix();
        float hcx = hx + hSize / 2f;
        float hcy = hy + hSize / 2f;
        graphics.pose().translate(hcx, hcy);
        float hs = 1f - 0.10f * flash;
        graphics.pose().scale(hs, hs);
        graphics.pose().translate(-hcx, -hcy);
        if (target instanceof net.minecraft.client.player.AbstractClientPlayer cp) {
            Identifier skin = cp.getSkin().body().texturePath();
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, skin, hx, hy, 8f, 8f, hSize, hSize, 8, 8, 64, 64, ColorUtility.withAlpha(headTint, a));
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, skin, hx, hy, 40f, 8f, hSize, hSize, 8, 8, 64, 64, ColorUtility.withAlpha(headTint, a));
        } else {
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, getMobTexture(target), hx, hy, 8f, 8f, hSize, hSize, 8, 8, 64, 64, ColorUtility.withAlpha(headTint, a));
        }
        graphics.pose().popMatrix();
        Render2DUtility.drawPixelPerfectRoundBorder(graphics, hx - 1, hy - 1, hSize + 2, hSize + 2, 4, 1, ColorUtility.withAlpha(ac, (int) (120 * hudAlpha)));

        // thunder burst: seven particles, random velocity and drift
        if (thunderSpawnPending) {
            thunderSpawnPending = false;
            for (int i = 0; i < 7; i++) {
                thunderParticles.add(new double[]{
                    hx + hSize / 2.0 + (Math.random() - 0.5) * 8.0,
                    hy + hSize / 2.0 + (Math.random() - 0.5) * 8.0,
                    (Math.random() - 0.5) * 6.0,
                    (Math.random() - 0.5) * 6.0,
                    13.0 + Math.random() * 5.0,
                    254.0
                });
            }
        }
        if (!thunderParticles.isEmpty()) {
            Identifier thunderTex = ravex.utility.render.TextureLoaderUtility.getParticleTexture("thunder", 0xFFFFD54F);
            long pnow = System.currentTimeMillis();
            float pdt = lastThunderFrame == 0L ? 0.016f : Math.min(0.05f, (pnow - lastThunderFrame) / 1000f);
            lastThunderFrame = pnow;
            float step = pdt * 60f;
            java.util.Iterator<double[]> it = thunderParticles.iterator();
            while (it.hasNext()) {
                double[] part = it.next();
                if (thunderTex == null) {
                    it.remove();
                    continue;
                }
                part[0] += part[2] * step;
                part[1] += part[3] * step - 0.7 * step;
                part[2] *= Math.pow(0.95, step);
                part[3] *= Math.pow(0.95, step);
                part[4] /= Math.pow(1.01, step);
                part[5] -= 2.2 * step;
                if (part[5] <= 4 || part[4] < 2) {
                    it.remove();
                    continue;
                }
                int palpha = (int) (part[5] * hudAlpha);
                if (palpha <= 6) continue;
                float psize = (float) part[4];
                graphics.pose().pushMatrix();
                graphics.pose().translate((float) part[0], (float) part[1]);
                graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, thunderTex,
                    (int) (-psize / 2), (int) (-psize / 2), 0f, 0f,
                    (int) psize, (int) psize, (int) psize, (int) psize,
                    ColorUtility.withAlpha(0xFFFFFFFF, Math.min(255, palpha)));
                graphics.pose().popMatrix();
            }
        }

        int tx = bx + 48;
        String hpText = switch (healthDisplay) {
            case "HP" -> String.format("%.1f", hp);
            default -> String.format("%d%%", (int) (frac * 100));
        };
        int hpCol = coloredHealthText ? ColorUtility.interpolate(lowHpColor, highHpColor, frac) : 0xFFFFFFFF;
        int hpW = FontRenderUtility.getStringWidth(hpText);
        int totemPops = target instanceof net.minecraft.world.entity.player.Player
            ? totemCounts.getOrDefault(target.getUUID(), 0) : 0;
        int totemBadgeW = totemPops > 0 ? 8 + 3 + FontRenderUtility.getStringWidth("x" + totemPops) + 6 : 0;

        // name shrinks instead of running under the badge
        int nameMax = (bx + pw - hpW - 8) - tx - (totemBadgeW > 0 ? totemBadgeW + 6 : 6);
        String displayName = target.getName().getString();
        if (nameMax > 12 && FontRenderUtility.getStringWidth(displayName) > nameMax) {
            FontRenderUtility.FontType ft = FontRenderUtility.getCurrentFontType();
            FontRenderUtility.FitText fit = FontRenderUtility.fitText(ft, displayName, nameMax);
            FontRenderUtility.drawScaled(graphics, ft, fit.text, tx, by + 6, fit.scale, ColorUtility.withAlpha(0xFFFFFFFF, a), true);
        } else {
            FontRenderUtility.drawString(graphics, displayName, tx, by + 6, ColorUtility.withAlpha(0xFFFFFFFF, a), true);
        }
        FontRenderUtility.drawString(graphics, hpText, bx + pw - hpW - 8, by + 6, ColorUtility.withAlpha(hpCol, a), true);

        int barX = tx;
        int barW = pw - (tx - bx) - 8;
        int barY = by + 21;
        int barH = 8;
        Render2DUtility.drawPixelPerfectRound(graphics, barX, barY, barW, barH, 3, ColorUtility.withAlpha(0x14141E, a));
        int fill = (int) (barW * Math.max(0f, Math.min(1f, animatedHpPercent)));
        if (fill > 0) {
            int hpColor = ColorUtility.interpolate(lowHpColor, highHpColor, frac);
            Render2DUtility.drawPixelPerfectRound(graphics, barX, barY, fill, barH, 3, ColorUtility.withAlpha(hpColor, a));
        }
        int abs = (int) (barW * Math.max(0f, Math.min(1f, animatedAbsorbPercent)));
        if (abs > 0 && fill < barW) {
            Render2DUtility.drawPixelPerfectRound(graphics, barX + fill, barY, Math.min(abs, barW - fill), barH, 3, ColorUtility.withAlpha(0xFFFFD54F, a));
        }

        String subText = String.format("%.1f / %.1f", hp, maxHp);
        FontRenderUtility.drawString(graphics, subText, tx, by + 33, ColorUtility.withAlpha(0xFF9A9AA8, a), false);

        // totem pops top right with a pop-in bounce
        if (totemPops > 0) {
            Float popAnim = totemPopAnim.get(target.getUUID());
            if (popAnim != null && popAnim < 1f) {
                totemPopAnim.put(target.getUUID(), Math.min(1f, popAnim + delta * 3.5f));
            }
            float p01 = popAnim == null ? 1f : popAnim;
            float bounce = 1f + 0.55f * (1f - Math.min(1f, p01 * 1.6f));
            String popText = "x" + totemPops;
            int badgeX = bx + pw - hpW - 8 - totemBadgeW;
            int badgeY = by + 5;
            graphics.pose().pushMatrix();
            graphics.pose().translate(badgeX + totemBadgeW / 2f, badgeY + 5);
            graphics.pose().scale(bounce, bounce);
            graphics.pose().translate(-(badgeX + totemBadgeW / 2f), -(badgeY + 5));
            graphics.pose().pushMatrix();
            graphics.pose().translate(badgeX, badgeY);
            graphics.pose().scale(0.5f, 0.5f);
            graphics.renderItem(TOTEM_STACK, 0, 0);
            graphics.pose().popMatrix();
            FontRenderUtility.drawString(graphics, popText, badgeX + 11, badgeY + 1,
                ColorUtility.withAlpha(0xFFFFD54F, a), true);
            graphics.pose().popMatrix();
        }

        // gear row, right aligned, main hand first
        net.minecraft.world.entity.EquipmentSlot[] row = {
            net.minecraft.world.entity.EquipmentSlot.MAINHAND,
            net.minecraft.world.entity.EquipmentSlot.OFFHAND,
            net.minecraft.world.entity.EquipmentSlot.HEAD,
            net.minecraft.world.entity.EquipmentSlot.CHEST,
            net.minecraft.world.entity.EquipmentSlot.LEGS,
            net.minecraft.world.entity.EquipmentSlot.FEET
        };
        List<ItemStack> gear = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.EquipmentSlot slot : row) {
            boolean show = (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND || slot == net.minecraft.world.entity.EquipmentSlot.OFFHAND)
                ? showMainHand : showArmor;
            if (!show) continue;
            ItemStack item = target.getItemBySlot(slot);
            if (!item.isEmpty()) gear.add(item);
        }
        if (!gear.isEmpty()) {
            int step = 10;
            int ix = bx + pw - 8 - 9;
            for (int i = gear.size() - 1; i >= 0; i--) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(ix, by + 33);
                graphics.pose().scale(0.5f, 0.5f);
                graphics.renderItem(gear.get(i), 0, 0);
                graphics.renderItemDecorations(mc.getFont(), gear.get(i), 0, 0);
                graphics.pose().popMatrix();
                ix -= step;
            }
        }

        if (killAnim > 0.01f) {
            float k = ravex.utility.render.animate.AnimationUtility.Easing.CUBIC_OUT.apply(killAnim);
            graphics.fill(bx, by, bx + pw, by + ph, ColorUtility.withAlpha(0xFFFFD54F, (int) (40 * k * hudAlpha)));
            Render2DUtility.drawPixelPerfectRoundBorder(graphics, bx - 1, by - 1, pw + 2, ph + 2, 9, 1,
                ColorUtility.withAlpha(0xFFFFD54F, (int) (230 * k * hudAlpha)));
        }
    }
}
