#include "jni_bridge.hpp"
#include "../hooks/norender/norender.hpp"
#include "../antiafk/antiafk.hpp"
#include "../common/memory.hpp"
#include "../hooks/shaders/hand/include/shader_color.hpp"
#include "../math/wave_math.hpp"
#include "../plugins/optimize/optimize.hpp"
#include "../plugins/manager/manager.hpp"
#include "brand.hpp"

#include <cstring>
#include <vector>
#include <cmath>








JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_AntiAfk_nativeStart(JNIEnv* env, jclass,
    jint intervalMs, jint maxJitterMs,
    jboolean mouseMove, jboolean mouseClick, jboolean keyPress,
    jboolean lookAround, jboolean jumpSim, jint rotationRange) {

    ravex::AfkConfig cfg;
    cfg.intervalMs      = static_cast<int>(intervalMs);
    cfg.maxJitterMs     = static_cast<int>(maxJitterMs);
    cfg.mouseMove       = mouseMove == JNI_TRUE;
    cfg.mouseClick      = mouseClick == JNI_TRUE;
    cfg.keyPress        = keyPress == JNI_TRUE;
    cfg.lookAround      = lookAround == JNI_TRUE;
    cfg.jumpSimulation  = jumpSim == JNI_TRUE;
    cfg.rotationRange   = static_cast<int>(rotationRange);

    return ravex::AntiAfk::start(cfg) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_ravex_modules_misc_AntiAfk_nativeStop(JNIEnv*, jclass) {
    ravex::AntiAfk::stop();
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_AntiAfk_nativeIsRunning(JNIEnv*, jclass) {
    return ravex::AntiAfk::isRunning() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_AntiAfk_nativePerformAction(JNIEnv*, jclass) {
    return ravex::AntiAfk::performRandomAction() ? JNI_TRUE : JNI_FALSE;
}





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
    jfloatArray arr = env->NewFloatArray(static_cast<jsize>(res.size()));
    env->SetFloatArrayRegion(arr, 0, static_cast<jsize>(res.size()), res.data());
    return arr;
}

JNIEXPORT void JNICALL
Java_ravex_utility_misc_GuiOptimizer_nativeOptimizeGui(JNIEnv*, jclass) {
    ravex::plugins::optimize::GuiOptimizer::optimizeGuiAndGame();
}

JNIEXPORT jint JNICALL
Java_ravex_utility_misc_GuiOptimizer_nativeOptimizeNameTags(
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
    
    jdouble* pCameraPos = env->GetDoubleArrayElements(cameraPos, nullptr);
    jfloat* pModelView = env->GetFloatArrayElements(modelView, nullptr);
    jfloat* pProjection = env->GetFloatArrayElements(projection, nullptr);
    jdouble* pPlayerViewVec = env->GetDoubleArrayElements(playerViewVec, nullptr);
    jdouble* pPositions = env->GetDoubleArrayElements(positions, nullptr);
    jdouble* pTextWidths = env->GetDoubleArrayElements(textWidths, nullptr);
    jint* pBooleans = env->GetIntArrayElements(booleans, nullptr);
    jint* pArmorCounts = env->GetIntArrayElements(armorCounts, nullptr);
    jdouble* pOutLayouts = env->GetDoubleArrayElements(outLayouts, nullptr);
    jint* pOutIndices = env->GetIntArrayElements(outIndices, nullptr);

    int result = 0;
    if (pCameraPos && pModelView && pProjection && pPlayerViewVec && pPositions &&
        pTextWidths && pBooleans && pArmorCounts && pOutLayouts && pOutIndices) {
        
        
        result = ravex::plugins::optimize::NameTagsOptimizer::optimizeNameTags(
            pCameraPos,
            pModelView,
            pProjection,
            pPlayerViewVec,
            pPositions,
            pTextWidths,
            pBooleans,
            pArmorCounts,
            count,
            scaleParam,
            distanceScaling == JNI_TRUE,
            maxDistance,
            guiWidth,
            guiHeight,
            pOutLayouts,
            pOutIndices
        );
    }

    
    if (pCameraPos) env->ReleaseDoubleArrayElements(cameraPos, pCameraPos, JNI_ABORT);
    if (pModelView) env->ReleaseFloatArrayElements(modelView, pModelView, JNI_ABORT);
    if (pProjection) env->ReleaseFloatArrayElements(projection, pProjection, JNI_ABORT);
    if (pPlayerViewVec) env->ReleaseDoubleArrayElements(playerViewVec, pPlayerViewVec, JNI_ABORT);
    if (pPositions) env->ReleaseDoubleArrayElements(positions, pPositions, JNI_ABORT);
    if (pTextWidths) env->ReleaseDoubleArrayElements(textWidths, pTextWidths, JNI_ABORT);
    if (pBooleans) env->ReleaseIntArrayElements(booleans, pBooleans, JNI_ABORT);
    if (pArmorCounts) env->ReleaseIntArrayElements(armorCounts, pArmorCounts, JNI_ABORT);
    
    
    if (pOutLayouts) env->ReleaseDoubleArrayElements(outLayouts, pOutLayouts, 0);
    if (pOutIndices) env->ReleaseIntArrayElements(outIndices, pOutIndices, 0);

    return result;
}

JNIEXPORT void JNICALL
Java_ravex_manager_NativeManager_nativeCheckNatives(JNIEnv*, jclass) {
    ravex::plugins::manager::NativeManager::checkNatives();
}

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_NameTags_nativeGetDistance(JNIEnv*, jclass, jdouble x1, jdouble y1, jdouble z1, jdouble x2, jdouble y2, jdouble z2) {
    
    double dx = x1 - x2;
    double dy = y1 - y2;
    double dz = z1 - z2;
    return std::sqrt(dx * dx + dy * dy + dz * dz);
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_render_NameTags_nativeIsWithinRange(JNIEnv*, jclass, jdouble distance, jdouble range) {
    
    return distance <= range ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_NameTags_nativeCalculateScale(JNIEnv*, jclass, jdouble distance, jdouble scaleParam, jboolean distanceScaling) {
    
    double scale = scaleParam;
    if (distanceScaling == JNI_TRUE) {
        scale = scaleParam * (distance * 0.15);
        if (scale < 0.5) scale = 0.5;
        if (scale > 3.0) scale = 3.0;
    }
    return scale;
}

JNIEXPORT void JNICALL
Java_ravex_utility_misc_GuiOptimizer_nativeUpdateHudAnimations(
    JNIEnv* env, jclass,
    jfloatArray displayXs,
    jfloatArray displayYs,
    jintArray targetXs,
    jintArray targetYs,
    jbooleanArray animInitializeds,
    jint count,
    jfloat speed
) {
    jfloat* pDisplayXs = env->GetFloatArrayElements(displayXs, nullptr);
    jfloat* pDisplayYs = env->GetFloatArrayElements(displayYs, nullptr);
    jint* pTargetXs = env->GetIntArrayElements(targetXs, nullptr);
    jint* pTargetYs = env->GetIntArrayElements(targetYs, nullptr);
    jboolean* pAnimInitializeds = env->GetBooleanArrayElements(animInitializeds, nullptr);

    if (pDisplayXs && pDisplayYs && pTargetXs && pTargetYs && pAnimInitializeds) {
        ravex::plugins::optimize::HudOptimizer::updateAnimations(
            pDisplayXs, pDisplayYs, pTargetXs, pTargetYs, pAnimInitializeds, count, speed
        );
    }

    if (pDisplayXs) env->ReleaseFloatArrayElements(displayXs, pDisplayXs, 0);
    if (pDisplayYs) env->ReleaseFloatArrayElements(displayYs, pDisplayYs, 0);
    if (pTargetXs) env->ReleaseIntArrayElements(targetXs, pTargetXs, JNI_ABORT);
    if (pTargetYs) env->ReleaseIntArrayElements(targetYs, pTargetYs, JNI_ABORT);
    if (pAnimInitializeds) env->ReleaseBooleanArrayElements(animInitializeds, pAnimInitializeds, 0);
}

JNIEXPORT void JNICALL
Java_ravex_utility_misc_GuiOptimizer_nativeOptimizeTracers(
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
    jdouble* pCameraPos = env->GetDoubleArrayElements(cameraPos, nullptr);
    jfloat* pModelView = env->GetFloatArrayElements(modelView, nullptr);
    jfloat* pProjection = env->GetFloatArrayElements(projection, nullptr);
    jdouble* pPositions = env->GetDoubleArrayElements(positions, nullptr);
    jdouble* pOutPoints = env->GetDoubleArrayElements(outPoints, nullptr);

    if (pCameraPos && pModelView && pProjection && pPositions && pOutPoints) {
        ravex::plugins::optimize::TracersOptimizer::optimizeTracers(
            pCameraPos, pModelView, pProjection, pPositions, count, guiWidth, guiHeight, pOutPoints
        );
    }

    if (pCameraPos) env->ReleaseDoubleArrayElements(cameraPos, pCameraPos, JNI_ABORT);
    if (pModelView) env->ReleaseFloatArrayElements(modelView, pModelView, JNI_ABORT);
    if (pProjection) env->ReleaseFloatArrayElements(projection, pProjection, JNI_ABORT);
    if (pPositions) env->ReleaseDoubleArrayElements(positions, pPositions, JNI_ABORT);
    if (pOutPoints) env->ReleaseDoubleArrayElements(outPoints, pOutPoints, 0);
}

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_Surround_nativeGetCenter(JNIEnv* env, jclass, jdouble px, jdouble py, jdouble pz, jboolean autoCenter) {
    jdoubleArray arr = env->NewDoubleArray(3);
    if (!arr) return nullptr;
    double cx = autoCenter ? std::floor(px) + 0.5 : px;
    double cy = py;
    double cz = autoCenter ? std::floor(pz) + 0.5 : pz;
    jdouble buf[] = {cx, cy, cz};
    env->SetDoubleArrayRegion(arr, 0, 3, buf);
    return arr;
}

JNIEXPORT jstring JNICALL
Java_ravex_modules_hud_ServerBrandHud_nativeFormatBrand(JNIEnv* env, jclass, jstring rawBrand) {
    if (!rawBrand) return env->NewStringUTF("Unknown");
    const char* cBrand = env->GetStringUTFChars(rawBrand, nullptr);
    std::string formatted = ravex::plugins::brand::BrandFormatter::formatBrand(cBrand);
    env->ReleaseStringUTFChars(rawBrand, cBrand);
    return env->NewStringUTF(formatted.c_str());
}

}

