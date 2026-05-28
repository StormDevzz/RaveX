#include "jni_bridge.h"
#include "../hooks/optimizer/optimizer.h"
#include "../hooks/antiafk/antiafk.h"
#include "../common/memory.h"
#include "../math/wave_math.h"
#include "../hooks/shaders/color_shader.h"
#include "../hooks/freecam/freecam.h"
#include "../hooks/freecam/camera_correction.h"
#include "../common/asset_protector.h"

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

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_AntiAfk_nativeHasXTest(JNIEnv*, jclass) {
    auto b = ravex::AntiAfk::backend();
    return (b && b->isAvailable()) ? JNI_TRUE : JNI_FALSE;
}

// ═════════════════════════════════════════════════════════════════════════════
//  SHADERS / MATH NATIVE HELPER
// ═════════════════════════════════════════════════════════════════════════════

JNIEXPORT jfloat JNICALL
Java_ravex_modules_render_Shaders_nativeCalculateWave(JNIEnv* env, jclass, jfloat time, jfloat x, jfloat z) {
    return static_cast<jfloat>(ravex::math::calculateWave(time, x, z));
}

JNIEXPORT jint JNICALL
Java_ravex_modules_render_Shaders_nativeBlendColors(JNIEnv* env, jclass, jint color1, jint color2, jfloat ratio) {
    return static_cast<jint>(ravex::shaders::blendColors(color1, color2, ratio));
}

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    // 1. Direct printf to raw OS stdout
    printf("[RaveX-C++] Successful initialization!\n");
    fflush(stdout);

    // 2. Direct JVM PrintStream callback to Java's redirected System.out
    JNIEnv* env = nullptr;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_8) == JNI_OK && env != nullptr) {
        jclass sysClass = env->FindClass("java/lang/System");
        if (sysClass != nullptr) {
            jfieldID outField = env->GetStaticFieldID(sysClass, "out", "Ljava/io/PrintStream;");
            if (outField != nullptr) {
                jobject outObj = env->GetStaticObjectField(sysClass, outField);
                if (outObj != nullptr) {
                    jclass printStreamClass = env->FindClass("java/io/PrintStream");
                    if (printStreamClass != nullptr) {
                        jmethodID printlnMethod = env->GetMethodID(printStreamClass, "println", "(Ljava/lang/String;)V");
                        if (printlnMethod != nullptr) {
                            jstring msg = env->NewStringUTF("[RaveX-C++] Successful initialization!");
                            if (msg != nullptr) {
                                env->CallVoidMethod(outObj, printlnMethod, msg);
                                env->DeleteLocalRef(msg);
                            }
                        }
                    }
                }
            }
        }
    }

    return JNI_VERSION_1_8;
}

// ═════════════════════════════════════════════════════════════════════════════
//  FREECAM
// ═════════════════════════════════════════════════════════════════════════════

JNIEXPORT void JNICALL
Java_ravex_modules_render_FreeCam_nativeReset(JNIEnv* env, jclass, jdouble x, jdouble y, jdouble z, jfloat yaw, jfloat pitch) {
    ravex::hooks::freecam::reset(x, y, z, yaw, pitch);
}

JNIEXPORT void JNICALL
Java_ravex_modules_render_FreeCam_nativeTurn(JNIEnv* env, jclass, jdouble yRot, jdouble xRot) {
    ravex::hooks::freecam::turn(yRot, xRot);
}

JNIEXPORT void JNICALL
Java_ravex_modules_render_FreeCam_nativeUpdatePosition(JNIEnv* env, jclass, jboolean keyUp, jboolean keyDown, jboolean keyLeft, jboolean keyRight, jboolean keyJump, jboolean keyShift, jdouble speed, jdouble smoothness) {
    ravex::hooks::freecam::updatePosition(keyUp, keyDown, keyLeft, keyRight, keyJump, keyShift, speed, smoothness);
}

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_FreeCam_nativeGetX(JNIEnv*, jclass) {
    return ravex::hooks::freecam::g_state.x;
}

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_FreeCam_nativeGetY(JNIEnv*, jclass) {
    return ravex::hooks::freecam::g_state.y;
}

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_FreeCam_nativeGetZ(JNIEnv*, jclass) {
    return ravex::hooks::freecam::g_state.z;
}

