# RaveX-Free Audit Report

Generated: 2026-07-29 | 559 Java files | 220 modules | 116 mixins | 78 utilities

---

## 1. CRITICAL ISSUES — Fix Immediately

### 1.1 MixinRealmsClient — Obfuscated Target Will Crash

**File:** `src/main/java/ravex/mixin/realms/MixinRealmsClient.java:8`
```java
@Mixin(targets = "net.minecraft.class_4341")
```

Uses intermediary/obfuscated name `class_4341` instead of Mojang-mapped class. With `"required": true` in mixins.json, this **will crash on launch**.

Same file uses `method_68466` / `method_20998` — also obfuscated names that won't resolve.

**Fix:** Replace with correct Mojang-mapped class and method names, or set `required = false` and add `@Expect(0)`.

---

### 1.2 AccessorLivingEntity — Not Registered in mixins.json

**File:** `src/main/java/ravex/mixin/client/AccessorLivingEntity.java`

This `@Accessor` interface is NOT in `ravex.mixins.json`. But `NoDelay.java:32` casts `p` to `AccessorLivingEntity` and calls `setNoJumpDelay(0)`. Since the mixin was never applied, the interface has no implementation → **`AbstractMethodError` at runtime**.

**Fix:** Register in `ravex.mixins.json`, or remove the cast and use access widener + reflection.

---

### 1.3 MixinNoRotate — NPE When Module Disabled

**File:** `src/main/java/ravex/mixin/movement/MixinNoRotate.java:23`
```java
Modules.get(NoRotate.class).restoreRotation();
```

Called unconditionally at `@At("TAIL")` — if NoRotate is disabled, `saveRotation()` was never called, so `restoreRotation()` restores garbage/default rotation. If the module has never been enabled, `Modules.get()` may return null.

**Fix:** Guard with `Modules.enabled(NoRotate.class)`.

---

## 2. HIGH PRIORITY

### 2.1 Replace @Overwrite with @Inject

**File:** `src/main/java/ravex/mixin/client/MixinClientBrandRetriever.java:14`

Sole `@Overwrite` in the project. Breaks compatibility with any other mod targeting `getClientModName()`.

**Fix:** Replace with `@Inject(method = "getClientModName", at = @At("RETURN"), cancellable = true, remap = false)`.

---

### 2.2 No isCancellable() Guards

**Zero** of 30+ cancellable `@Inject` mixins check `cir.isCancellable()`. If a future MC version changes the injection point to non-cancellable, these will throw.

**Affects:** All cancellable mixins (~30 files).

**Fix:** Add `if (cir.isCancellable()) cir.cancel();` to each.

---

### 2.3 MixinLocalPlayer — Wrong Rotation Source

**File:** `src/main/java/ravex/mixin/client/MixinLocalPlayer.java` (inside Scaffold block)
```java
yaw = KillAura.silentRotation.yaw;  // Should be Scaffold.silentRotation
```

Inside a `Modules.enabled(Scaffold.class)` guard, reads rotation from `KillAura` instead of `Scaffold`. If KillAura is disabled, sends `Rotation(0,0)`.

---

### 2.4 Gradle Properties Typo

**File:** `gradle.properties:11`
```properties
org.gradle.vfs.watching = trues
```
`trues` is not valid → VFS watching silently disabled.

---

### 2.5 Dependency Version Mismatch

`gradle.properties:6` says `loader_version = 0.16.10`, but `libs.versions.toml` pins `loader = 0.19.3` (used at compile time). `fabric.mod.json` reads from properties (0.16.10). These should match.

---

## 3. MEDIUM PRIORITY

### 3.1 Security: RichPresence Leaks Server IP (Default On)

**File:** `src/main/java/ravex/modules/client/RichPresence.java`

Server IP sent to Discord IPC when `showIP = true` (default). Visible in Discord activity feed.

**Fix:** Change default to `showIP = false`.

---

### 3.2 Security: Remote Asset Downloads Without Integrity Check

**File:** `src/main/java/ravex/mixin/client/MixinVanillaPackResources.java`

Downloads assets from GitHub raw at runtime. No SHA-256 verification (unlike native libs). TTF files only pass a magic number check.

---

### 3.3 Security: ConfigManager Path Traversal

**File:** `src/main/java/ravex/manager/ConfigManager.java`

Config `name` used directly in `new File(configDir, name + ".json")` without sanitization. No `../` filter.

---

### 3.4 Remove 4 Empty Mixins

