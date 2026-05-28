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

// ── Shaders ──────────────────────────────────────────────────────────────
JNIEXPORT jfloat JNICALL
Java_ravex_modules_render_Shaders_nativeCalculateWave(JNIEnv* env, jclass clazz, jfloat time, jfloat x, jfloat z);

JNIEXPORT jint JNICALL
Java_ravex_modules_render_Shaders_nativeBlendColors(JNIEnv* env, jclass clazz, jint color1, jint color2, jfloat ratio);

// ── FreeCam ──────────────────────────────────────────────────────────────
JNIEXPORT void JNICALL
Java_ravex_modules_render_FreeCam_nativeReset(JNIEnv* env, jclass clazz, jdouble x, jdouble y, jdouble z, jfloat yaw, jfloat pitch);

JNIEXPORT void JNICALL
Java_ravex_modules_render_FreeCam_nativeTurn(JNIEnv* env, jclass clazz, jdouble yRot, jdouble xRot);

JNIEXPORT void JNICALL
Java_ravex_modules_render_FreeCam_nativeUpdatePosition(JNIEnv* env, jclass clazz, jboolean keyUp, jboolean keyDown, jboolean keyLeft, jboolean keyRight, jboolean keyJump, jboolean keyShift, jdouble speed, jdouble smoothness);

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_FreeCam_nativeGetX(JNIEnv* env, jclass clazz);

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_FreeCam_nativeGetY(JNIEnv* env, jclass clazz);

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_FreeCam_nativeGetZ(JNIEnv* env, jclass clazz);

JNIEXPORT jfloat JNICALL
Java_ravex_modules_render_FreeCam_nativeGetYaw(JNIEnv* env, jclass clazz);

JNIEXPORT jfloat JNICALL
Java_ravex_modules_render_FreeCam_nativeGetPitch(JNIEnv* env, jclass clazz);

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_FreeCam_nativeGetPrevX(JNIEnv* env, jclass clazz);

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_FreeCam_nativeGetPrevY(JNIEnv* env, jclass clazz);

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_FreeCam_nativeGetPrevZ(JNIEnv* env, jclass clazz);

JNIEXPORT jfloat JNICALL
Java_ravex_modules_render_FreeCam_nativeGetPrevYaw(JNIEnv* env, jclass clazz);

JNIEXPORT jfloat JNICALL
Java_ravex_modules_render_FreeCam_nativeGetPrevPitch(JNIEnv* env, jclass clazz);

JNIEXPORT void JNICALL
Java_ravex_modules_render_FreeCam_nativeGetCorrected(JNIEnv* env, jclass clazz, jdouble partialTicks, jdoubleArray outArray);

// ── ScriptProtector ───────────────────────────────────────────────────────────
// Принимает зашифрованный .dat буфер (jbyteArray), расшифровывает XOR в памяти
// и возвращает чистый Lua-код как jstring. Расшифрованный текст НИКОГДА не
// записывается на диск — он живёт только в оперативной памяти во время сессии.
JNIEXPORT jstring JNICALL
Java_ravex_utility_lua_LuaManager_nativeDecryptScript(JNIEnv* env, jclass clazz,
    jbyteArray encryptedData, jstring key);

#ifdef __cplusplus
}
#endif