JNIEXPORT jfloat JNICALL
Java_ravex_modules_render_FreeCam_nativeGetYaw(JNIEnv*, jclass) {
    return ravex::hooks::freecam::g_state.yaw;
}

JNIEXPORT jfloat JNICALL
Java_ravex_modules_render_FreeCam_nativeGetPitch(JNIEnv*, jclass) {
    return ravex::hooks::freecam::g_state.pitch;
}

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_FreeCam_nativeGetPrevX(JNIEnv*, jclass) {
    return ravex::hooks::freecam::g_state.prevX;
}

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_FreeCam_nativeGetPrevY(JNIEnv*, jclass) {
    return ravex::hooks::freecam::g_state.prevY;
}

JNIEXPORT jdouble JNICALL
Java_ravex_modules_render_FreeCam_nativeGetPrevZ(JNIEnv*, jclass) {
    return ravex::hooks::freecam::g_state.prevZ;
}

JNIEXPORT jfloat JNICALL
Java_ravex_modules_render_FreeCam_nativeGetPrevYaw(JNIEnv*, jclass) {
    return ravex::hooks::freecam::g_state.prevYaw;
}

JNIEXPORT jfloat JNICALL
Java_ravex_modules_render_FreeCam_nativeGetPrevPitch(JNIEnv*, jclass) {
    return ravex::hooks::freecam::g_state.prevPitch;
}

JNIEXPORT void JNICALL
Java_ravex_modules_render_FreeCam_nativeGetCorrected(JNIEnv* env, jclass, jdouble partialTicks, jdoubleArray outArray) {
    auto coords = ravex::hooks::freecam::getCorrectedCoordinates(partialTicks);
    jdouble* elems = env->GetDoubleArrayElements(outArray, nullptr);
    if (elems != nullptr) {
        elems[0] = coords.renderX;
        elems[1] = coords.renderY;
        elems[2] = coords.renderZ;
        elems[3] = coords.renderYaw;
        elems[4] = coords.renderPitch;
        env->ReleaseDoubleArrayElements(outArray, elems, 0);
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  SCRIPT PROTECTOR — Runtime XOR decryption (never touches disk)
// ═════════════════════════════════════════════════════════════════════════════

JNIEXPORT jstring JNICALL
Java_ravex_utility_lua_LuaManager_nativeDecryptScript(JNIEnv* env, jclass,
    jbyteArray encryptedData, jstring keyStr)
{
    // Получаем зашифрованные байты из Java
    jsize len = env->GetArrayLength(encryptedData);
    jbyte* rawBytes = env->GetByteArrayElements(encryptedData, nullptr);
    if (rawBytes == nullptr || len == 0) return env->NewStringUTF("");

    std::vector<uint8_t> buf(reinterpret_cast<uint8_t*>(rawBytes),
                              reinterpret_cast<uint8_t*>(rawBytes) + len);
    env->ReleaseByteArrayElements(encryptedData, rawBytes, JNI_ABORT);

    // Получаем ключ из Java
    const char* keyChars = env->GetStringUTFChars(keyStr, nullptr);
    std::string key(keyChars ? keyChars : "");
    env->ReleaseStringUTFChars(keyStr, keyChars);

    // Расшифровываем XOR прямо в памяти — на диск НИЧЕГО не пишем
    ravex::utility::xorMask(buf, key);

    // Возвращаем чистый Lua-код как UTF-8 строку в Java
    std::string luaSource(reinterpret_cast<char*>(buf.data()), buf.size());
    return env->NewStringUTF(luaSource.c_str());
}
