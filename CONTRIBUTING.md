# Contributing to RaveX

Thank you for your interest in contributing to RaveX. This document outlines the process and guidelines for contributing.

## Getting Started

### Prerequisites

- **Java 21** (OpenJDK 21+)
- **Gradle** (use the wrapper: `./gradlew`)
- **Git**
- For native code: **CMake**, **Clang/GCC**, **Linux x86_64**

### Building

```bash
git clone https://github.com/StormDevzz/RaveX.git
cd RaveX
./gradlew build
```

The output JAR will be in `build/libs/`.

## Project Structure

```
src/
├── main/
│   ├── java/ravex/       # Core mod (modules, mixins, rendering, utilities)
│   ├── cpp/               # Native C++ code (JNI bridge, performance-critical ops)
│   └── resources/         # Assets, fabric.mod.json, mixin configs
└── ...
```

- **Java** - Core mod logic: ClickGUI, modules, mixins, rendering
- **C++** - Native code: optimizer, anti-AFK, shader hooks, JNI bridge
- **Lua** - Scripting: Discord integration, custom modules, addons

## Code Style

### Java

- Use **Mojang mappings** (official names), not Yarn or intermediary
- Package root: `ravex.*`
- **No comments in code.** Code must be self-documenting. No `//`, `/* */`, `#`, or docstrings. This rule is absolute.
- Follow existing patterns - look at neighboring files before writing new ones
- Keybindings are set only via **middle-click** on the module button in the ClickGUI
- **Prefer utility/wrapper classes over direct Minecraft imports.** The project provides extensive ready-to-use utilities in `ravex.utility.*` and wrappers in `ravex.mcwrapper.*`. These handle null safety, consistency, and reduce boilerplate. Use `PlayerUtility.getPlayer()` instead of `Minecraft.getInstance().player`, `NetworkUtility.sendPacket()` instead of `Minecraft.getInstance().getConnection().send()`, `MinecraftWrapper` instead of `Minecraft.getInstance()`, and so on. Before writing raw Minecraft API calls, check if a utility already exists — common operations (movement, rotation, rendering, inventory, sound, chat, entities) are already wrapped. This keeps the codebase maintainable, avoids repeated null checks, and centralizes version-specific changes.
- **Declare settings with `@Parameter` annotations on primitive fields.** Do NOT create `BooleanParameter`, `ModeParameter`, `NumberParameter`, `ColorParameter` objects directly. Instead, annotate primitive fields with `@Parameter(name = "...", ...)`. Supported field types: `boolean`, `String`, `double`, `int`. The annotation supports `min`, `max`, `step` (for numeric), `modes` (for String → ModeParameter), `color` (for int → ColorParameter), and `options` (for `List<String>` → MultiSelectParameter). The `Module` base class automatically creates the appropriate `Parameter<?>` wrapper objects at runtime. Example:
  ```java
  @Parameter(name = "Speed", min = 0.1, max = 10.0, step = 0.5)
  public double speed = 4.0;

  @Parameter(name = "Targets", modes = {"Players", "Monsters", "Passives"})
  public String targetMode = "Players";

  @Parameter(name = "Color", color = true)
  public int color = 0xFF00FF00;
  ```
  For classes implementing `ModuleAccess` (which do not extend `Module`), the same `@Parameter` annotation system works — just ensure the annotation is present and the field is accessible. The ClickGUI and settings panels discover parameters via the `getParameters()` method inherited from `Module` or through reflection-based scanning.

### C++ (Native Code)

- Located in `src/main/cpp/`
- 61 native `.so` files in `src/main/resources/assets/ravex/natives/`
- JNI bridge follows standard `JNIEnv*` patterns
- Use modern C++20/23 where applicable

### Lua

- Used for scripting and addons
- Located in relevant script directories

## Making Changes

### Branching

1. Fork the repository
2. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Make your changes
4. Test your changes by building: `./gradlew build`
5. Commit with a clear message describing what changed and why

### Commit Messages

- Use the format: `<type>: <description>`
- Types: `feat`, `fix`, `refactor`, `chore`, `docs`
- Examples:
  ```
  feat: add SpeedNCP mode to KillAura
  fix: resolve rotation desync on server hop
  refactor: simplify packet filter logic
  ```

### What to Contribute

- **New modules** - Follow existing module patterns (`src/main/java/ravex/modules/`)
- **Bypass improvements** - Research first, then implement with proper anticheat-specific modes
- **Bug fixes** - Reproduce the issue, fix it, verify the fix
- **Performance optimizations** - Profile first, measure improvement

### What NOT to Contribute

- Server-side exploits or destructive plugins
- Code that violates the GPL license

## Pull Request Process

1. Ensure your code builds without errors: `./gradlew build`
2. Verify your changes don't break existing functionality
3. Open a pull request against `main`
4. Provide a clear description of what your PR does and why
5. Reference any related issues

## Module Development

If you're adding or modifying a module:

1. Check existing modules for patterns (`src/main/java/ravex/modules/combat/`, `render/`, `movement/`, etc.)
2. Register the module in the module manager
3. Add appropriate settings using the existing setting system
4. Test in-game before submitting

## Native Code (C++)

For changes to native code:

1. Ensure compatibility across supported platforms (Linux x86_64 primary)
2. JNI functions must follow the `Java_ravex_*` naming convention
3. Test native builds separately before integrating
4. **Always verify native compilation** — run the CMake build in `src/main/cpp/` before merging. Native compilation errors are not caught by `./gradlew build` alone.

## Reporting Issues

- Use [GitHub Issues](https://github.com/StormDevzz/RaveX/issues)
- Include: steps to reproduce, expected behavior, actual behavior
- Include your OS, Java version, and Minecraft version

## AI-Assisted Development

When using AI coding assistants (such as ChatGPT, Claude, Copilot, etc.) to contribute to RaveX:

- **Feed the rules first.** Provide the AI with the relevant sections of this CONTRIBUTING.md — especially the Java Code Style rules about using utility/wrapper classes over direct Minecraft imports.
- **Verify utility usage.** AI models often default to `Minecraft.getInstance().player` and other raw API calls. Remind the AI to use `PlayerUtility.getPlayer()`, `NetworkUtility.sendPacket()`, `MinecraftWrapper` etc. Check the finished code for unnecessary direct Minecraft imports.
- **Stick to existing patterns.** Tell the AI to look at neighboring files for reference before generating new code — modules, managers, mixins all follow specific templates.
- **No comments rule applies.** AI models love adding `//` comments. Strip them out. Code must be self-documenting.
- **Review everything.** AI-generated code still needs human review. Build the project (`./gradlew build`) before committing.

## Community

- [Telegram](https://t.me/ravex_free)
- [Discord](https://discord.gg/n9HPbgN7S)

## License

By contributing, you agree that your contributions will be licensed under the [GNU General Public License v3.0](LICENSE).
