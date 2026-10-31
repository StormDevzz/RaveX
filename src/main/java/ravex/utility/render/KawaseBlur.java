package ravex.utility.render;

import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.*;
import ravex.RaveX;
import ravex.mcwrapper.MinecraftWrapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * High-performance Dual-Kawase screen blur utility (ThunderHack Recode Blurry style).
 * Captures the game screen, runs 5-pass Kawase blur once per frame,
 * and renders a rounded anti-aliased frosted glass card behind HUD elements.
 */
public final class KawaseBlur {
    private static final int DOWNSCALE = 2;

    private static boolean initialized = false;
    private static boolean shaderFailed = false;

    // Kawase Blur Shader
    private static int kawaseProgram = 0;
    private static int kawaseTexLoc = 0;
    private static int kawaseOffsetLoc = 0;
    private static int kawaseHalfpixelLoc = 0;

    // Masked Rounded Box Shader
    private static int maskProgram = 0;
    private static int maskBlurTexLoc = 0;
    private static int maskScreenSizeLoc = 0;
    private static int maskGuiScaleLoc = 0;
    private static int maskRectPosLoc = 0;
    private static int maskRectSizeLoc = 0;
    private static int maskRadiusLoc = 0;
    private static int maskTintLoc = 0;
    private static int maskAlphaLoc = 0;

    // Framebuffers and textures
    private static int fbo1 = 0;
    private static int tex1 = 0;
    private static int fbo2 = 0;
    private static int tex2 = 0;
    private static int fboWidth = 0;
    private static int fboHeight = 0;

    // Geometry buffers
    private static int fullVao = 0;
    private static int fullVbo = 0;
    private static int rectVao = 0;
    private static int rectVbo = 0;
    private static FloatBuffer rectBuffer = null;

    // Frame caching
    private static long lastFrameTimestamp = 0L;
    private static boolean blurUpdatedThisFrame = false;

    private KawaseBlur() {}

    public static void onFrameStart() {
        blurUpdatedThisFrame = false;
    }

    private static void init() {
        if (initialized || shaderFailed) return;
        try {
            // Fullscreen quad VAO/VBO
            float[] quad = {
                -1.0f,  1.0f, 0.0f, 1.0f,
                -1.0f, -1.0f, 0.0f, 0.0f,
                 1.0f,  1.0f, 1.0f, 1.0f,
                 1.0f, -1.0f, 1.0f, 0.0f
            };
            ByteBuffer bb = ByteBuffer.allocateDirect(quad.length * 4).order(ByteOrder.nativeOrder());
            bb.asFloatBuffer().put(quad).flip();

            fullVao = GL30.glGenVertexArrays();
            fullVbo = GL15.glGenBuffers();
            GL30.glBindVertexArray(fullVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, fullVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bb, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0L);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 2 * 4L);

            // Dynamic rect VAO/VBO
            ByteBuffer rbb = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder());
            rectBuffer = rbb.asFloatBuffer();

            rectVao = GL30.glGenVertexArrays();
            rectVbo = GL15.glGenBuffers();
            GL30.glBindVertexArray(rectVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, rectVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 4 * 2 * 4L, GL15.GL_DYNAMIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * 4, 0L);

            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            // 1. Compile Kawase Program
            String kawaseVert = """
                #version 150
                in vec2 Position;
                in vec2 UV;
                out vec2 v_uv;
                void main() {
                    v_uv = UV;
                    gl_Position = vec4(Position, 0.0, 1.0);
                }
                """;

            String kawaseFrag = """
                #version 150
                uniform sampler2D u_texture;
                uniform vec2 u_offset;
                uniform vec2 u_halfpixel;
                in vec2 v_uv;
                out vec4 fragColor;
                void main() {
                    vec2 d = u_halfpixel * u_offset;
                    vec4 sum = vec4(0.0);
                    sum += texture(u_texture, v_uv + vec2(-d.x, -d.y));
                    sum += texture(u_texture, v_uv + vec2( d.x, -d.y));
                    sum += texture(u_texture, v_uv + vec2(-d.x,  d.y));
                    sum += texture(u_texture, v_uv + vec2( d.x,  d.y));
                    fragColor = sum * 0.25;
                }
                """;

