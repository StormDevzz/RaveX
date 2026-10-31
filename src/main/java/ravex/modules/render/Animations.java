package ravex.modules.render;

import ravex.modules.Modules;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.event.combat.AttackEvent;
import ravex.event.player.DeathEvent;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.misc.EntityUtility;
import ravex.utility.network.NetworkUtility;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Module(name = "Animations", category = "Render", enabled = true)
public class Animations {
    @Parameter(name = "TabAnimation")
    public boolean tabAnimation = true;

    @Parameter(name = "SmoothCamera")
    public boolean smoothCamera = false;

    @Parameter(name = "Chat", group = true)
    public boolean chatGroup = true;
    @Parameter(name = "ChatMode", modes = {"Vanilla", "Slide", "Drop", "Pop"}, visible = "chatGroup")
    public String chatMode = "Slide";
    @Parameter(name = "ChatSpeed", min = 0.2, max = 2.0, step = 0.1, visible = "chatGroup")
    public double chatSpeed = 1.0;

    // totem pop overlay
    @Parameter(name = "Totem", group = true)
    public boolean totemGroup = true;
    @Parameter(name = "TotemMode", modes = {"Vanilla", "Pop", "Slide", "Zoom"}, visible = "totemGroup")
    public String totemMode = "Pop";
    @Parameter(name = "TotemSpeed", min = 0.2, max = 2.0, step = 0.1, visible = "totemGroup")
    public double totemSpeed = 1.0;

    // clock state for chat and totem overlays
    public static long chatMessageNs = 0L;
    public static volatile boolean chatDirty = false;
    public static long totemPopNs = 0L;

    public static float chatAnimEase() {
        if (!Modules.enabled(Animations.class)) return 1f;
        Animations m = Modules.get(Animations.class);
        // restart the clock on the first rendered frame
        if (chatDirty) {
            chatDirty = false;
            chatMessageNs = System.nanoTime();
        }
        if (m == null || !m.chatGroup || chatMessageNs == 0L) return 1f;
        if ("Vanilla".equals(m.chatMode)) return 1f;
        float dur = (float) (0.4 / Math.max(0.2, m.chatSpeed));
        float t = (System.nanoTime() - chatMessageNs) / 1_000_000_000f / dur;
        if (t >= 1f) return 1f;
        float u = 1f - t;
        return 1f - u * u * u;
    }

    public static float totemAnimEase() {
        if (!Modules.enabled(Animations.class)) return 1f;
        Animations m = Modules.get(Animations.class);
        if (m == null || !m.totemGroup || totemPopNs == 0L) return 1f;
        if ("Vanilla".equals(m.totemMode)) return 1f;
        float dur = (float) (1.1 / Math.max(0.2, m.totemSpeed));
        float t = (System.nanoTime() - totemPopNs) / 1_000_000_000f / dur;
        if (t >= 1f) return 1f;
        return t;
    }

    // tab animation state, driven by tab overlay mixins
    public static float tabFade = 1f;
    public static long tabLastNs = 0L;

    @Parameter(name = "Player", group = true)
    public boolean playerScale = false;

    @Parameter(name = "Scale", min = 0.2, max = 2.5, step = 0.05, visible = "playerScale")
    public double playerScaleValue = 1.0;

    @Parameter(name = "SelfOnly", visible = "playerScale")
    public boolean scaleSelfOnly = false;

    // crystal size and spin tweaks
    @Parameter(name = "Crystal", group = true)
    public boolean crystalGroup = false;

    @Parameter(name = "CrystalSize", min = 0.3, max = 3.0, step = 0.1, visible = "crystalGroup")
    public double crystalSize = 1.0;

    // freeze both vanilla and custom spin
    @Parameter(name = "CrystalStatic", visible = "crystalGroup")
    public boolean crystalStatic = false;

    @Parameter(name = "CrystalSpinH", min = 0.0, max = 10.0, step = 0.5, visible = "crystalGroup")
    public double crystalSpinH = 1.0;