Files registered in mixins.json with zero methods:
- `render.MixinChat.java`
- `render.MixinEndCrystalRenderer.java`
- `render.MixinSubmitNodeStorage.java`
- `render.MixinWorldBorderRenderer.java`

Each is dead code with startup overhead.

---

### 3.5 Remove 2 Orphaned Mixins

Files on disk but NOT in mixins.json:
- `render/MixinParticles.java` (logic duplicated in `client/MixinMultiPlayerGameMode`)
- `world/MixinHoneyBlock.java` (NoSlow honey cancel never applied)

---

## 4. LOW PRIORITY

### 4.1 JSON Comment in mixins.json

**File:** `src/main/resources/ravex.mixins.json:29`
```json
// "render.MixinPlayerRenderer",  // disabled: @Shadow model removed from AvatarRenderer in 1.21.11
```

JSON doesn't support `//` comments. Some parsers/tooling will fail.

---

### 4.2 MixinBoatRenderer — Unbalanced PoseStack

**File:** `src/main/java/ravex/mixin/render/MixinBoatRenderer.java`

Push/pop guarded by condition that could theoretically change mid-frame. Use `try-finally`.

---

### 4.3 MixinModelPart — No-Op @ModifyVariable

**File:** `src/main/java/ravex/mixin/render/MixinModelPart.java`

Both methods just `return consumer;` — dead code.

---

### 4.4 Two MixinClientPacketListener Files

- `client/MixinClientPacketListener.java` — targets `ClientCommonPacketListenerImpl`
- `player/MixinClientPacketListener.java` — targets `ClientPacketListener`

Identical short names cause confusion. Rename one.

---

### 4.5 Hardcoded Discord Client ID & AutoAuth Password

- Discord ID `1517835260799484034` — public by design, but centralizes to one app
- AutoAuth default password `"r1v2x"` — weak default

---

## 5. BUILD & TOOLING

### 5.1 Loom 1.10.5 → 1.14 Available

Current Loom `1.10.5` is outdated. Latest is `1.14.x` for 1.21.11. Upgrade recommended.

---

### 5.2 Sodium Compile-Only Dependency

`libs/sodium-fabric-0.8.13+mc1.21.11-fixed.jar` is `modCompileOnly`. If Sodium APIs are used at runtime without it present, `ClassNotFoundException`.

---

## 6. MODULES-SPECIFIC (Sampled)

From the previous refactoring pass (49 modules converted):

### 6.1 Common Anti-Patterns Found

- `new Thread(...)` in some modules — Minecraft is single-threaded; use `mc.execute()` or `MinecraftWrapper.getWrapper().submit()`
- `Thread.sleep()` in tick handlers — blocks the render thread
- Swallowed exceptions in `catch (Exception ignored){}` — hides bugs
- Hardcoded magic numbers instead of `@Parameter` fields

---

## 7. WHAT'S DONE WELL

### 7.1 Good Patterns

- **`@Parameter` system** — clean annotation-driven config; no manual `BooleanParameter` etc.
- **Module/Event separation** — clean `@Subscribe` event model with `EventBus`
- **Addon security** — RSA-2048 signatures + sandboxed ClassLoader blocking reflection/process/network
- **MixinPlugin** — enforces `ravex.mixin` package restriction
- **Utility wrapper migration** — 0 modules now import `net.minecraft.client.Minecraft` directly
- **Mojang mappings** — future-proof for unobfuscated Minecraft 26.1+

### 7.2 Architecture Strengths

- Service locator pattern with `Injector` DI
- Native library integrity verification (SHA-256 manifest)
- Profile/Config/Macro managers
- Lua scripting with controlled API surface

---

## 8. BOTTOM-LINE RECOMMENDATIONS

Do first (CRITICAL):
1. Fix `MixinRealmsClient` target → Mojang-mapped name
2. Register `AccessorLivingEntity` in mixins.json
3. Fix `MixinNoRotate` unconditional `restoreRotation()`
4. Replace `@Overwrite` with `@Inject` cancellable

Do next (HIGH):
5. Fix `trues` → `true` in gradle.properties
6. Align `loader_version` (0.16.10 vs 0.19.3)
7. Add `isCancellable()` guards to all cancellable mixins
8. Fix `MixinLocalPlayer` Scaffold rotation source
9. Remove 4 empty + 2 orphaned mixin files

Do eventually:
10. Change RichPresence `showIP` default to `false`
11. Add SHA-256 verification to `MixinVanillaPackResources` downloads
12. Sanitize config names in `ConfigManager`
13. Upgrade Loom 1.10.5 → 1.14.x
14. Rename ambiguous `MixinClientPacketListener` duplicates
