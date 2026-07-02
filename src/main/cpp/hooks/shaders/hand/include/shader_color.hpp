#ifndef RAVEX_SHADER_COLOR_H
#define RAVEX_SHADER_COLOR_H

#include "shader_types.hpp"
#include "shader_math.hpp"
#include <cmath>
#include <cstdint>

namespace ravex::shaders {

inline ColorRGB intToRgb(int argb) {
    return {
        ((argb >> 16) & 0xff) / 255.0f,
        ((argb >> 8) & 0xff) / 255.0f,
        ((argb) & 0xff) / 255.0f
    };
}

inline ColorRGBA intToRgba(int argb) {
    return {
        ((argb >> 16) & 0xff) / 255.0f,
        ((argb >> 8) & 0xff) / 255.0f,
        ((argb) & 0xff) / 255.0f,
        ((argb >> 24) & 0xff) / 255.0f
    };
}

inline int rgbaToInt(const ColorRGBA& c) {
    return ((int)(c.a * 255) << 24) | ((int)(c.r * 255) << 16)
         | ((int)(c.g * 255) << 8) | (int)(c.b * 255);
}

inline int rgbToInt(const ColorRGB& c) {
    return 0xff000000 | ((int)(c.r * 255) << 16)
         | ((int)(c.g * 255) << 8) | (int)(c.b * 255);
}

inline ColorRGB hsbToRgb(const ColorRGB& hsb) {
    float h = hsb.r, s = hsb.g, v = hsb.b;
    float r = v, g = v, b = v;
    if (s > 0) {
        h = h - std::floor(h);
        int i = (int)(h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - s * f);
        float t = v * (1 - s * (1 - f));
        switch (i % 6) {
            case 0: r=v; g=t; b=p; break;
            case 1: r=q; g=v; b=p; break;
            case 2: r=p; g=v; b=t; break;
            case 3: r=p; g=q; b=v; break;
            case 4: r=t; g=p; b=v; break;
            case 5: r=v; g=p; b=q; break;
        }
    }
    return {r, g, b};
}

inline ColorRGB rgbToHsb(const ColorRGB& rgb) {
    float r = rgb.r, g = rgb.g, b = rgb.b;
    float mx = std::max({r, g, b}), mn = std::min({r, g, b});
    float d = mx - mn;
    float h = 0, s = mx > 0 ? d / mx : 0;
    if (d > 0) {
        if (mx == r) h = 60 * std::fmod((g - b) / d, 6.0f);
        else if (mx == g) h = 60 * ((b - r) / d + 2);
        else h = 60 * ((r - g) / d + 4);
        if (h < 0) h += 360;
        h /= 360.0f;
    }
    return {h, s, mx};
}

inline ColorRGBA blendColors(const ColorRGBA& a, const ColorRGBA& b, float t) {
    return {
        lerp(a.r, b.r, t),
        lerp(a.g, b.g, t),
        lerp(a.b, b.b, t),
        lerp(a.a, b.a, t)
    };
}

inline ColorRGBA gradientColor(const ColorRGBA* stops, int count, float t) {
    if (count <= 0) return {1,1,1,1};
    if (t <= 0) return stops[0];
    if (t >= 1) return stops[count-1];
    float seg = t * (count - 1);
    int i = (int)seg;
    float f = seg - i;
    if (i >= count - 1) return stops[count-1];
    return blendColors(stops[i], stops[i+1], f);
}

inline ColorRGBA rainbowColor(float time, float speed, float saturation, float brightness) {
    float h = frac(time * speed);
    ColorRGB c = hsbToRgb({h, saturation, brightness});
    return {c.r, c.g, c.b, 1.0f};
}

inline ColorRGBA chromaColor(float time, float speed, float saturation) {
    return rainbowColor(time, speed, saturation, 1.0f);
}

inline ColorRGBA heatmapColor(float t) {
    t = saturate(t);
    ColorRGBA stops[] = {
        {0.0f, 0.0f, 0.5f, 1.0f},
        {0.0f, 0.5f, 1.0f, 1.0f},
        {0.0f, 1.0f, 0.5f, 1.0f},
        {1.0f, 1.0f, 0.0f, 1.0f},
        {1.0f, 0.0f, 0.0f, 1.0f}
    };
    return gradientColor(stops, 5, t);
}

inline ColorRGBA pastelColor(const ColorRGBA& base, float amount) {
    return {
        base.r + (1.0f - base.r) * amount * 0.3f,
        base.g + (1.0f - base.g) * amount * 0.3f,
        base.b + (1.0f - base.b) * amount * 0.3f,
        base.a
    };
}

struct ColorPalette {
    ColorRGBA primary;
    ColorRGBA secondary;
    ColorRGBA accent;
    ColorRGBA background;
    float animationTime;
    float speed;

    ColorRGBA sample(float t) const {
        ColorRGBA stops[] = {primary, secondary, accent, primary};
        return gradientColor(stops, 4, frac(t));
    }

    void animate(float dt) {
        animationTime += dt * speed;
    }
};

} // namespace ravex::shaders

#endif