            kawaseProgram = buildProgram(kawaseVert, kawaseFrag);
            if (kawaseProgram == 0) {
                shaderFailed = true;
                return;
            }
            kawaseTexLoc = GL20.glGetUniformLocation(kawaseProgram, "u_texture");
            kawaseOffsetLoc = GL20.glGetUniformLocation(kawaseProgram, "u_offset");
            kawaseHalfpixelLoc = GL20.glGetUniformLocation(kawaseProgram, "u_halfpixel");

            // 2. Compile Masked Rounded Blur Program
            String maskVert = """
                #version 150
                in vec2 Position;
                void main() {
                    gl_Position = vec4(Position, 0.0, 1.0);
                }
                """;

            String maskFrag = """
                #version 150
                uniform sampler2D u_blurTexture;
                uniform vec2 u_screenSize;
                uniform float u_guiScale;
                uniform vec2 u_rectPos;
                uniform vec2 u_rectSize;
                uniform float u_radius;
                uniform vec4 u_tint;
                uniform float u_alpha;
                out vec4 fragColor;

                float roundedBoxSDF(vec2 p, vec2 b, float r) {
                    vec2 q = abs(p) - b + r;
                    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
                }

                void main() {
                    vec2 pixelCoord = vec2(gl_FragCoord.x / u_guiScale, (u_screenSize.y - gl_FragCoord.y) / u_guiScale);
                    vec2 uv = vec2(gl_FragCoord.x / u_screenSize.x, gl_FragCoord.y / u_screenSize.y);
                    vec2 rectCenter = u_rectPos + u_rectSize * 0.5;
                    vec2 halfSize = u_rectSize * 0.5;
                    float dist = roundedBoxSDF(pixelCoord - rectCenter, halfSize, u_radius);
                    float edgeAlpha = clamp(0.5 - dist, 0.0, 1.0);
                    if (edgeAlpha <= 0.0) {
                        discard;
                    }
                    vec4 blurColor = texture(u_blurTexture, uv);
                    vec3 color = mix(blurColor.rgb, u_tint.rgb, u_tint.a);
                    fragColor = vec4(color, edgeAlpha * u_alpha);
                }
                """;

            maskProgram = buildProgram(maskVert, maskFrag);
            if (maskProgram == 0) {
                shaderFailed = true;
                return;
            }
            maskBlurTexLoc = GL20.glGetUniformLocation(maskProgram, "u_blurTexture");
            maskScreenSizeLoc = GL20.glGetUniformLocation(maskProgram, "u_screenSize");
            maskGuiScaleLoc = GL20.glGetUniformLocation(maskProgram, "u_guiScale");
            maskRectPosLoc = GL20.glGetUniformLocation(maskProgram, "u_rectPos");
            maskRectSizeLoc = GL20.glGetUniformLocation(maskProgram, "u_rectSize");
            maskRadiusLoc = GL20.glGetUniformLocation(maskProgram, "u_radius");
            maskTintLoc = GL20.glGetUniformLocation(maskProgram, "u_tint");
            maskAlphaLoc = GL20.glGetUniformLocation(maskProgram, "u_alpha");