    @Parameter(name = "CrystalSpinV", min = 0.0, max = 10.0, step = 0.5, visible = "crystalGroup")
    public double crystalSpinV = 0.0;

    // gui slide animations follow the TabAnimation toggle.
    public static boolean tabAnimation() {
        if (!Modules.enabled(Animations.class)) return false;
        Animations m = Modules.get(Animations.class);
        return m == null || m.tabAnimation;
    }

    // death physics: client side corpse copy
    @Parameter(name = "DeathPhysics", group = true)
    public boolean deathPhysics = false;
    @Parameter(name = "DeathLifetime", min = 5.0, max = 120.0, step = 5.0, visible = "deathPhysics")
    public double deathLifetime = 30.0;
    @Parameter(name = "DeathPush", visible = "deathPhysics")
    public boolean deathPush = true;

    private static final Map<Integer, Corpse> corpses = new LinkedHashMap<>();
    private static int nextCorpseId = -10000;
    private static final int MAX_CORPSES = 20;

    private static class Corpse {
        final net.minecraft.world.entity.LivingEntity body;
        final int victimId;
        int age = 0;
        int lastDeathTime = 0;
        boolean groundContact = false;
        float limbPos = 0f;
        // small startle so limbs drop naturally
        float limbSpeed = 0.45f;
        Corpse(net.minecraft.world.entity.LivingEntity body, int victimId) {
            this.body = body;
            this.victimId = victimId;
        }
    }

    // {walk animation position, swing speed for the corpse model, null if not one
    public static float[] corpseLimbs(int entityId) {
        Corpse c = corpses.get(entityId);
        if (c == null) return null;
        return new float[]{c.limbPos, Math.min(1.2f, c.limbSpeed)};
    }

    private static final Map<Integer, Long> lastCorpseTick = new java.util.HashMap<>();

    private static boolean hasCorpse(int victimId) {
        for (Corpse c : corpses.values()) {
            if (c.victimId == victimId) return true;
        }
        return false;
    }

    // same tick double calls (event + direct) must not stack corpses
    private static boolean corpseSpawnedThisTick(int victimId) {
        var level = MinecraftWrapper.getWrapper().getLevel();
        long tick = level == null ? -1L : level.getGameTime();
        Long last = lastCorpseTick.get(victimId);
        if (last != null && last == tick) return true;
        lastCorpseTick.put(victimId, tick);
        return false;
    }

    // spawns the ragdoll for a victim; safe to call from packet handling
    public static void onVictimDied(net.minecraft.world.entity.LivingEntity victim) {
        Animations m = Modules.get(Animations.class);
        var mc = MinecraftWrapper.getWrapper();
        var level = mc.getLevel();
        if (victim == null || level == null || victim.level() != level) return;
        if (!Modules.enabled(Animations.class) || m == null || !m.deathPhysics) return;
        if (victim.getId() <= -10000) return; // -10000 and below are our own corpse copies
        // each death gets its own body
        if (corpseSpawnedThisTick(victim.getId())) return;
        captureAndSpawn(victim);
    }

    @Subscribe
    public void onDeath(DeathEvent event) {
        onVictimDied(event.getPlayer());
    }

