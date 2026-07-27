package ravex.modules.hud;
import ravex.utility.misc.ScreenUtility;

import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.Identifier;

import ravex.utility.misc.EntityUtility;
import net.minecraft.world.item.ItemStack;
import ravex.gui.clickgui.ColorUtility;

import ravex.modules.client.Hud;
import ravex.modules.combat.KillAura;
import ravex.modules.combat.Trigger;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.mcwrapper.MinecraftWrapper;

@ModuleInfo(name = "TargetHud", category = "HUD")
public class TargetHud extends ravex.modules.Module {
    public int x;
    public int y;
    public int width;
    public int height;
    @Parameter(name = "MainHand")
    public boolean showMainHand = true;
    @Parameter(name = "Armor")
    public boolean showArmor = true;
    @Parameter(name = "ShowOnHover")
    public boolean showOnHover = true;
    @Parameter(name = "Health", modes = {"HP", "%"})
    public String healthDisplay = "HP";
    @Parameter(name = "HealthColor")
    public boolean healthColor = false;

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

    private TargetHud() {
        super("TargetHud", 350, 50, 180, 50);
        this.x = 10; this.y = 400; this.width = 175; this.height = 46;
    }

    private net.minecraft.world.entity.LivingEntity getTarget(MinecraftWrapper mc) {
        if (ravex.manager.ModuleManager.delegate(KillAura.class).getEnabled()) {
            net.minecraft.world.entity.LivingEntity target = ravex.manager.ModuleManager.delegate(KillAura.class).getCurrentTarget();
            if (target != null && target.isAlive()) return target;
        }
        if (ravex.manager.ModuleManager.delegate(Trigger.class).getEnabled()) {
            net.minecraft.world.entity.LivingEntity target = ravex.manager.ModuleManager.delegate(Trigger.class).getCurrentTarget();
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
        if (!ravex.manager.ModuleManager.delegate(Hud.class).getEnabled()) return;
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

        int bx = x;
        int by = y;
        int w = width;
        int h = height;

        float scale = 0.92f + 0.08f * hudAlpha;
        graphics.pose().pushMatrix();
        float centerX = bx + w / 2f;
        float centerY = by + h / 2f;
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-centerX, -centerY);

        int bgAlpha = (int)(170 * hudAlpha);
        int bgColor = (bgAlpha << 24) | 0x0A0A0E;
        Render2DUtility.drawPixelPerfectRound(graphics, bx, by, w, h, 6, bgColor);
        Render2DUtility.drawRoundBorder(graphics, bx, by, w, h, 6, 1, ColorUtility.withAlpha(ColorUtility.getActiveColor(), (int)(120 * hudAlpha)));

        if (targetEntity != null) {
            if (targetEntity.getId() != lastEntityId) {
                lastEntityId = targetEntity.getId();
                lastEntityHealth = targetEntity.getHealth();
                targetHurtAnim = 0f;
            } else {
                float currentHealth = targetEntity.getHealth();
                if (currentHealth < lastEntityHealth) {
                    targetHurtAnim = 1.0f;
                }
                lastEntityHealth = currentHealth;
            }
        }

        if (targetHurtAnim > 0f) {
            targetHurtAnim = Math.max(0f, targetHurtAnim - delta * 2.2f);
        }

        float hurtProgress = Math.max(targetHurtAnim, targetEntity != null ? targetEntity.hurtTime / 10f : 0f);
        float headScale = 1.0f - 0.15f * hurtProgress;
        int headTint = lerpColor(0xFFFFFFFF, 0xFFFF4444, hurtProgress);

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
        int hpTextColor = healthColor ? lerpColor(0xFFFF3333, 0xFF33FF33, maxHp > 0 ? hp / maxHp : 1f) : 0xFFFFFFFF;
        FontRenderUtility.drawString(graphics, hpText, nx, by + 21, ColorUtility.withAlpha(hpTextColor, (int)(255 * hudAlpha)), true);

        int gridX = bx + w - 55;
        int barX = bx + 44;
        int barW = gridX - 4 - barX;
        int barY = by + 32;

        Render2DUtility.drawRound(graphics, barX, barY, barW, 3, 1, ColorUtility.withAlpha(0xFF1A1A2A, (int)(255 * hudAlpha)));

        int fillHpW = (int) (barW * animatedHpPercent);
        if (fillHpW > 0) {
            int hpColor = lerpColor(0xFFFF3333, 0xFF33FF33, hp / maxHp);
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
                Render2DUtility.drawRoundBorder(graphics, cellX, cellY, cellSize, cellSize, 3, 1, ColorUtility.withAlpha(0x000000, (int)(60 * hudAlpha)));

                boolean shouldShow = (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND && showMainHand)
                        || (slot == net.minecraft.world.entity.EquipmentSlot.OFFHAND && showMainHand)
                        || (slot != net.minecraft.world.entity.EquipmentSlot.MAINHAND && slot != net.minecraft.world.entity.EquipmentSlot.OFFHAND && showArmor);

                if (shouldShow && targetEntity != null) {
                    ItemStack item = targetEntity.getItemBySlot(slot);
                    if (!item.isEmpty()) {
                        graphics.pose().pushMatrix();
                        float itemScale = 0.8f;
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

    private static int lerpColor(int from, int to, float ratio) {
        if (ratio <= 0f) return from;
        if (ratio >= 1f) return to;
        int a1 = (from >> 24) & 0xFF;
        int r1 = (from >> 16) & 0xFF;
        int g1 = (from >> 8) & 0xFF;
        int b1 = from & 0xFF;

        int a2 = (to >> 24) & 0xFF;
        int r2 = (to >> 16) & 0xFF;
        int g2 = (to >> 8) & 0xFF;
        int b2 = to & 0xFF;

        int a = (int)(a1 + (a2 - a1) * ratio);
        int r = (int)(r1 + (r2 - r1) * ratio);
        int g = (int)(g1 + (g2 - g1) * ratio);
        int b = (int)(b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static TargetHud itz() {
        return ravex.manager.ModuleManager.delegate(TargetHud.class);
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("TargetHud").getEnabled();
    }


    

    @Override
    public int getX() { return x; }
    @Override
    public void setX(int x) { this.x = x; }
    @Override
    public int getY() { return y; }
    @Override
    public void setY(int y) { this.y = y; }
    @Override
    public int getWidth() { return width; }
    @Override
    public void setWidth(int w) { this.width = w; }
    @Override
    public int getHeight() { return height; }
    @Override
    public void setHeight(int h) { this.height = h; }

    public boolean isHud() {
        return hud;
    }
}