#pragma once

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

// ── Optimizer ────────────────────────────────────────────────────────────
JNIEXPORT jstring JNICALL
Java_ravex_modules_misc_Optimizer_nativeOptimize(JNIEnv* env, jclass clazz, jstring mode);

JNIEXPORT jlong JNICALL
Java_ravex_modules_misc_Optimizer_nativeFreeMemory(JNIEnv* env, jclass clazz);

JNIEXPORT jobjectArray JNICALL
Java_ravex_modules_misc_Optimizer_nativeListTechniques(JNIEnv* env, jclass clazz);

JNIEXPORT void JNICALL
Java_ravex_utility_misc_GuiOptimizer_nativeOptimizeGui(JNIEnv* env, jclass clazz);

JNIEXPORT void JNICALL
Java_ravex_manager_NativeManager_nativeCheckNatives(JNIEnv* env, jclass clazz);

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_NameTags_nativeGetDistance(JNIEnv* env, jclass clazz, jdouble x1, jdouble y1, jdouble z1, jdouble x2, jdouble y2, jdouble z2);

JNIEXPORT jboolean JNICALL
Java_ravex_modules_render_NameTags_nativeIsWithinRange(JNIEnv* env, jclass clazz, jdouble distance, jdouble range);

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_NameTags_nativeCalculateScale(JNIEnv* env, jclass clazz, jdouble distance, jdouble scaleParam, jboolean distanceScaling);

JNIEXPORT jint JNICALL
Java_ravex_utility_misc_GuiOptimizer_nativeOptimizeNameTags(
    JNIEnv* env, jclass clazz,
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
);

JNIEXPORT void JNICALL
Java_ravex_utility_misc_GuiOptimizer_nativeUpdateHudAnimations(
    JNIEnv* env, jclass clazz,
    jfloatArray displayXs,
    jfloatArray displayYs,
    jintArray targetXs,
    jintArray targetYs,
    jbooleanArray animInitializeds,
    jint count,
    jfloat speed
);

JNIEXPORT void JNICALL
Java_ravex_utility_misc_GuiOptimizer_nativeOptimizeTracers(
    JNIEnv* env, jclass clazz,
    jdoubleArray cameraPos,
    jfloatArray modelView,
    jfloatArray projection,
    jdoubleArray positions,
    jint count,
    jint guiWidth,
    jint guiHeight,
    jdoubleArray outPoints
);

// ── AntiAFK ──────────────────────────────────────────────────────────────
JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_AntiAfk_nativeStart(JNIEnv* env, jclass clazz,
    jint intervalMs, jint maxJitterMs,
    jboolean mouseMove, jboolean mouseClick, jboolean keyPress,
    jboolean lookAround, jboolean jumpSim, jint rotationRange);

JNIEXPORT void JNICALL
Java_ravex_modules_misc_AntiAfk_nativeStop(JNIEnv* env, jclass clazz);

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_AntiAfk_nativeIsRunning(JNIEnv* env, jclass clazz);

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_AntiAfk_nativePerformAction(JNIEnv* env, jclass clazz);

#ifdef __cplusplus
}
#endif
