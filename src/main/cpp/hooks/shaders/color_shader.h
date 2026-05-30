#ifndef RAVEX_COLOR_SHADER_H
#define RAVEX_COLOR_SHADER_H

namespace ravex::shaders {
    int blendColors(int color1, int color2, float ratio);
    float calculateWave(float time, float x, float z);
}

#endif // RAVEX_COLOR_SHADER_H
