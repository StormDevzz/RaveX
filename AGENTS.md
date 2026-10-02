# RaveX AI Agent Guidelines

You are an AI coding assistant contributing to **RaveX** - a Fabric Minecraft 1.21.11 utility client mod.

## Critical rules

1. **No direct Minecraft imports when a utility exists.** Do NOT use `Minecraft.getInstance().player` - use `PlayerUtility.getPlayer()`. Do NOT use `Minecraft.getInstance().getConnection().send(...)` - use `NetworkUtility.sendPacket()`. Use `MinecraftWrapper` instead of raw `Minecraft.getInstance()`. Always check `ravex.utility.*` and `ravex.mcwrapper.*` first.
2. **Look at neighboring files before writing new ones.** Match existing patterns for modules, mixins, managers, parameters, events.
3. **No wildcard imports.** Use explicit single-type imports.
4. **Package root:** `ravex.*`. Modules go in `ravex.modules.*`, utilities in `ravex.utility.*`, mixins in `ravex.mixin.*`.
5. **Settings use `@Parameter` annotations on primitive fields.** Do NOT instantiate `BooleanParameter`, `ModeParameter`, `NumberParameter`, `ColorParameter` directly. Use `@Parameter(name = "...", ...)` on `boolean`, `String`, `double`, `int` fields. The annotation supports `min`, `max`, `step`, `modes`, `color`, `options`. Example: `@Parameter(name = "Range", min = 1, max = 10) public double range = 5;`. The `Module` base class automatically wraps these in proper `Parameter<?>` objects at runtime via `scanParameterFields()`.
6. **Keybindings** are set via middle-click in the ClickGUI, not in code.
7. **Mojang mappings** (official names), not Yarn or intermediary.

## Before generating code

- Read the relevant section of `CONTRIBUTING.md` for full context.
- Check `src/main/java/ravex/` for existing utilities before writing raw Minecraft API calls.
- If unsure about a pattern, find a similar existing file and mirror it.

## Verification

- Build: `./gradlew build`
- Ensure no direct `Minecraft.getInstance()` calls slipped in when a wrapper exists.
