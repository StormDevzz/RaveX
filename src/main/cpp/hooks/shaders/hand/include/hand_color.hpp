#ifndef RAVEX_HAND_COLOR_H
#define RAVEX_HAND_COLOR_H

#include "shader_types.hpp"
#include "shader_color.hpp"

namespace ravex::shaders::hand {

void initColorSystem();
void updateColorSystem(float deltaTime);
void setPalette(const ColorPalette& palette);
void triggerReactive(float intensity);

ColorRGBA getPrimaryColor();
ColorRGBA getSecondaryColor();
ColorRGBA getAccentColor();

ColorRGBA getCyclingColor(float speed);
ColorRGBA getBreathingColor(const ColorRGBA& base);
ColorRGBA getReactiveColor(const ColorRGBA& base);
ColorRGBA getGradientAt(float t);

void applyColorToVertex(Vertex& vertex, const ColorRGBA& color, float intensity);
void applyChromaToVertex(Vertex& vertex, float time, float speed, float saturation);
void applyHeatmapToVertex(Vertex& vertex, float value);

} // namespace ravex::shaders::hand

#endif