    private static void captureAndSpawn(net.minecraft.world.entity.LivingEntity victim) {
        var mc = MinecraftWrapper.getWrapper();
        var level = mc.getLevel();
        if (victim == null || level == null) return;
        // snapshot now, adding mid tick is unsafe
        double x = victim.getX(), y = victim.getY(), z = victim.getZ();
        float yaw = victim.getYRot(), pitch = victim.getXRot();
        var motion = victim.getDeltaMovement();
        int victimId = victim.getId();
        boolean isPlayer = EntityUtility.isPlayer(victim);
        com.mojang.authlib.GameProfile copy = null;
        java.util.UUID srcId = null;
        if (isPlayer) {
            var src = ((net.minecraft.world.entity.player.Player) victim).getGameProfile();
            // copy needs a new uuid, EntityLookup drops duplicates
            srcId = src.id();
            copy = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), src.name());
        }
        final net.minecraft.world.entity.EntityType<?> type = victim.getType();
        final com.mojang.authlib.GameProfile finalCopy = copy;
        final java.util.UUID finalSrcId = srcId;
        mc.execute(() -> {
            var lv = mc.getLevel();
            if (lv == null || !Modules.enabled(Animations.class)) return;
            Animations self = Modules.get(Animations.class);
            if (self == null || !self.deathPhysics) return;
            trimCorpses(lv);
            net.minecraft.world.entity.LivingEntity body;
            if (isPlayer) {
                body = new net.minecraft.client.player.RemotePlayer(lv, finalCopy);
                // copy uuid is unknown, cache player info
                try {
                    var info = finalSrcId != null ? NetworkUtility.getPlayerInfo(finalSrcId) : null;
                    if (info != null) {
                        ((ravex.mixin.network.AccessorAbstractClientPlayer) (Object) body).setPlayerInfo(info);
                    }
                } catch (Throwable ignored) {}
            } else {
                var created = type.create(lv, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                if (!(created instanceof net.minecraft.world.entity.LivingEntity living)) return;
                body = living;
            }
            body.setPos(x, y, z);
            body.setYRot(yaw);
            body.setXRot(pitch);
            // avoids a first frame lerp to origin
            body.setOldPosAndRot();
            // noPhysics starts true, corpse needs collisions
            body.noPhysics = false;
            body.setId(nextCorpseId--);
            body.setDeltaMovement(motion);
            body.setHealth(0f);
            lv.addEntity(body);
            corpses.put(body.getId(), new Corpse(body, victimId));
            ravex.RaveX.LOGGER.info("[DeathPhysics] corpse for {} at {}/{}/{}", victimId, (int) x, (int) y, (int) z);
        });
    }

    private static void trimCorpses(net.minecraft.client.multiplayer.ClientLevel lv) {
        while (corpses.size() >= MAX_CORPSES) {
            Iterator<Corpse> it = corpses.values().iterator();
            if (!it.hasNext()) return;
            Corpse oldest = it.next();
            it.remove();
            if (!oldest.body.isRemoved()) {
                lv.removeEntity(oldest.body.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        }
    }

    @Subscribe
    public void onClientTick(TickEvent.Client event) {
        var mc = MinecraftWrapper.getWrapper();
        var level = mc.getLevel();
        Animations m = Modules.get(Animations.class);
        if (level == null || !Modules.enabled(Animations.class) || m == null || !m.deathPhysics) {
            clearCorpses(level);
            return;
        }
        // fallback if the death event never fired
        // iterate a snapshot, spawning registers entities
        var players = new java.util.ArrayList<>(level.players());
        for (var p : players) {
            if (p == null || p.getId() <= -10000) continue;
            if (!EntityUtility.isDead(p) || hasCorpse(p.getId())) continue;
            captureAndSpawn(p);
        }
        var snapshot = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
        for (var e : level.entitiesForRendering()) snapshot.add(e);
        for (var e : snapshot) {
            var living = EntityUtility.asLivingEntity(e);
            if (living == null || EntityUtility.isPlayer(living)) continue;
            if (living.getId() <= -10000) continue;
            if (!EntityUtility.isDead(living) || hasCorpse(living.getId())) continue;
            captureAndSpawn(living);
        }
        if (corpses.isEmpty()) return;
        int lifetime = (int) (m.deathLifetime * 20);
        var self = mc.getPlayer();
        Iterator<Corpse> it = corpses.values().iterator();
        while (it.hasNext()) {
            Corpse c = it.next();
            net.minecraft.world.entity.LivingEntity b = c.body;
            if (b.isRemoved() || b.level() != level) {
                ravex.RaveX.LOGGER.info("[DeathPhysics] corpse {} lost early (removed={}, sameLevel={})",
                    c.victimId, b.isRemoved(), b.level() == level);
                it.remove();
                continue;
            }
            if (++c.age > lifetime) {
                level.removeEntity(b.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                it.remove();
                continue;
            }
            if (c.age == 1 || (c.age % 20 == 0 && c.age <= 100)) {
                ravex.RaveX.LOGGER.info("[DeathPhysics] {} tick {} pos {}/{}/{} ground {} present {}",
                    c.victimId, c.age, (int) b.getX(), (int) b.getY(), (int) b.getZ(),
                    b.onGround(), level.getEntity(b.getId()) != null);
            }
            // only step deathTime when vanilla did not
            if (b.deathTime == c.lastDeathTime) b.deathTime++;
            c.lastDeathTime = b.deathTime;
            var v = b.getDeltaMovement();
            double vyBefore = v.y;
            double vy = v.y - 0.08;
            boolean bounced = false;
            if (b.onGround()) {
                // one small bounce per landing
                if (!c.groundContact) {
                    c.groundContact = true;
                    if (vy < -0.5) {
                        vy = -vy * 0.22;
                        bounced = true;
                    }
                }
            } else {
                c.groundContact = false;
            }
            vy = Math.max(vy, -3.0);
            double fx = v.x * 0.98;
            double fz = v.z * 0.98;
            if (b.onGround()) {
                fx *= 0.6;
                fz *= 0.6;
            }
            double bx = b.getX(), bz = b.getZ();
            b.setDeltaMovement(fx, vy, fz);
            b.move(net.minecraft.world.entity.MoverType.SELF, b.getDeltaMovement());
            b.fallDistance = 0f;
            // limbs swing from sliding and falling, damped
            double slid = Math.hypot(b.getX() - bx, b.getZ() - bz);
            double fall = b.onGround() ? 0.0 : Math.max(0.0, -vyBefore - 0.12);
            c.limbSpeed += slid * 9.0f + (float) fall * 5.0f + (bounced ? 0.6f : 0f);
            c.limbSpeed = Math.min(1.8f, c.limbSpeed * 0.8f);
            if (c.limbSpeed < 0.01f) c.limbSpeed = 0f;
            c.limbPos += c.limbSpeed;
            if (m.deathPush && self != null && self != b
                    && self.getBoundingBox().intersects(b.getBoundingBox().inflate(0.03))) {
                // push only on real overlap, never from standing
                var pv = self.getDeltaMovement();
                double spd = Math.hypot(pv.x, pv.z);
                double dx = b.getX() - self.getX();
                double dz = b.getZ() - self.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                double toward = len > 0.001 ? (pv.x * dx + pv.z * dz) / len : 0;
                if (spd > 0.02 && toward > 0.01) {
                    double push = Math.min(0.32, spd * 0.9);
                    b.push(pv.x / spd * push, 0, pv.z / spd * push);
                    c.limbSpeed = Math.min(1.8f, c.limbSpeed + 0.35f); // limbs react to the shove
                }
            }
        }
    }

    @Subscribe
    public void onAttack(AttackEvent event) {
        if (event.getType() != AttackEvent.AttackType.PRE) return;
        if (corpses.isEmpty()) return;
        Animations mm = Modules.get(Animations.class);
        if (mm == null || !mm.deathPhysics || !mm.deathPush) return;
        for (Corpse c : corpses.values()) {
            if (event.getTarget() != c.body) continue;
            var self = MinecraftWrapper.getWrapper().getPlayer();
            if (self == null) return;
            var d = c.body.position().subtract(self.position());
            double len = Math.sqrt(d.x * d.x + d.z * d.z);
            if (len < 0.001) {
                c.body.push(0, 0.2, 0);
            } else {
                c.body.push(d.x / len * 0.5, 0.15, d.z / len * 0.5);
            }
            event.setCancelled(true);
            return;
        }
    }

    private static void clearCorpses(net.minecraft.client.multiplayer.ClientLevel level) {
        if (level != null) {
            for (Corpse c : corpses.values()) {
                if (!c.body.isRemoved()) {
                    level.removeEntity(c.body.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                }
            }
        }
        corpses.clear();
    }

    // true when the camera should glide over teleports (ender pearl and friends).
    public static boolean smoothCamera() {
        Animations m = Modules.get(Animations.class);
        return m != null && Modules.enabled(Animations.class) && m.smoothCamera;
    }
}