            initialized = true;
        } catch (Throwable t) {
            RaveX.LOGGER.warn("[KawaseBlur] Initialization failed: {}", t.getMessage());
            shaderFailed = true;
        }
    }

    private static int buildProgram(String vert, String frag) {
        int v = compileShader(vert, GL20.GL_VERTEX_SHADER);
        int f = compileShader(frag, GL20.GL_FRAGMENT_SHADER);
        if (v == 0 || f == 0) return 0;

        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, v);
        GL20.glAttachShader(p, f);
        GL20.glBindAttribLocation(p, 0, "Position");
        GL20.glBindAttribLocation(p, 1, "UV");
        GL20.glLinkProgram(p);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == 0) {
            RaveX.LOGGER.warn("[KawaseBlur] Link error: {}", GL20.glGetProgramInfoLog(p));
            GL20.glDeleteProgram(p);
            return 0;
        }
        GL20.glDeleteShader(v);
        GL20.glDeleteShader(f);
        return p;
    }

    private static int compileShader(String source, int type) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, source);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == 0) {
            RaveX.LOGGER.warn("[KawaseBlur] Compile error: {}", GL20.glGetShaderInfoLog(s));
            GL20.glDeleteShader(s);
            return 0;
        }
        return s;
    }

    private static void checkFboSize(int width, int height) {
        int downW = Math.max(1, width / DOWNSCALE);
        int downH = Math.max(1, height / DOWNSCALE);
        if (fboWidth == downW && fboHeight == downH && fbo1 != 0 && fbo2 != 0) return;

        deleteFbos();

        fboWidth = downW;
        fboHeight = downH;

        fbo1 = GL30.glGenFramebuffers();
        tex1 = GL11.glGenTextures();
        initFbo(fbo1, tex1, downW, downH);

        fbo2 = GL30.glGenFramebuffers();
        tex2 = GL11.glGenTextures();
        initFbo(fbo2, tex2, downW, downH);
    }

    private static void initFbo(int fbo, int tex, int w, int h) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, tex, 0);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            RaveX.LOGGER.warn("[KawaseBlur] Framebuffer {} incomplete: {}", fbo, status);
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private static void deleteFbos() {
        if (fbo1 != 0) { GL30.glDeleteFramebuffers(fbo1); fbo1 = 0; }
        if (tex1 != 0) { GL11.glDeleteTextures(tex1); tex1 = 0; }
        if (fbo2 != 0) { GL30.glDeleteFramebuffers(fbo2); fbo2 = 0; }
        if (tex2 != 0) { GL11.glDeleteTextures(tex2); tex2 = 0; }
        fboWidth = 0;
        fboHeight = 0;
    }

    private static void updateBlur(int width, int height, int prevFbo) {
        checkFboSize(width, height);
        if (fbo1 == 0 || fbo2 == 0) return;

        int attachedTex = 0;
        if (prevFbo != 0) {
            try {
                attachedTex = GL30.glGetFramebufferAttachmentParameteri(
                    GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
            } catch (Throwable ignored) {}
        }

        if (attachedTex > 0) {
            // Sample attached color texture with bilinear downsampling
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo1);
            GL11.glViewport(0, 0, fboWidth, fboHeight);
            GL30.glBindVertexArray(fullVao);
            GL20.glUseProgram(kawaseProgram);
            GL20.glUniform1i(kawaseTexLoc, 0);
            GL20.glUniform2f(kawaseHalfpixelLoc, 1.0f / fboWidth, 1.0f / fboHeight);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, attachedTex);
            GL20.glUniform2f(kawaseOffsetLoc, 1.5f, 1.5f);
            GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        } else {
            // Hardware blit from previous framebuffer
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevFbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fbo1);
            if (prevFbo != 0) {
                GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            } else {
                GL11.glReadBuffer(GL11.GL_BACK);
            }
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, fboWidth, fboHeight, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        }

        // Kawase Passes
        GL30.glBindVertexArray(fullVao);
        GL20.glUseProgram(kawaseProgram);
        GL20.glUniform1i(kawaseTexLoc, 0);
        GL20.glUniform2f(kawaseHalfpixelLoc, 1.0f / fboWidth, 1.0f / fboHeight);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        // Pass 1: fbo1 -> fbo2 (offset 2.5)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo2);
        GL11.glViewport(0, 0, fboWidth, fboHeight);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex1);
        GL20.glUniform2f(kawaseOffsetLoc, 2.5f, 2.5f);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);

        // Pass 2: fbo2 -> fbo1 (offset 4.0)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex2);
        GL20.glUniform2f(kawaseOffsetLoc, 4.0f, 4.0f);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);

        // Pass 3: fbo1 -> fbo2 (offset 7.0)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex1);
        GL20.glUniform2f(kawaseOffsetLoc, 7.0f, 7.0f);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);

        // Pass 4: fbo2 -> fbo1 (offset 10.0)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex2);
        GL20.glUniform2f(kawaseOffsetLoc, 10.0f, 10.0f);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);

        // Pass 5: fbo1 -> fbo2 (offset 14.0) - result in tex2
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex1);
        GL20.glUniform2f(kawaseOffsetLoc, 14.0f, 14.0f);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);

        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);
    }

    private static void ensureBlurUpdated(int width, int height, int prevFbo) {
        long now = System.nanoTime();
        if (now - lastFrameTimestamp > 3_000_000L) {
            blurUpdatedThisFrame = false;
            lastFrameTimestamp = now;
        }
        if (!blurUpdatedThisFrame) {
            updateBlur(width, height, prevFbo);
            blurUpdatedThisFrame = true;
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
            GL11.glViewport(0, 0, width, height);
        }
    }

    /**
     * Draws the real blurred scene behind a rounded rectangle (ThunderHack Recode Blurry style).
     * @return true if blur was successfully rendered, false if fallback should be drawn.
     */
    public static boolean draw(GuiGraphics graphics, int x, int y, int w, int h, int radius, float alpha) {
        if (w <= 0 || h <= 0 || alpha <= 0.005f) return false;

        init();
        if (!initialized || shaderFailed) return false;

        var mc = MinecraftWrapper.getWrapper().getRaw();
        if (mc == null || mc.getWindow() == null) return false;
        var window = mc.getWindow();
        int width = window.getWidth();
        int height = window.getHeight();
        double guiScale = window.getGuiScale();
        if (width <= 0 || height <= 0 || guiScale <= 0) return false;

        // Calculate transformed matrix coordinates
        var mat = graphics.pose();
        float kx = mat.m00;
        float ky = mat.m11;
        float sx = mat.m20;
        float sy = mat.m21;

        float screenX = x * kx + sx;
        float screenY = y * ky + sy;
        float screenW = w * kx;
        float screenH = h * ky;
        float screenR = radius * kx;

        if (screenW <= 1 || screenH <= 1) return false;

        // Add 1px padding in GUI space so the SDF edge antialiasing has room to fade out
        float pad = 1.0f;
        float qx = screenX - pad;
        float qy = screenY - pad;
        float qw = screenW + pad * 2.0f;
        float qh = screenH + pad * 2.0f;

        // Normalized Device Coordinates
        float gw = (float) (width / guiScale);
        float gh = (float) (height / guiScale);
        float ndcX1 = (qx / gw) * 2.0f - 1.0f;
        float ndcX2 = ((qx + qw) / gw) * 2.0f - 1.0f;
        float ndcY1 = 1.0f - (qy / gh) * 2.0f;
        float ndcY2 = 1.0f - ((qy + qh) / gh) * 2.0f;

        // Save OpenGL state
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int prevFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);

        try {
            ensureBlurUpdated(width, height, prevFbo);
            if (fbo2 == 0 || tex2 == 0) return false;

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

            GL20.glUseProgram(maskProgram);
            GL20.glUniform1i(maskBlurTexLoc, 0);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex2);

            GL20.glUniform2f(maskScreenSizeLoc, (float) width, (float) height);
            GL20.glUniform1f(maskGuiScaleLoc, (float) guiScale);
            GL20.glUniform2f(maskRectPosLoc, screenX, screenY);
            GL20.glUniform2f(maskRectSizeLoc, screenW, screenH);
            GL20.glUniform1f(maskRadiusLoc, screenR);
            // ThunderHack Recode Blurry theme tint: clean dark slate acrylic glass
            GL20.glUniform4f(maskTintLoc, 0.06f, 0.08f, 0.12f, 0.52f);
            GL20.glUniform1f(maskAlphaLoc, alpha);

            float[] rectVerts = {
                ndcX1, ndcY1,
                ndcX1, ndcY2,
                ndcX2, ndcY1,
                ndcX2, ndcY2
            };
            rectBuffer.clear();
            rectBuffer.put(rectVerts).flip();

            GL30.glBindVertexArray(rectVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, rectVbo);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0L, rectBuffer);

            GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);

            return true;
        } catch (Throwable t) {
            RaveX.LOGGER.warn("[KawaseBlur] Draw error: {}", t.getMessage());
            return false;
        } finally {
            // Restore OpenGL state
            GL20.glUseProgram(prevProgram);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
            GL30.glBindVertexArray(prevVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuffer);

            if (!blendEnabled) GL11.glDisable(GL11.GL_BLEND);
            if (depthEnabled) GL11.glEnable(GL11.GL_DEPTH_TEST);
            if (cullEnabled) GL11.glEnable(GL11.GL_CULL_FACE);
            if (scissorEnabled) GL11.glEnable(GL11.GL_SCISSOR_TEST);
        }
    }
}
