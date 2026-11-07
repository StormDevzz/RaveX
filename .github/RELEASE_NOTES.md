# RaveX

## November 7, 2026

### Loader
- Loader window is now draggable (mouse events properly handled on the content pane).
- Added Windows acrylic blur effect to the loader window (DWM SetWindowCompositionAttribute + DwmEnableBlurBehindWindow), with proper HWND retrieval and delayed application after window is shown.
- Replaced the old single-arc loading spinner with a rotating dots spinner (5 dots with staggered delays and non-linear rotation/opacity keyframes).
- Loading dots are now white; animation speed increased.
- Fixed text rendering issues on the acrylic backdrop: switched text antialiasing from LCD/ClearType (colored fringes) to grayscale gasp-based antialiasing; disabled antialiasing for the "X" letter to remove blue outline.
- Text elements (OS version, system info, extra info, status) are now truncated with "..." if they exceed the available width.
- OS version string now shows the localized Windows edition and release (e.g. "Windows 11 Pro 25H2") via WMI `Win32_OperatingSystem.Caption` + registry `DisplayVersion`, supporting all Windows display languages.
- Loader window now appears in the taskbar (changed from `JWindow` to undecorated `JFrame`).
- Fixed window transparency/flickering during drag (override `update()` to skip background erase, `repaint()` after `setLocation`).

### RichPresence (Discord)
- Fixed Discord IPC handshake: expected opcode was incorrectly set to 2 (Close) instead of 1 (Frame), causing all handshakes to fail. Status now updates correctly.
- Fixed window freeze when disabling RichPresence (deadlock between `updateThread` holding `rpcMutex` and `nativeShutdown` acquiring it) — added `join(2000)` timeout.
- Added diagnostic logging (`isConnected`, `sent:` state/details).

### Removed Lua integration
- Removed `LuaManager` (660 lines), Lua resource files, and C++ Lua native code.
- Removed `org.luaj:luaj-jse` dependency from `build.gradle`.
- `Handshake` module now uses only built-in modes (Basic/Forge/Lunar/Custom) without Lua `onHandshake` override.

### Loader (launcher integration)
- Embedded JAR-in-JAR dependencies (including JNA) are now extracted to a temp directory and added to the loader process classpath, so acrylic blur works when running through external launchers (Freesm Launcher, etc.).

### Native layer
- Updated `ravex_dc.dll` manifest hash and rebuilt the native library with the handshake fix.
- Added "loader" to `preloadDependencies` in `NativeLoader`.

## November 1, 2026

## KickX Launcher v1.3 (Windows)

- KickX Launcher for Windows is now version **1.3** (not 1.3.0). Version reflected in `app.rc`, `app.manifest` and `build_info.h.in`.
- Launcher self-update: download hash verification, DPAPI-encrypted tokens, safe uninstall, offline/config/java crash fixes, faster mod search, "what's new" dialog.
- New launcher themes: Hungarian language, Win95 and Vista themes.
- Support for the Linux KickX Launcher ended on **October 11, 2026**. The Linux launcher is no longer developed or supported.
- The Windows KickX Launcher continues to be updated. The Windows launcher version is not mentioned in README.

## Build system

- Native libraries (C++ DLL/so) are now built **only** when explicitly running `./gradlew build`. Regular run/build tasks (e.g. `./gradlew runClient`) no longer trigger the native CMake build. The native build still runs before resource processing within `./gradlew build`.
- Removed the temporary `scripts/` folder (`generate_intro.py`, `sync_native_manifest.py`, pycache).
- Native libraries verified: full CMake/MSVC build succeeded (all `ravex_*.dll`, `kickx_launcher.exe`, `KickXSetup.exe`) and the `ravex_native_smoke` ctest passed.

## Native layer (C++ / JNI) reworked and extended

A large set of native hooks was added or reworked across many modules:

