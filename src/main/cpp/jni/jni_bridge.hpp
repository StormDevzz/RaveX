#pragma once

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif


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


JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_Surround_nativeGetCenter(JNIEnv* env, jclass clazz, jdouble px, jdouble py, jdouble pz, jboolean autoCenter);

// ===== Shader Native Math =====
JNIEXPORT jfloat JNICALL
Java_ravex_shaders_nativec_ShaderNative_nVec3Length(JNIEnv* env, jclass clazz, jfloat x, jfloat y, jfloat z);

JNIEXPORT jfloat JNICALL
Java_ravex_shaders_nativec_ShaderNative_nVec3Dot(JNIEnv* env, jclass clazz, jfloat x1, jfloat y1, jfloat z1, jfloat x2, jfloat y2, jfloat z2);

JNIEXPORT void JNICALL
Java_ravex_shaders_nativec_ShaderNative_nVec3Normalize(JNIEnv* env, jclass clazz, jfloatArray v);

JNIEXPORT void JNICALL
Java_ravex_shaders_nativec_ShaderNative_nMatrixMul(JNIEnv* env, jclass clazz, jfloatArray a, jfloatArray b, jfloatArray out);

JNIEXPORT void JNICALL
Java_ravex_shaders_nativec_ShaderNative_nMatrixTransform(JNIEnv* env, jclass clazz, jfloatArray m, jfloat x, jfloat y, jfloat z, jfloat w, jfloatArray out);

JNIEXPORT jfloat JNICALL
Java_ravex_shaders_nativec_ShaderNative_nPerlinNoise(JNIEnv* env, jclass clazz, jfloat x, jfloat y, jfloat z);

JNIEXPORT jfloat JNICALL
Java_ravex_shaders_nativec_ShaderNative_nFbmNoise(JNIEnv* env, jclass clazz, jfloat x, jfloat y, jfloat z, jint octaves, jfloat lacunarity, jfloat gain);

JNIEXPORT jfloat JNICALL
Java_ravex_shaders_nativec_ShaderNative_nSimplexNoise(JNIEnv* env, jclass clazz, jfloat x, jfloat y, jfloat z);

JNIEXPORT jfloat JNICALL
Java_ravex_shaders_nativec_ShaderNative_nValueNoise2D(JNIEnv* env, jclass clazz, jfloat x, jfloat y);

JNIEXPORT jfloat JNICALL
Java_ravex_shaders_nativec_ShaderNative_nCellularNoise(JNIEnv* env, jclass clazz, jfloat x, jfloat y, jfloat z);

JNIEXPORT void JNICALL
Java_ravex_shaders_nativec_ShaderNative_nHsbToRgb(JNIEnv* env, jclass clazz, jfloat h, jfloat s, jfloat v, jfloatArray out);

JNIEXPORT void JNICALL
Java_ravex_shaders_nativec_ShaderNative_nRgbToHsb(JNIEnv* env, jclass clazz, jfloat r, jfloat g, jfloat b, jfloatArray out);

JNIEXPORT jint JNICALL
Java_ravex_shaders_nativec_ShaderNative_nBlendColors(JNIEnv* env, jclass clazz, jint c1, jint c2, jfloat t);

#ifdef __cplusplus
}
#endif
