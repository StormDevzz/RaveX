#include "jni_bridge.hpp"
#include "../hooks/norender/include/norender.hpp"
#include "../common/include/memory.hpp"
#include "../common/include/jni_raii.hpp"
#include "../hooks/shaders/hand/include/shader_color.hpp"
#include "../math/include/wave_math.hpp"
#include "../plugins/optimize/include/optimize.hpp"
#include "../plugins/manager/include/manager.hpp"
#include "brand.hpp"

#include <cstring>
#include <vector>
#include <cmath>


extern "C" {

JNIEXPORT jfloat JNICALL
Java_ravex_modules_render_Shaders_nativeCalculateWave(JNIEnv*, jclass, jfloat time, jfloat x, jfloat z) {
    return ravex::math::calculateWave(time, x, z);
}

JNIEXPORT jint JNICALL
Java_ravex_modules_render_Shaders_nativeBlendColors(JNIEnv*, jclass, jint color1, jint color2, jfloat ratio) {
    auto a = ravex::shaders::intToRgba((int)color1);
    auto b = ravex::shaders::intToRgba((int)color2);
    auto r = ravex::shaders::blendColors(a, b, ratio);
    return (jint)ravex::shaders::rgbaToInt(r);
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_render_NoRender_nativeShouldCull(JNIEnv*, jclass, jdouble x, jdouble y, jdouble z, jdouble camX, jdouble camY, jdouble camZ, jdouble maxDist) {
    return ravex::norender::shouldCull(x, y, z, camX, camY, camZ, maxDist) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_ravex_modules_render_NoRender_nativeOptimizeBudget(JNIEnv*, jclass, jint activeCount, jint currentFps, jint minFps) {
    return ravex::norender::optimizeBudget(activeCount, currentFps, minFps);
}

JNIEXPORT jfloatArray JNICALL
Java_ravex_modules_render_NoRender_nativeOptimizeFog(JNIEnv* env, jclass, jfloat envStart, jfloat envEnd, jfloat rdStart, jfloat rdEnd, jfloat skyEnd, jfloat cloudEnd) {
    auto res = ravex::norender::optimizeFog(envStart, envEnd, rdStart, rdEnd, skyEnd, cloudEnd);
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    jfloatArray arr = env->NewFloatArray(static_cast<jsize>(res.size()));
    if (!arr) return nullptr;
    env->SetFloatArrayRegion(arr, 0, static_cast<jsize>(res.size()), res.data());
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    return arr;
}

JNIEXPORT void JNICALL
Java_ravex_utility_misc_GuiOptimizerUtility_nativeOptimizeGui(JNIEnv*, jclass) {
    ravex::plugins::optimize::GuiOptimizer::optimizeGuiAndGame();
}

JNIEXPORT jint JNICALL
Java_ravex_utility_misc_GuiOptimizerUtility_nativeOptimizeNameTags(
    JNIEnv* env, jclass,
    jdoubleArray cameraPos,
    jfloatArray modelView,
    jfloatArray projection,
    jdoubleArray playerViewVec,
    jdoubleArray positions,
    jdoubleArray textWidths,
    jintArray booleans,
    jintArray armorCounts,
    jint count,
    jdouble scaleParam,
    jboolean distanceScaling,
    jdouble maxDistance,
    jint guiWidth,
    jint guiHeight,
    jdoubleArray outLayouts,
    jintArray outIndices
) {
    JniDoubleElements cameraPosGuard(env, cameraPos);
    JniFloatElements modelViewGuard(env, modelView);
    JniFloatElements projectionGuard(env, projection);
    JniDoubleElements playerViewVecGuard(env, playerViewVec);
    JniDoubleElements positionsGuard(env, positions);
    JniDoubleElements textWidthsGuard(env, textWidths);
    JniIntElements booleansGuard(env, booleans);
    JniIntElements armorCountsGuard(env, armorCounts);
    JniDoubleElements outLayoutsGuard(env, outLayouts);
    JniIntElements outIndicesGuard(env, outIndices);

    if (ravex::jni::checkAndClearException(env)) return 0;

    if (!cameraPosGuard || !modelViewGuard || !projectionGuard || !playerViewVecGuard ||
        !positionsGuard || !textWidthsGuard || !booleansGuard || !armorCountsGuard ||
        !outLayoutsGuard || !outIndicesGuard) {
        return 0;
    }

    int result = ravex::plugins::optimize::NameTagsOptimizer::optimizeNameTags(
        cameraPosGuard.get(),
        modelViewGuard.get(),
        projectionGuard.get(),
        playerViewVecGuard.get(),
        positionsGuard.get(),
        textWidthsGuard.get(),
        booleansGuard.get(),
        armorCountsGuard.get(),
        count,
        scaleParam,
        distanceScaling == JNI_TRUE,
        maxDistance,
        guiWidth,
        guiHeight,
        outLayoutsGuard.get(),
        outIndicesGuard.get()
    );

    if (ravex::jni::checkAndClearException(env)) return 0;
    return result;
}

JNIEXPORT void JNICALL
Java_ravex_manager_NativeManager_nativeCheckNatives(JNIEnv*, jclass) {
    ravex::plugins::manager::NativeManager::checkNatives();
}

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_NameTags_nativeGetDistance(JNIEnv* env, jclass, jdouble x1, jdouble y1, jdouble z1, jdouble x2, jdouble y2, jdouble z2) {
    double dx = x1 - x2;
    double dy = y1 - y2;
    double dz = z1 - z2;
    double d = std::sqrt(dx * dx + dy * dy + dz * dz);
    if (ravex::jni::checkAndClearException(env)) return 0.0;
    return d;
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_render_NameTags_nativeIsWithinRange(JNIEnv* env, jclass, jdouble distance, jdouble range) {
    bool res = distance <= range;
    if (ravex::jni::checkAndClearException(env)) return JNI_FALSE;
    return res ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_NameTags_nativeCalculateScale(JNIEnv* env, jclass, jdouble distance, jdouble scaleParam, jboolean distanceScaling) {
    double scale = scaleParam;
    if (distanceScaling == JNI_TRUE) {
        scale = scaleParam * (distance * 0.15);
        if (scale < 0.5) scale = 0.5;
        if (scale > 3.0) scale = 3.0;
    }
    if (ravex::jni::checkAndClearException(env)) return 0.0;
    return scale;
}

JNIEXPORT void JNICALL
Java_ravex_utility_misc_GuiOptimizerUtility_nativeUpdateHudAnimations(
    JNIEnv* env, jclass,
    jfloatArray displayXs,
    jfloatArray displayYs,
    jintArray targetXs,
    jintArray targetYs,
    jbooleanArray animInitializeds,
    jint count,
    jfloat speed
) {
    JniFloatElements displayXsGuard(env, displayXs);
    JniFloatElements displayYsGuard(env, displayYs);
    JniIntElements targetXsGuard(env, targetXs);
    JniIntElements targetYsGuard(env, targetYs);
    JniBooleanElements animInitializedsGuard(env, animInitializeds);

    if (ravex::jni::checkAndClearException(env)) return;

    if (!displayXsGuard || !displayYsGuard || !targetXsGuard || !targetYsGuard ||
        !animInitializedsGuard) {
        return;
    }

    ravex::plugins::optimize::HudOptimizer::updateAnimations(
        displayXsGuard.get(), displayYsGuard.get(), targetXsGuard.get(),
        targetYsGuard.get(), animInitializedsGuard.get(), count, speed
    );

    if (ravex::jni::checkAndClearException(env)) return;
}

JNIEXPORT void JNICALL
Java_ravex_utility_misc_GuiOptimizerUtility_nativeOptimizeTracers(
    JNIEnv* env, jclass,
    jdoubleArray cameraPos,
    jfloatArray modelView,
    jfloatArray projection,
    jdoubleArray positions,
    jint count,
    jint guiWidth,
    jint guiHeight,
    jdoubleArray outPoints
) {
    JniDoubleElements cameraPosGuard(env, cameraPos);
    JniFloatElements modelViewGuard(env, modelView);
    JniFloatElements projectionGuard(env, projection);
    JniDoubleElements positionsGuard(env, positions);
    JniDoubleElements outPointsGuard(env, outPoints);

    if (ravex::jni::checkAndClearException(env)) return;

    if (!cameraPosGuard || !modelViewGuard || !projectionGuard || !positionsGuard ||
        !outPointsGuard) {
        return;
    }

    ravex::plugins::optimize::TracersOptimizer::optimizeTracers(
        cameraPosGuard.get(), modelViewGuard.get(), projectionGuard.get(),
        positionsGuard.get(), count, guiWidth, guiHeight, outPointsGuard.get()
    );

    if (ravex::jni::checkAndClearException(env)) return;
}

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_Surround_nativeGetCenter(JNIEnv* env, jclass, jdouble px, jdouble py, jdouble pz, jboolean autoCenter) {
    jdoubleArray arr = env->NewDoubleArray(3);
    if (!arr) return nullptr;
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    double cx = autoCenter ? std::floor(px) + 0.5 : px;
    double cy = py;
    double cz = autoCenter ? std::floor(pz) + 0.5 : pz;
    jdouble buf[] = {cx, cy, cz};
    env->SetDoubleArrayRegion(arr, 0, 3, buf);
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    return arr;
}

JNIEXPORT jstring JNICALL
Java_ravex_modules_hud_ServerBrandHud_nativeFormatBrand(JNIEnv* env, jclass, jstring rawBrand) {
    if (!rawBrand) return env->NewStringUTF("Unknown");
    JniStringUtfChars brandGuard(env, rawBrand);
    if (!brandGuard) return env->NewStringUTF("Unknown");
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    std::string formatted = ravex::plugins::brand::BrandFormatter::formatBrand(brandGuard.get());
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    jstring result = env->NewStringUTF(formatted.c_str());
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    return result;
}

JNIEXPORT jfloat JNICALL
Java_ravex_utility_shaders_nativec_ShaderNative_nVec3Length(JNIEnv* env, jclass, jfloat x, jfloat y, jfloat z) {
    float res = std::sqrt(x*x + y*y + z*z);
    if (ravex::jni::checkAndClearException(env)) return 0.0f;
    return res;
}

JNIEXPORT jfloat JNICALL
Java_ravex_utility_shaders_nativec_ShaderNative_nVec3Dot(JNIEnv* env, jclass, jfloat x1, jfloat y1, jfloat z1, jfloat x2, jfloat y2, jfloat z2) {
    float res = x1*x2 + y1*y2 + z1*z2;
    if (ravex::jni::checkAndClearException(env)) return 0.0f;
    return res;
}

JNIEXPORT void JNICALL
Java_ravex_utility_shaders_nativec_ShaderNative_nVec3Normalize(JNIEnv* env, jclass, jfloatArray v) {
    JniFloatElements guard(env, v);
    if (ravex::jni::checkAndClearException(env)) return;
    if (!guard) return;
    float len = std::sqrt(guard[0]*guard[0] + guard[1]*guard[1] + guard[2]*guard[2]);
    if (len > 0.0f) {
        guard[0] /= len;
        guard[1] /= len;
        guard[2] /= len;
    }
    if (ravex::jni::checkAndClearException(env)) return;
}

JNIEXPORT void JNICALL
Java_ravex_utility_shaders_nativec_ShaderNative_nMatrixMul(JNIEnv* env, jclass, jfloatArray a, jfloatArray b, jfloatArray out) {
    JniFloatElements guardA(env, a);
    JniFloatElements guardB(env, b);
    JniFloatElements guardOut(env, out);
    if (ravex::jni::checkAndClearException(env)) return;
    if (!guardA || !guardB || !guardOut) return;
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            float sum = 0.0f;
            for (int k = 0; k < 4; k++) {
                sum += guardA[i * 4 + k] * guardB[k * 4 + j];
            }
            guardOut[i * 4 + j] = sum;
        }
    }
    if (ravex::jni::checkAndClearException(env)) return;
}

JNIEXPORT void JNICALL
Java_ravex_utility_shaders_nativec_ShaderNative_nMatrixTransform(JNIEnv* env, jclass, jfloatArray m, jfloat x, jfloat y, jfloat z, jfloat w, jfloatArray out) {
    JniFloatElements guardM(env, m);
    JniFloatElements guardOut(env, out);
    if (ravex::jni::checkAndClearException(env)) return;
    if (!guardM || !guardOut) return;
    guardOut[0] = guardM[0] * x + guardM[4] * y + guardM[8] * z + guardM[12] * w;
    guardOut[1] = guardM[1] * x + guardM[5] * y + guardM[9] * z + guardM[13] * w;
    guardOut[2] = guardM[2] * x + guardM[6] * y + guardM[10] * z + guardM[14] * w;
    guardOut[3] = guardM[3] * x + guardM[7] * y + guardM[11] * z + guardM[15] * w;
    if (ravex::jni::checkAndClearException(env)) return;
}

JNIEXPORT jfloat JNICALL
Java_ravex_utility_shaders_nativec_ShaderNative_nPerlinNoise(JNIEnv* env, jclass, jfloat x, jfloat y, jfloat z) {
    float res = ravex::math::calculateWave(0.0f, x, z);
    if (ravex::jni::checkAndClearException(env)) return 0.0f;
    return res;
}

JNIEXPORT jfloat JNICALL
Java_ravex_utility_shaders_nativec_ShaderNative_nFbmNoise(JNIEnv* env, jclass, jfloat x, jfloat y, jfloat z, jint octaves, jfloat lacunarity, jfloat gain) {
    float res = ravex::math::calculateWave(0.0f, x, z);
    if (ravex::jni::checkAndClearException(env)) return 0.0f;
    return res;
}

JNIEXPORT jfloat JNICALL
Java_ravex_utility_shaders_nativec_ShaderNative_nSimplexNoise(JNIEnv* env, jclass, jfloat x, jfloat y, jfloat z) {
    float res = ravex::math::calculateWave(0.0f, x, z);
    if (ravex::jni::checkAndClearException(env)) return 0.0f;
    return res;
}

JNIEXPORT jfloat JNICALL
Java_ravex_utility_shaders_nativec_ShaderNative_nValueNoise2D(JNIEnv* env, jclass, jfloat x, jfloat y) {
    float res = ravex::math::calculateWave(0.0f, x, y);
    if (ravex::jni::checkAndClearException(env)) return 0.0f;
    return res;
}

JNIEXPORT jfloat JNICALL
Java_ravex_utility_shaders_nativec_ShaderNative_nCellularNoise(JNIEnv* env, jclass, jfloat x, jfloat y, jfloat z) {
    float res = ravex::math::calculateWave(0.0f, x, z);
    if (ravex::jni::checkAndClearException(env)) return 0.0f;
    return res;
}

JNIEXPORT void JNICALL
Java_ravex_utility_shaders_nativec_ShaderNative_nHsbToRgb(JNIEnv* env, jclass, jfloat h, jfloat s, jfloat v, jfloatArray out) {
    JniFloatElements guard(env, out);
    if (ravex::jni::checkAndClearException(env)) return;
    if (!guard) return;
    float r = v, g = v, b = v;
    guard[0] = r;
    guard[1] = g;
    guard[2] = b;
    if (ravex::jni::checkAndClearException(env)) return;
}

JNIEXPORT void JNICALL
Java_ravex_utility_shaders_nativec_ShaderNative_nRgbToHsb(JNIEnv* env, jclass, jfloat r, jfloat g, jfloat b, jfloatArray out) {
    JniFloatElements guard(env, out);
    if (ravex::jni::checkAndClearException(env)) return;
    if (!guard) return;
    guard[0] = r;
    guard[1] = g;
    guard[2] = b;
    if (ravex::jni::checkAndClearException(env)) return;
}

JNIEXPORT jint JNICALL
Java_ravex_utility_shaders_nativec_ShaderNative_nBlendColors(JNIEnv* env, jclass, jint c1, jint c2, jfloat t) {
    auto a = ravex::shaders::intToRgba((int)c1);
    auto b = ravex::shaders::intToRgba((int)c2);
    auto r = ravex::shaders::blendColors(a, b, t);
    jint res = (jint)ravex::shaders::rgbaToInt(r);
    if (ravex::jni::checkAndClearException(env)) return 0;
    return res;
}

} // extern "C"