- **AutoClicker** (`autoclicker_jni.cpp`)
- **AutoCrystal** (extensive): `autocrtystal.cpp` + JNI, `armorfucker` (armor durability/evaluation/penetration/prioritizer), `conditions` (grim/player/world), `damage` (armor/collateral/calculator/logger/modifier/prediction/explosion/raycast/shield), `effects` (particle/render/sound/visual), `timings` (delay/latency/predict/scheduler/throttle/ticks/manager), `tracker` (block/crystal/knockback/motion/prediction/target/velocity), `ncp_timings`.
- **AnchorAura**, **AntiBot**, **AntiPearl**, **AntiQuit** (`antiquit`), **AntiRegear**
- **AutoDrop**, **AutoRegear**, **BasePlace**, **BedBomb**, **BowAim**, **Breaker**, **Burrow**
- **Calculator**, **ChunkExploit**, **DC**, **DesktopGUI**
- **EcFarmer**, **Elytra++**, **FakePearl**, **HoleFill**
- **KillAura** (`killaura_jni.cpp`)
- **MediaQuery**, **NameTags**, **NoPacketKick**
- **Nuker** (`nuker.cpp` + JNI)
- **PacketMine**, **PearlTarget**, **Phase**, **Quiver**, **SelfTrap**
- **Shaders**, **ShieldFucker**, **TNTaura**, **Trap**
- Plugins: **FastExp**, **FileProt**, **NativeSc**
- Shared: JNI bridge (`jni_bridge.cpp`/`jni_bridge.hpp`), Lua context, native manager, C addon loader (`c_addon_loader.c`, `common.c`), font SDF (`font_sdf_jni.cpp`), mediaquery JNI.

## Java modules

### Combat
- **AutoCrystal** (`modules/combat/AutoCrystal.java`)

### HUD
- **ArmorHud**, **ArrayListHud**, **WatermarkHud**
- **InvPreviewHud**: now fades out smoothly (animated alpha) and disappears completely when the inventory is empty; shown only when at least one item is present or in the HUD editor. Selection highlight, panel, icon, text and counter all fade with the animation.

### Misc
- **AntiQuit**, **AutoAuth**, **AutoLog**, **BlockMixer**, **FastItem**, **MessageAura**, **PacketHelper**, **PingSpoof**, **StashFinder**

### Movement
- **AntiVoid**, **AutoWalk**, **FastStairs**, **HighJump**, **LongJump**, **NoFall**, **NoWeb**, **ReverseStep**, **RidingHelper**, **SafeWalk**
- **GuiMove**: new option `DisableInClickGui` - when enabled, GuiMove does not move the player while the RaveX ClickGUI is open.

### Player
- **AutoArmor**, **AutoMend**, **AutoRespawn**, **ElytraHelper**, **GhostHand**, **ItemSaver**, **Replenish**, **SourceFiller**, **ViewLock**

### Render
- **ESP**, **Shaders**, **ShiftInterp**, **ViewModel**

### World
- **AutoFish**, **AutoSign**, **AutoTrade**, **AutoWither**, **ECFarmer**, **PVEUtils**, **Scaffold**, **Nuker** (+ `NukerData`)

## HUD / rendering

- **Blur backgrounds**: All HUD module panels now render with a real blurred/frosted-glass background. `Render2DUtility.drawBlurBackground()` uses `drawGradientBlurredShadow` (real Gaussian-blurred textures via `GaussianFilter`) for a soft outer shadow, plus a more opaque frosted panel (`0xD8000000`, ~84% black) and a subtle border (`0x60FFFFFF`) for glass definition. Panel transparency was reduced (panels are more opaque) for better readability.
- `HudRendererUtility.drawBackground()` now supports an optional `alpha` parameter for fading panels; used by `InvPreviewHud` (fade-out when inventory empties) and `WatermarkHud` (intro reveal).
- Modules using `drawBackground`: ArmorHud, ArrayListHud, CooldownsHud, CoordsHud, CurrencyHud, FpsHud, IndicatorsHud, ServerBrandHud, SpeedometerHud, TpsHud, BindListHud, ChatHud, ChestCounterHud, HotbarHud, MediaHud, RamHud, StaffBoardHud, WatermarkHud, InvPreviewHud (all migrated from `Render2DUtility.drawRound()` to `HudRendererUtility.drawBackground()`).
- **HotbarHud**: the custom hotbar panel is positioned exactly at the vanilla health/hunger bars level (top of panel aligned with health bars, bottom-center), with hardcoded offset **OffsetX=10, OffsetY=3**. Only the custom hotbar slots and selection highlight are drawn; health, hunger, armor and experience are rendered by vanilla Minecraft (no custom stat bars anymore, so nothing overlaps or duplicates the vanilla HUD).
- **ArmorHud**: smooth fade-in/fade-out animation (animated alpha) when armor pieces appear or disappear, instead of instant show/hide.
- **CooldownsHud**: fixed dragging in the HUD editor - the element no longer resizes dynamically while being dragged, so it moves smoothly and predictably.
- **Cursor hover**: in both **ClickGUI** and **HudEditorScreen**, the mouse cursor changes to a hand when hovering over HUD modules/elements, and resets to default when not hovering.
- Vanilla hotbar slots are cancelled when HotbarHud + Hud are enabled, so the custom hotbar replaces the vanilla one without duplicating health/hunger/armor/XP.
- Mixins reworked: `MixinInGameHUD` now cancels only `renderItemHotbar` (vanilla slots) so health/hunger/armor/XP keep rendering vanilla; the earlier non-existent `renderHealth`/`renderArmor`/`renderFood` inject targets that crashed the game were removed.

