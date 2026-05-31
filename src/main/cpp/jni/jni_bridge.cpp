#include "jni_bridge.h"
#include "../optimizer/optimizer.h"
#include "../antiafk/antiafk.h"
#include "../common/memory.h"
#include "../hooks/shaders/color_shader.h"

#include <cstring>
#include <vector>

// ═════════════════════════════════════════════════════════════════════════════
//  OPTIMIZER
// ═════════════════════════════════════════════════════════════════════════════

JNIEXPORT jstring JNICALL
Java_ravex_modules_misc_Optimizer_nativeOptimize(JNIEnv* env, jclass, jstring mode) {
    const char* modeStr = env->GetStringUTFChars(mode, nullptr);
    auto result = ravex::Optimizer::run(std::string(modeStr));
    env->ReleaseStringUTFChars(mode, modeStr);

    std::string payload = result.message
        + "|FREE_KB:" + std::to_string(result.freeMemoryKb)
        + "|FREED_KB:" + std::to_string(result.freedKb)
        + "|ACTIONS:"  + std::to_string(result.actionsPerformed);
    return env->NewStringUTF(payload.c_str());
}

JNIEXPORT jlong JNICALL
Java_ravex_modules_misc_Optimizer_nativeFreeMemory(JNIEnv*, jclass) {
    return static_cast<jlong>(ravex::Memory::readMemInfo().free_kb);
}

JNIEXPORT jobjectArray JNICALL
Java_ravex_modules_misc_Optimizer_nativeListTechniques(JNIEnv* env, jclass) {
    auto techniques = ravex::Optimizer::listTechniques();
    jobjectArray arr = env->NewObjectArray(
        static_cast<jsize>(techniques.size()),
        env->FindClass("java/lang/String"),
        env->NewStringUTF("")
    );
    for (size_t i = 0; i < techniques.size(); i++) {
        env->SetObjectArrayElement(arr, static_cast<jsize>(i),
                                   env->NewStringUTF(techniques[i].c_str()));
    }
    return arr;
}

// ═════════════════════════════════════════════════════════════════════════════
//  ANTI-AFK
// ═════════════════════════════════════════════════════════════════════════════

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

// ═════════════════════════════════════════════════════════════════════════════
//  SHADERS JNI BINDINGS
// ═════════════════════════════════════════════════════════════════════════════

extern "C" {

JNIEXPORT jfloat JNICALL
Java_ravex_modules_render_Shaders_nativeCalculateWave(JNIEnv*, jclass, jfloat time, jfloat x, jfloat z) {
    return ravex::shaders::calculateWave(time, x, z);
}

JNIEXPORT jint JNICALL
Java_ravex_modules_render_Shaders_nativeBlendColors(JNIEnv*, jclass, jint color1, jint color2, jfloat ratio) {
    return ravex::shaders::blendColors(color1, color2, ratio);
}

}

