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