## GUI

- **ClickGUI** reworked: `AutoReGearScreen`, `CategoryPanel`, `ColorPaletteModal`, `ConfigsScreen`, `InvCleanScreen`, `MacroScreen`, `ModuleButton`, `ParameterElement`, `ProfilesScreen`, `SubScreenStyle`.
- **HUD editor**: `HudEditorScreen`, `HudModuleEntry`, `HudPanel` (removed `HudParameterEntry`).
- **Onboarding**: `OnboardingScreen`.
- **Browser**: `BlockBrowserScreen`, `SearchBrowserScreen`.
- **Changelog**: `ChangelogScreen`, `ChangelogManager`.
- **Tooltips**: `ShulkerDataTooltipComponent`, `ShulkerTooltipComponent`.
- **Update**: `UpdateScreen`, `UpdateManager`, `UpdateInstaller`.
- Descriptions: `ClickGuiDescriptions`, `HudDescriptions`.

## Managers / core

- **AddonManager**, **ChangelogManager**, **ConfigManager**, **LayoutManager**, **LuaManager**, **ModuleManager**, **UpdateManager**.
- Removed: `HandShaderManager`, `PlayerShaderManager`.
- Loader: `NativeBridge`, `AddonLoader`, `CAddonManager`.
- Native scripts (`PanicCmd`) and shader managers removed.

## Mixins

Extensive mixin changes across: client (`Minecraft`, `LocalPlayer`, `Mouse`, `MultiPlayerGameMode`, `SoundEngine`, `ClientPacketListener`, `ClientBrandRetriever`, `LocalPlayerDeath`), entity (`Entity`, `NoWeb`), font (`FontManager`), menu (`TitleScreen`), movement (`Flight`, `GuiMove`, `LivingEntityTravel`, `NoRotate`, `PlayerNoSlow`, `Speed`, `Spider`; removed `Timer`), network (`Connection`, `AccessorMultiPlayerGameMode`), player (`GhostHand`, `PlayerInfo`), render (`ChatHud`, `ClientTooltipComponent`, `DeltaTrackerTimer`, `EndCrystalRenderer`, `InGameHUD`, `ItemStack`, `LevelRenderer`, `LivingEntityRenderer`, `MultiBufferSource`, `SubmitNodeStorage`), realms (`RealmsClient`).

## Commands, events, parameters, utilities

- Commands: `BindCmd`, `CalcCmd`, `ConfigCmd`, `FriendCmd`, `HelpCmd`, `SayCmd`, `ToggleCmd`, `WaypointCmd` (removed `PanicCmd`); core `Cmd`, `CmdReg`.
- Events: `DeathEvent`.
- Parameters: `ModeParameter`.
- Utilities: `ContainerUtility`, `RotationUtility`, `BlockRendererUtility`, `FontRenderUtility`, `Render2DUtility`, `Render3DUtility`, `TextureLoaderUtility`, `AnimationUtility` (+ native), `ShaderNative`, `LanguageUtility`.

## Documentation

- README: strictly split - English section on top, Russian section on the bottom, with cross-links at the start of each part. Removed mentions of Windows 10 IoT Enterprise LTSC (only Windows 10 21H2/22H2 or Windows 11 remain). Added note that Linux KickX Launcher support ended on October 11, 2026; the Windows launcher continues to be updated (its version is not listed in README).
- `CONTRIBUTING.md`, `.github/workflows/release_and_notify.yml`, `.gitignore` updated.

## Compatibility

- Minecraft 1.21.11
- Fabric Loader 0.19.3
- Java 21
