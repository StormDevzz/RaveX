#pragma once

namespace ravex {
namespace plugins {
namespace optimize {

class GuiOptimizer {
public:
    static void optimizeGuiAndGame();
};

class NameTagsOptimizer {
public:
    // optimize NameTags layout calculations
    static int optimizeNameTags(
        const double* cameraPos,
        const float* modelView,
        const float* projection,
        const double* playerViewVec,
        const double* positions,
        const double* textWidths,
        const int* booleans,
        const int* armorCounts,
        int count,
        double scaleParam,
        bool distanceScaling,
        double maxDistance,
        int guiWidth,
        int guiHeight,
        double* outLayouts,
        int* outIndices
    );
};

class HudOptimizer {
public:
    // optimize Hud module lerp animations in batch
    static void updateAnimations(
        float* displayXs,
        float* displayYs,
        const int* targetXs,
        const int* targetYs,
        unsigned char* animInitializeds,
        int count,
        float speed
    );
};

class TracersOptimizer {
public:
    // optimize Tracer projection and offscreen clamping calculations
    static void optimizeTracers(
        const double* cameraPos,
        const float* modelView,
        const float* projection,
        const double* positions,
        int count,
        int guiWidth,
        int guiHeight,
        double* outPoints
    );
};

} // namespace optimize
} // namespace plugins
} // namespace ravex
