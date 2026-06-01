## Summary

**Goal**: Make RaveX's ClickGUI visually comparable to ThunderHack — shader-based rounded rects, 4-corner animated gradients, smooth toggle/slider animations, and custom bitmap font (SF Pro / Comfortaa).

**Constraints**: RaveX is on Minecraft 1.21.11 (Mojmap), ThunderHack on 1.21. BufferBuilder/VertexFormat API differs significantly — no `setShaderTexture()`, no `DefaultVertexFormat`, uses `RenderPipeline` + `GpuTextureView` + `TextureSetup`.

### Done
- **Shader resources ported** — `hudshader.fsh/.json`, `rectangle.fsh/.json`, `blur.fsh/.json`, `arc.fsh/.json` at `assets/ravex/shaders/core/`
- **Render2DEngine.java** — `drawRound()`, `drawRoundGradient()`, `skyRainbow()`, `rainbow()`, `fade()`, `twoColorEffect()`, `interpolateColorsBackAndForth()`, `getAnalogousColor()`, `fastAnimation()` lerp
- **ColorUtility.java** — 7 color modes (Static/Sky/LightRainbow/Rainbow/Fade/DoubleColor/Analogous) via `getColor(int index)`, mirrors ThunderHack's pattern
- **ClickGui module** — `colorMode`, `colorSpeed`, `color1`, `color2`, `gradientMode` (LeftToRight/UpsideDown/Both), `blur`, `customFont`
- **CategoryPanel** — gradient header accent using `getColor(270/0/180/90)`, `headerAnim` lerp, Comfortaa font for headers
- **ModuleButton** — `enabledAnim` gradient fill, `hoverProgress` lerp, accent bar, smooth expand, search highlighting, SF Bold font
- **ParameterElement** — rounded toggle switch with animated knob, rounded slider with gradient fill, improved visuals
- **Custom font system** — `RavexFontRenderer` loads TTF via `java.awt.Font`, renders to `BufferedImage`, converts to `NativeImage`, uploads as `DynamicTexture`, draws via `GuiGraphics.fill(RenderPipelines.GUI_TEXTURED, TextureSetup, ...)`
- **FontRenderUtility** — supports `FontType` enum (SF_MEDIUM, SF_BOLD, COMFORTAA, VANILLA) with per-override drawing
- **TTF providers** — `assets/ravex/font/sf_medium.json`, `comfortaa.json`, `sf_bold.json` for resource pack font system
- **Pre-existing fixes** — `ConfigsScreen.java:154` (skinType), `InvPreviewHud.java:61` (getSelectedSlot)
- **Build passes** — zero errors

### Key API Differences (1.21.11 Mojmap vs 1.21)
| Concept | 1.21 (Yarn/Fabric) | 1.21.11 (Mojmap) |
|---|---|---|
| Font | `new Font(FontSet)` | `new Font(Font.Provider)` via `gio$e` |
| BufferBuilder | `begin(VertexFormat.Mode, VertexFormat)` | `BufferBuilder(ByteBufferBuilder, mode, format)` |
| Texture drawing | `RenderSystem.setShaderTexture()` | `RenderPipelines.GUI_TEXTURED` + `TextureSetup.singleTexture(view, sampler)` |
| NativeImage pixel | `setPixelRGBA(x, y, int)` | `setPixelABGR(x, y, int)` |
| DynamicTexture upload | `upload()` | `.upload()` (same name, different bytecode) |
| Identifier | `ResourceLocation` | `Identifier` (`net.minecraft.resources.Identifier`) |

### Next Steps (priority order)
1. Test font rendering ingame — cache may need LRU eviction to avoid memory leaks
2. Optionally port `GlyphMap`-style bitmap atlas for better performance (batch all glyphs into one texture)
3. Resolve `submitBlit`/`innerBlit` access — these methods exist but are private; a mixin could expose them for direct `GpuTextureView`+color rendering
4. Add per-glyph atlas instead of per-string cache for font performance
5. Implement blur shader rendering for the `blur` setting (needs RenderPipeline-based approach)
