#include <jni.h>
#include "../../../common/include/jni_raii.hpp"
#include "include/nativesc.hpp"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jboolean JNICALL
Java_ravex_nativesc_NativeScBridge_nativeInit(
    JNIEnv* env, jclass)
{
    if (ravex::jni::checkAndClearException(env)) return JNI_FALSE;
    return ravex::nativesc::initialize() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_ravex_nativesc_NativeScBridge_nativeShutdown(
    JNIEnv* env, jclass)
{
    if (ravex::jni::checkAndClearException(env)) return;
    ravex::nativesc::shutdown();
}

JNIEXPORT jstring JNICALL
Java_ravex_nativesc_NativeScBridge_nativeCaptureScreen(
    JNIEnv* env, jclass, jstring path)
{
    JniStringUtfChars pathGuard(env, path);
    if (!pathGuard) return env->NewStringUTF("");
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    const char* pathStr = pathGuard.get();

    auto result = ravex::nativesc::captureScreen(
        ravex::nativesc::CaptureSource::FullScreen,
        ravex::nativesc::ImageFormat::PNG);
    bool saved = false;
    if (result.success && pathStr) {
        saved = ravex::nativesc::saveToFile(result, pathStr);
    }
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    return env->NewStringUTF(saved ? "ok" : result.errorMsg.c_str());
}

JNIEXPORT jobjectArray JNICALL
Java_ravex_nativesc_NativeScBridge_nativeListMonitors(
    JNIEnv* env, jclass)
{
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    auto monitors = ravex::nativesc::listMonitors();
    jclass stringCls = env->FindClass("java/lang/String");
    if (!stringCls) return nullptr;
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    jobjectArray arr = env->NewObjectArray(
        monitors.size(), stringCls, nullptr);
    if (!arr) return nullptr;
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    for (size_t i = 0; i < monitors.size(); i++) {
        env->SetObjectArrayElement(arr, i, env->NewStringUTF(monitors[i].c_str()));
        if (ravex::jni::checkAndClearException(env)) return nullptr;
    }
    return arr;
}

}
